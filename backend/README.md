# Play2PDF Backend

FastAPI app that turns YouTube playlists into PDF study guides.

**Production URL:** https://adnanfoisal-play2pdf.hf.space

This is the maintenance-fork of the original Play2Pdf backend, kept
backwards-compatible with the v1 endpoints while adding the v3
improvements called for in `docs/PREMIUM_DESIGN_PLAN.md` Phase H.

---

## Endpoints

| Method | Path                       | Purpose                                            |
|--------|----------------------------|----------------------------------------------------|
| GET    | `/ping`                    | Lightweight liveness check (`{status: "awake"}`)   |
| GET    | `/health`                  | Richer health check (`{status, version, themes}`)  |
| GET    | `/themes`                  | Server-authoritative theme palettes (cover + page) |
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
  "topics": "Big-O\nRecursion\nSorting",
  "theme": "tufte_scholar",
  "layout": "portrait"
}
```

Notes:
- `topics` is newline-delimited (a topic may contain a comma); a single
  comma-separated line is accepted as a legacy fallback.
- `layout` is optional: `"portrait"` (default — v3.1 redesigned output
  with cover, table of contents, bookmarks, and per-topic sections) or
  `"grid_landscape"` (the pre-3.1 checklist grid).
- PDFs embed Noto fonts (Bengali/Cyrillic/Greek/accents render as real
  text, not `?`), remain searchable/copyable, and every theme passes
  WCAG AA contrast (see `test_themes.py`).

Themes (21 total): `nordic_frost`, `velvet_dawn`, `mint_blueprint`,
`golden_era`, `midnight_purple`, `cyberpunk_2077`, `obsidian_crimson`,
`oceanic_abyss`, `tufte_scholar`, `princeton_math`, `midnight_terminal`,
`cambridge_emerald`, `bauhaus_geometric`, `swiss_stark`,
`oxford_burgundy`, `deep_space`, `mit_tech`, `wharton_ledger`,
`sumi_ink`, `renaissance_gold`, `warm_sunset_dark`.
Full palettes: `GET /themes`.

---

## Local dev

```bash
cd backend
pip install -r requirements.txt   # pinned to exact versions
export YOUTUBE_API_KEY=...   # only needed if you want to test full flows
export GEMINI_API_KEY=...
uvicorn server:app --reload --port 7860
```

The app will start without the API keys — they're passed in the request
body per call, so they're not environment variables on the server.

Offline test suite (no keys, no network):

```bash
python test_themes.py   # 21 themes x 2 layouts: render, blank pages,
                        # extractability, WCAG AA contrast, Unicode
python test_e2e.py      # full HTTP surface with monkeypatched YT/Gemini
```

---

## Deployment

Deployed to HuggingFace Spaces via the included `Dockerfile`. The space
is at https://adnanfoisal-play2pdf.hf.space.

Pushes to `main` that touch `backend/` trigger the
`.github/workflows/deploy-backend.yml` GitHub Action (requires the
`HF_TOKEN` repo secret — a write-access HuggingFace token). The action
fails loudly if the secret is missing.

### Cold-start behavior

HF Spaces free tier sleeps after 48h of inactivity and takes 30-60s to
wake up on the first request. The Android app's "Test Connection" button
has a 30s timeout to absorb this — if it reports "Offline", the user
should retry in a minute.

---

## Models

- **Topic extraction:** `gemini-3.5-flash-lite` (cheap, fast)
- **Topic-to-video matching:** `gemini-3.8-flash` (newest flash, verified live 2026-09-03)

Both are defined once at the top of `server.py` (`MODEL_EXTRACT` /
`MODEL_MATCH`) and reported by `GET /health` under `models`, so what's
running is never a guess.

Gemini returns transient `503 overloaded` errors under load — measured
3 calls each on 2026-09-03: `gemini-3.8-flash` 3/3, `gemini-3.7-flash`
2/3, `gemini-3.6-flash` 1/3. `_gemini_generate()` therefore retries
transient failures (429/5xx) with 1s / 3s / 7s backoff; before that a
single 503 surfaced to the user as "Compilation failed".

Both models are configured per-request via the `gemini_key` field — the
server itself has no API keys baked in.

---

## Rate limiting

`slowapi` enforces 60 requests/minute per IP on the root app. The
Android app should never hit this in normal use (a single compile flow
is ~3 requests), but it protects the Space from abuse.

Note: the `/api/v1/` mounted sub-app does not inherit the root app's
middleware (FastAPI `mount` limitation) — rate limiting and request
logging apply to the root paths the app actually uses.

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
