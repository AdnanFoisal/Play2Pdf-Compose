# Play2PDF Backend

FastAPI app that turns YouTube playlists into PDF study guides.

**Production URL:** https://adnanfoisal-play2pdf.hf.space

This is the maintenance-fork of the original Play2Pdf backend, kept
backwards-compatible with the v1 endpoints while adding the v3
improvements called for in `docs/PREMIUM_DESIGN_PLAN.md` §Phase H.

---

## Endpoints

| Method | Path                       | Purpose                                            |
|--------|----------------------------|----------------------------------------------------|
| GET    | `/ping`                    | Lightweight liveness check (`{status: "awake"}`)   |
| GET    | `/health`                  | Richer health check (`{status, version, themes}`)  |
| POST   | `/extract_topics`          | AI-extract syllabus topics from a playlist         |
| POST   | `/playlist_meta`           | Fetch title/channel/video-count for a playlist     |
| POST   | `/generate_guide`          | Compile the actual PDF (returns `application/pdf`) |

All endpoints are also available under `/api/v1/` for forward-compat.
The Android Compose app currently uses the root paths.

### Request bodies

**`POST /extract_topics`**
```json
{ "youtube_key": "...", "gemini_key": "...", "playlist_urls": ["..."] }
```

**`POST /playlist_meta`**
```json
{ "youtube_key": "...", "playlist_url": "..." }
```

**`POST /generate_guide`**
```json
{
  "youtube_key": "...", "gemini_key": "...",
  "subject": "Data Structures", "author": "Student",
  "playlist_urls": ["..."],
  "topics": "Big-O,Recursion,Sorting",
  "theme": "tufte_scholar"
}
```

Themes (13 total): `tufte_scholar`, `princeton_math`, `midnight_terminal`,
`cambridge_emerald`, `bauhaus_geometric`, `swiss_stark`, `oxford_burgundy`,
`deep_space`, `mit_tech`, `wharton_ledger`, `sumi_ink`, `renaissance_gold`,
`warm_sunset_dark`.

---

## Local dev

```bash
cd backend
pip install -r requirements.txt
export YOUTUBE_API_KEY=...   # only needed if you want to test full flows
export GEMINI_API_KEY=...
uvicorn server:app --reload --port 7860
```

The app will start without the API keys — they're passed in the request
body per call, so they're not environment variables on the server.

---

## Deployment

Deployed to HuggingFace Spaces via the included `Dockerfile`. The space
is at https://adnanfoisal-play2pdf.hf.space.

Pushes to `main` trigger an automatic redeploy (configurable via the
`.github/workflows/deploy-backend.yml` GitHub Action — disabled by
default, enable by setting the `HF_TOKEN` secret).

### Cold-start behavior

HF Spaces free tier sleeps after 48h of inactivity and takes 30-60s to
wake up on the first request. The Android app's "Test Connection" button
has a 30s timeout to absorb this — if it reports "Offline", the user
should retry in a minute.

---

## Models

- **Topic extraction:** `gemini-3.5-flash-lite` (cheap, fast)
- **Topic-to-video matching:** `gemini-3.6-flash` (latest GA, confirmed July 2026)

Both models are configured per-request via the `gemini_key` field — the
server itself has no API keys baked in.

---

## Rate limiting

`slowapi` enforces 60 requests/minute per IP. The Android app should
never hit this in normal use (a single compile flow is ~3 requests),
but it protects the Space from abuse.

---

## Logging

JSON-formatted lines to stdout, picked up by HF Spaces log aggregator.
Fields:
- `ts` — unix timestamp
- `level` — INFO / WARNING / ERROR
- `logger` — `play2pdf`
- `msg` — human-readable message
- `method`, `path`, `status`, `ms` — per-request metrics (on the `request` message)

---

## License

© 2026 Adnan Foisal. All Rights Reserved.
