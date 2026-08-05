#!/usr/bin/env python3
"""Send job alerts (Telegram + email) for listings newly discovered this run.

Run immediately after fetch_jobs.py in the same workflow job — reads the
ephemeral data/.new_this_run.json it writes. Both channels are optional and
degrade to a no-op with a log line if their secrets/config aren't set.
"""
from __future__ import annotations

import json
import os
import smtplib
import sys
from email.mime.text import MIMEText
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT / "data"
NEW_THIS_RUN_FILE = DATA_DIR / ".new_this_run.json"
CONFIG_FILE = ROOT / "config" / "alerts.json"

REQUEST_TIMEOUT = 15
DIGEST_JOB_LIMIT = 20


def load_json(path: Path, default):
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return default


def matches_keywords(job: dict, keywords: list[str]) -> bool:
    if not keywords:
        return True
    haystack = " ".join(
        [job.get("title", ""), job.get("company", ""), " ".join(job.get("tags", []))]
    ).lower()
    return any(kw.lower() in haystack for kw in keywords)


def format_digest(jobs: list[dict]) -> str:
    lines = [f"{len(jobs)} new job listing(s):", ""]
    for job in jobs[:DIGEST_JOB_LIMIT]:
        lines.append(f"- {job['title']} @ {job['company']} ({job.get('source', '')})\n  {job['url']}")
    if len(jobs) > DIGEST_JOB_LIMIT:
        lines.append(f"...and {len(jobs) - DIGEST_JOB_LIMIT} more")
    return "\n".join(lines)


def send_telegram(text: str) -> None:
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID")
    if not token or not chat_id:
        print("[info] telegram: TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID not set; skipping")
        return
    resp = requests.post(
        f"https://api.telegram.org/bot{token}/sendMessage",
        json={"chat_id": chat_id, "text": text[:4096], "disable_web_page_preview": True},
        timeout=REQUEST_TIMEOUT,
    )
    if resp.ok:
        print("[info] telegram: alert sent")
    else:
        print(f"[warn] telegram: send failed ({resp.status_code}): {resp.text}", file=sys.stderr)


def send_email(subject: str, body: str, to_addr: str) -> None:
    smtp_user = os.environ.get("SMTP_USERNAME")
    smtp_pass = os.environ.get("SMTP_PASSWORD")
    smtp_host = os.environ.get("SMTP_HOST", "smtp.gmail.com")
    smtp_port = int(os.environ.get("SMTP_PORT", "587"))

    if not smtp_user or not smtp_pass:
        print("[info] email: SMTP_USERNAME/SMTP_PASSWORD not set; skipping")
        return
    if not to_addr:
        print("[info] email: no recipient set (config/alerts.json 'email.to' or ALERT_EMAIL_TO secret); skipping")
        return

    msg = MIMEText(body)
    msg["Subject"] = subject
    msg["From"] = smtp_user
    msg["To"] = to_addr

    try:
        with smtplib.SMTP(smtp_host, smtp_port, timeout=REQUEST_TIMEOUT) as server:
            server.starttls()
            server.login(smtp_user, smtp_pass)
            server.sendmail(smtp_user, [to_addr], msg.as_string())
        print(f"[info] email: alert sent to {to_addr}")
    except smtplib.SMTPException as exc:
        print(f"[warn] email: send failed: {exc}", file=sys.stderr)


def main() -> int:
    config = load_json(CONFIG_FILE, {})
    new_jobs = load_json(NEW_THIS_RUN_FILE, {}).get("jobs", [])

    keywords = config.get("keywords", [])
    matched = [j for j in new_jobs if matches_keywords(j, keywords)]

    if not matched:
        print("[info] no new jobs match alert criteria; nothing to send")
        return 0

    digest = format_digest(matched)

    if config.get("telegram", {}).get("enabled", True):
        send_telegram(digest)

    email_cfg = config.get("email", {})
    if email_cfg.get("enabled", True):
        to_addr = os.environ.get("ALERT_EMAIL_TO") or email_cfg.get("to") or ""
        send_email(f"Job Tracker: {len(matched)} new listing(s)", digest, to_addr)

    print(f"[info] alert dispatch complete for {len(matched)} matching job(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
