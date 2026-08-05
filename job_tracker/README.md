# Job Tracker (GitHub Actions Edition)

A serverless job board: GitHub Actions fetches and aggregates remote job
listings on a schedule, commits them as JSON, and GitHub Pages serves a
static frontend that reads that JSON. No backend, no hosting cost.

```
GitHub Actions (scheduled, hourly)
  -> job_tracker/scripts/fetch_jobs.py
  -> fetches RemoteOK, We Work Remotely, Dev.to jobs feeds
  -> dedupes against job_tracker/data/jobs.json
  -> aggregates per-company counts into job_tracker/data/companies.json
  -> commits the updated JSON
       |
       v
GitHub Pages (static)
  -> job_tracker/frontend/index.html
  -> fetches ./data/*.json client-side
  -> search, filter by company, save favorites (localStorage)
```

## Layout

```
job_tracker/
├── scripts/
│   ├── fetch_jobs.py       # fetch + dedupe + aggregate
│   └── requirements.txt
├── data/
│   ├── jobs.json           # active listings (last 30 days)
│   ├── companies.json      # per-company listing counts
│   └── job-history.json    # archive of listings older than 30 days
└── frontend/
    └── index.html          # vanilla JS + Tailwind (CDN), no build step
```

Workflows live at the repo root (GitHub Actions requires `.github/workflows/`):

- `.github/workflows/job-tracker-fetch.yml` — runs hourly, refreshes the data files.
- `.github/workflows/job-tracker-pages.yml` — deploys `frontend/` + `data/` to GitHub Pages whenever either changes.

## Data sources

Free, no API key required:

- [RemoteOK](https://remoteok.com/api) — JSON API
- [We Work Remotely](https://weworkremotely.com/categories/remote-programming-jobs.rss) — RSS
- [Dev.to jobs tag](https://dev.to/feed/tag/jobs) — RSS

Add more by appending to the `SOURCES` list in `scripts/fetch_jobs.py`. Each
source is wrapped in its own try/except so one feed going down doesn't break
the run.

## One-time setup

1. Merge this branch to `main` (scheduled workflows and the Pages deploy
   trigger only run on the default branch).
2. In repo **Settings → Pages**, set the source to **GitHub Actions**.
3. Optionally trigger both workflows manually once via **Actions → Run
   workflow** to populate data immediately instead of waiting for the next
   hourly run.

## Local development

```bash
pip install -r job_tracker/scripts/requirements.txt
python job_tracker/scripts/fetch_jobs.py   # refreshes job_tracker/data/*.json
python -m http.server 8000 --directory job_tracker/frontend
# then open http://localhost:8000 (data/ won't resolve locally unless
# you also serve job_tracker/data at ../data, or copy it alongside index.html)
```

## Notes / limitations

- Indeed retired its public RSS feeds, so it isn't included as a source.
- Job "id" is a hash of the listing URL, used for dedup and favoriting.
- Listings older than 30 days roll off `jobs.json` into `job-history.json`
  (capped at the most recent 5,000 archived listings).
