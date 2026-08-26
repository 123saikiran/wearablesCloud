#!/usr/bin/env python3
"""Fetch remote-job RSS/JSON feeds and aggregate them into data/jobs.json + data/companies.json.

Run by the "Job Tracker - Fetch Jobs" GitHub Actions workflow on a schedule.
Safe to run locally too: `python job_tracker/scripts/fetch_jobs.py`
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

import feedparser
import requests

ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT / "data"
JOBS_FILE = DATA_DIR / "jobs.json"
COMPANIES_FILE = DATA_DIR / "companies.json"
HISTORY_FILE = DATA_DIR / "job-history.json"
NEW_THIS_RUN_FILE = DATA_DIR / ".new_this_run.json"

RETENTION_DAYS = 3
HISTORY_RETENTION_DAYS = 3
HISTORY_CAP = 5000
REQUEST_TIMEOUT = 15
USER_AGENT = "job-tracker-bot/1.0 (+github actions; educational project)"


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def job_id(url: str) -> str:
    return hashlib.sha1(url.encode("utf-8")).hexdigest()[:12]


def clean_text(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", value).strip()


def parse_date(value: str | None) -> str | None:
    """Best-effort parse of a feed date string into an ISO 8601 string (UTC)."""
    if not value:
        return None
    try:
        import email.utils as eut

        parsed = eut.parsedate_to_datetime(value)
        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=timezone.utc)
        return parsed.astimezone(timezone.utc).isoformat()
    except Exception:
        return None


def fetch_remoteok() -> list[dict]:
    """RemoteOK public JSON API: https://remoteok.com/api"""
    jobs = []
    resp = requests.get(
        "https://remoteok.com/api",
        headers={"User-Agent": USER_AGENT},
        timeout=REQUEST_TIMEOUT,
    )
    resp.raise_for_status()
    for item in resp.json():
        url = item.get("url")
        title = clean_text(item.get("position"))
        if not url or not title:
            continue
        jobs.append(
            {
                "title": title,
                "company": clean_text(item.get("company")) or "Unknown",
                "location": clean_text(item.get("location")) or "Remote",
                "url": url,
                "source": "remoteok",
                "tags": [clean_text(t) for t in item.get("tags", []) if t],
                "posted_at": parse_date(item.get("date")),
            }
        )
    return jobs


def fetch_rss(feed_url: str, source: str, company_from_title: bool = False) -> list[dict]:
    """Generic RSS feed handler (used for We Work Remotely, Dev.to, etc.)."""
    jobs = []
    parsed = feedparser.parse(
        feed_url,
        agent=USER_AGENT,
        request_headers={"User-Agent": USER_AGENT},
    )
    for entry in parsed.entries:
        url = entry.get("link")
        raw_title = clean_text(entry.get("title"))
        if not url or not raw_title:
            continue

        title = raw_title
        company = clean_text(entry.get("author")) or "Unknown"
        if company_from_title and ":" in raw_title:
            maybe_company, _, rest = raw_title.partition(":")
            if 0 < len(maybe_company) <= 60 and rest.strip():
                company = clean_text(maybe_company)
                title = clean_text(rest)

        tags = [clean_text(t.get("term")) for t in entry.get("tags", []) if t.get("term")]

        jobs.append(
            {
                "title": title,
                "company": company,
                "location": "Remote",
                "url": url,
                "source": source,
                "tags": tags,
                "posted_at": parse_date(entry.get("published")),
            }
        )
    return jobs


SOURCES = [
    ("remoteok", fetch_remoteok),
    (
        "weworkremotely",
        lambda: fetch_rss(
            "https://weworkremotely.com/categories/remote-programming-jobs.rss",
            "weworkremotely",
            company_from_title=True,
        ),
    ),
    (
        "devto",
        lambda: fetch_rss("https://dev.to/feed/tag/jobs", "devto"),
    ),
]


def load_json(path: Path, default: dict) -> dict:
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return default


def save_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> int:
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    existing = load_json(JOBS_FILE, {"updated_at": None, "jobs": []})
    existing_by_id = {j["id"]: j for j in existing.get("jobs", [])}

    history = load_json(HISTORY_FILE, {"archived_before": None, "jobs": []})
    history_by_id = {j["id"]: j for j in history.get("jobs", [])}

    fetched_at = now_iso()
    newly_added: list[dict] = []

    for name, fetcher in SOURCES:
        try:
            raw_jobs = fetcher()
        except Exception as exc:  # noqa: BLE001 - one bad feed shouldn't kill the run
            print(f"[warn] source '{name}' failed: {exc}", file=sys.stderr)
            continue

        for job in raw_jobs:
            jid = job_id(job["url"])
            # Skip anything already known, whether still active or already
            # archived — otherwise a source that keeps re-serving an old
            # listing (e.g. RemoteOK) makes it look "new" again forever,
            # re-triggering alerts and re-archiving it every run.
            if jid in existing_by_id or jid in history_by_id:
                continue
            job["id"] = jid
            job["fetched_at"] = fetched_at
            existing_by_id[jid] = job
            newly_added.append(job)

        print(f"[info] {name}: {len(raw_jobs)} listing(s) parsed")

    save_json(NEW_THIS_RUN_FILE, {"jobs": newly_added})

    def older_than(job: dict, cutoff: datetime) -> bool:
        ts = job.get("posted_at") or job.get("fetched_at")
        if not ts:
            return False
        try:
            return datetime.fromisoformat(ts) < cutoff
        except ValueError:
            return False

    active_cutoff = datetime.now(timezone.utc) - timedelta(days=RETENTION_DAYS)
    active_jobs = [j for j in existing_by_id.values() if not older_than(j, active_cutoff)]
    archived_jobs = [j for j in existing_by_id.values() if older_than(j, active_cutoff)]

    active_jobs.sort(key=lambda j: j.get("posted_at") or j.get("fetched_at") or "", reverse=True)

    save_json(JOBS_FILE, {"updated_at": fetched_at, "jobs": active_jobs})

    for j in archived_jobs:
        history_by_id[j["id"]] = j

    history_cutoff = datetime.now(timezone.utc) - timedelta(days=HISTORY_RETENTION_DAYS)
    history_jobs = sorted(
        (j for j in history_by_id.values() if not older_than(j, history_cutoff)),
        key=lambda j: j.get("posted_at") or j.get("fetched_at") or "",
        reverse=True,
    )[:HISTORY_CAP]
    save_json(HISTORY_FILE, {"archived_before": fetched_at, "jobs": history_jobs})

    companies: dict[str, dict] = {}
    for job in active_jobs:
        company = job["company"]
        entry = companies.setdefault(company, {"count": 0, "job_ids": [], "last_seen": job.get("posted_at") or job.get("fetched_at")})
        entry["count"] += 1
        entry["job_ids"].append(job["id"])
        seen = job.get("posted_at") or job.get("fetched_at")
        if seen and (not entry["last_seen"] or seen > entry["last_seen"]):
            entry["last_seen"] = seen

    save_json(COMPANIES_FILE, {"updated_at": fetched_at, "companies": companies})

    print(f"[info] {len(newly_added)} new listing(s); {len(active_jobs)} active; {len(archived_jobs)} archived this run")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
