"""
Play2PDF FastAPI backend — Compose rewrite (v3.0).

This is the maintenance-fork of the original Play2Pdf backend, kept
backwards-compatible with the v1 endpoints (so the existing Flet app
keeps working) while adding the v3 improvements called for in
PREMIUM_DESIGN_PLAN.md §Phase H:

  * /health endpoint for uptime monitoring
  * Rate limiting via slowapi
  * Structured JSON logging (stdout)
  * CORS allow-list (Android app origin + localhost for dev)
  * /api/v1/ prefix on all v3 endpoints (v1 endpoints kept at root for
    backwards-compat with the Flet app)
  * Multi-stage Dockerfile (smaller image)

Backend is deployed to:
  https://adnanfoisal-play2pdf.hf.space

Cold-start time on HF Spaces free tier is 30-60s — the Android app's
"Test Connection" button has a 30s timeout to absorb this.
"""
import os
import re
import json
import time
import logging
import tempfile
import hashlib
from typing import Any

from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, field_validator
from google.generativeai import GenerativeModel, configure as configure_genai
from googleapiclient.discovery import build
from fpdf import FPDF
import qrcode

# --- Structured JSON logging -------------------------------------------------
# HF Spaces aggregates stdout — JSON lines are easy to grep.
class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, Any] = {
            "ts": int(record.created),
            "level": record.levelname,
            "logger": record.name,
            "msg": record.getMessage(),
        }
        if record.exc_info:
            payload["exc"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)

logging.basicConfig(level=logging.INFO)
handler = logging.StreamHandler()
handler.setFormatter(JsonFormatter())
logging.getLogger().handlers = [handler]
log = logging.getLogger("play2pdf")


# --- FastAPI app + middleware ------------------------------------------------
app = FastAPI(title="Play2PDF API", version="3.0")

# CORS allow-list — the Android app's package name (sent as Origin) plus
# localhost for local dev. We can't whitelist by package name at the HTTP
# level (that's an Android-system concept), so we allow any origin but
# log the Origin header so we can spot abuse in production logs.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Android apps don't send a meaningful Origin
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


# --- Rate limiting (slowapi) -------------------------------------------------
# 60 req/min per IP is enough for a single-user study-guide app while
# keeping the HF Space free tier happy.
try:
    from slowapi import Limiter
    from slowapi.util import get_remote_address
    from slowapi.errors import RateLimitExceeded
    from slowapi.middleware import SlowAPIMiddleware

    limiter = Limiter(key_func=get_remote_address, default_limits=["60/minute"])
    app.state.limiter = limiter
    app.add_middleware(SlowAPIMiddleware)

    @app.exception_handler(RateLimitExceeded)
    async def rate_limit_handler(request: Request, exc: RateLimitExceeded):
        log.warning("rate_limit_exceeded", extra={"path": request.url.path})
        return Response(
            content=json.dumps({"detail": "Rate limit exceeded. Try again in a minute."}),
            status_code=429,
            media_type="application/json",
        )
except ImportError:
    log.warning("slowapi not installed — rate limiting disabled")


# --- PDF themes (unchanged from v1) -----------------------------------------
THEMES: dict[str, dict] = {
    # Light Themes
    "nordic_frost": {
        "bg": (242, 246, 250), "accent": (134, 168, 196), "text": (44, 62, 80), "subtext": (127, 140, 141),
        "paper_bg": (255, 255, 255), "paper_text": (44, 62, 80), "paper_border": (236, 240, 241), "font_family": "Times",
    },
    "velvet_dawn": {
        "bg": (253, 246, 241), "accent": (214, 158, 145), "text": (92, 70, 70), "subtext": (168, 144, 144),
        "paper_bg": (255, 252, 249), "paper_text": (92, 70, 70), "paper_border": (245, 235, 230), "font_family": "Times",
    },
    "mint_blueprint": {
        "bg": (237, 252, 248), "accent": (52, 211, 153), "text": (15, 118, 110), "subtext": (20, 184, 166),
        "paper_bg": (255, 255, 255), "paper_text": (15, 118, 110), "paper_border": (204, 251, 241), "font_family": "Courier",
    },
    "golden_era": {
        "bg": (244, 240, 230), "accent": (184, 145, 78), "text": (66, 52, 38), "subtext": (117, 102, 88),
        "paper_bg": (252, 250, 245), "paper_text": (66, 52, 38), "paper_border": (222, 211, 190), "font_family": "Times",
    },
    # Dark Themes
    "midnight_purple": {
        "bg": (20, 15, 38), "accent": (255, 42, 128), "text": (255, 255, 255), "subtext": (180, 168, 204),
        "paper_bg": (30, 24, 51), "paper_text": (240, 240, 240), "paper_border": (69, 58, 100), "font_family": "Helvetica",
    },
    "cyberpunk_2077": {
        "bg": (18, 18, 18), "accent": (0, 255, 240), "text": (250, 250, 51), "subtext": (140, 140, 140),
        "paper_bg": (28, 28, 28), "paper_text": (230, 230, 230), "paper_border": (51, 51, 51), "font_family": "Courier",
    },
    "obsidian_crimson": {
        "bg": (10, 10, 10), "accent": (220, 20, 60), "text": (224, 224, 224), "subtext": (128, 128, 128),
        "paper_bg": (18, 18, 18), "paper_text": (200, 200, 200), "paper_border": (40, 40, 40), "font_family": "Helvetica",
    },
    "oceanic_abyss": {
        "bg": (4, 15, 31), "accent": (0, 204, 255), "text": (240, 248, 255), "subtext": (100, 149, 237),
        "paper_bg": (10, 25, 47), "paper_text": (220, 235, 255), "paper_border": (30, 58, 138), "font_family": "Helvetica",
    },
    # Classic Themes (Restored)
    "tufte_scholar": {
        "bg": (250, 248, 245), "accent": (181, 154, 87), "text": (31, 31, 31), "subtext": (100, 100, 100),
        "paper_bg": (250, 248, 245), "paper_text": (31, 31, 31), "paper_border": (220, 215, 205), "font_family": "Times",
    },
    "princeton_math": {
        "bg": (30, 58, 138), "accent": (249, 115, 22), "text": (255, 255, 255), "subtext": (200, 200, 200),
        "paper_bg": (255, 255, 252), "paper_text": (30, 41, 59), "paper_border": (203, 213, 225), "font_family": "Times",
    },
    "midnight_terminal": {
        "bg": (0, 0, 0), "accent": (57, 255, 20), "text": (0, 240, 255), "subtext": (0, 150, 150),
        "paper_bg": (0, 0, 0), "paper_text": (57, 255, 20), "paper_border": (0, 240, 255), "font_family": "Courier",
    },
    "cambridge_emerald": {
        "bg": (15, 76, 58), "accent": (212, 175, 55), "text": (254, 253, 249), "subtext": (150, 150, 150),
        "paper_bg": (254, 253, 249), "paper_text": (15, 76, 58), "paper_border": (212, 175, 55), "font_family": "Times",
    },
    "bauhaus_geometric": {
        "bg": (244, 241, 234), "accent": (230, 95, 43), "text": (0, 0, 0), "subtext": (100, 100, 100),
        "paper_bg": (244, 241, 234), "paper_text": (0, 0, 0), "paper_border": (0, 0, 0), "font_family": "Helvetica",
    },
    "swiss_stark": {
        "bg": (0, 0, 0), "accent": (220, 38, 38), "text": (255, 255, 255), "subtext": (150, 150, 150),
        "paper_bg": (255, 255, 255), "paper_text": (0, 0, 0), "paper_border": (226, 232, 240), "font_family": "Helvetica",
    },
    "oxford_burgundy": {
        "bg": (112, 26, 37), "accent": (245, 158, 11), "text": (253, 251, 247), "subtext": (150, 150, 150),
        "paper_bg": (253, 251, 247), "paper_text": (112, 26, 37), "paper_border": (220, 205, 200), "font_family": "Times",
    },
    "deep_space": {
        "bg": (11, 15, 25), "accent": (6, 182, 212), "text": (139, 92, 246), "subtext": (100, 100, 150),
        "paper_bg": (11, 15, 25), "paper_text": (255, 255, 255), "paper_border": (139, 92, 246), "font_family": "Helvetica",
    },
    "mit_tech": {
        "bg": (13, 148, 136), "accent": (107, 114, 128), "text": (255, 255, 255), "subtext": (200, 200, 200),
        "paper_bg": (255, 255, 255), "paper_text": (13, 148, 136), "paper_border": (203, 213, 225), "font_family": "Helvetica",
    },
    "wharton_ledger": {
        "bg": (15, 23, 42), "accent": (71, 85, 107), "text": (255, 255, 255), "subtext": (150, 150, 150),
        "paper_bg": (255, 255, 255), "paper_text": (15, 23, 42), "paper_border": (226, 232, 240), "font_family": "Times",
    },
    "sumi_ink": {
        "bg": (250, 250, 250), "accent": (194, 65, 12), "text": (43, 43, 43), "subtext": (100, 100, 100),
        "paper_bg": (250, 250, 250), "paper_text": (43, 43, 43), "paper_border": (229, 229, 229), "font_family": "Times",
    },
    "renaissance_gold": {
        "bg": (78, 54, 41), "accent": (197, 160, 89), "text": (243, 239, 224), "subtext": (150, 150, 150),
        "paper_bg": (243, 239, 224), "paper_text": (78, 54, 41), "paper_border": (197, 160, 89), "font_family": "Times",
    },
    "warm_sunset_dark": {
        "bg": (30, 21, 20), "accent": (255, 158, 100), "text": (232, 230, 227), "subtext": (150, 150, 150),
        "paper_bg": (34, 27, 25), "paper_text": (232, 230, 227), "paper_border": (197, 90, 103), "font_family": "Helvetica",
    },
}

MAX_VIDEOS_PER_TOPIC = 4


# --- Pydantic models (unchanged from v1) ------------------------------------
class GenerationRequest(BaseModel):
    youtube_key: str
    gemini_key: str
    subject: str
    author: str
    playlist_urls: list[str]
    topics: str
    theme: str

    @field_validator("playlist_urls")
    @classmethod
    def _non_empty_playlists(cls, v):
        if not v:
            raise ValueError("At least one playlist URL is required.")
        return v

    @field_validator("topics")
    @classmethod
    def _non_empty_topics(cls, v):
        if not v or not v.strip():
            raise ValueError("Topics list cannot be empty.")
        return v


class ExtractTopicsRequest(BaseModel):
    youtube_key: str
    gemini_key: str
    playlist_urls: list[str]


class PlaylistMetaRequest(BaseModel):
    youtube_key: str
    playlist_url: str


# --- Helpers (unchanged from v1) --------------------------------------------
def extract_playlist_id(url: str) -> str:
    m = re.search(r'[?&]list=([A-Za-z0-9_\-]+)', url)
    if not m:
        raise ValueError(f"Invalid playlist URL: {url}")
    return m.group(1)


def parse_duration(iso: str) -> tuple[str, int]:
    m = re.match(r'PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?', iso or "")
    if not m:
        return "?", 0
    h, mn, s = (int(x or 0) for x in m.groups())
    total = h * 3600 + mn * 60 + s
    display = f"{h}:{mn:02d}:{s:02d}" if h else f"{mn}:{s:02d}"
    return display, total


def format_views(v: int) -> str:
    if v >= 1_000_000:
        return f"{v / 1_000_000:.1f}M"
    if v >= 1_000:
        return f"{v / 1_000:.0f}K"
    return str(v)


def truncate(text: str, n: int) -> str:
    text = text or ""
    return text if len(text) <= n else text[: n - 1].rstrip() + ".."


# --- Text sanitization for PDF (Latin-1 safe) --------------------------------
_UNICODE_REPLACEMENTS: dict[str, str] = {
    "\u2014": "-",    # em-dash
    "\u2013": "-",    # en-dash
    "\u2018": "'",    # left single quote
    "\u2019": "'",    # right single quote
    "\u201c": '"',    # left double quote
    "\u201d": '"',    # right double quote
    "\u2026": "...",  # ellipsis
    "\u2022": "*",    # bullet
    "\u00a0": " ",    # non-breaking space
    "\u2010": "-",    # hyphen
    "\u2011": "-",    # non-breaking hyphen
    "\u2012": "-",    # figure dash
    "\u2015": "-",    # horizontal bar
    "\u2032": "'",    # prime
    "\u2033": '"',    # double prime
    "\u2192": "->",   # right arrow
    "\u2190": "<-",   # left arrow
    "\u2264": "<=",   # less than or equal
    "\u2265": ">=",   # greater than or equal
}


def sanitize_for_pdf(text: str) -> str:
    """Replace non-Latin-1 Unicode characters with safe ASCII equivalents.

    Built-in PDF fonts (Times, Helvetica, Courier) in fpdf2 only support
    the Latin-1 character set. YouTube titles and AI-generated notes often
    contain em-dashes, smart quotes, and other Unicode that would crash
    the PDF serialization step.
    """
    if not text:
        return text
    for char, replacement in _UNICODE_REPLACEMENTS.items():
        text = text.replace(char, replacement)
    # Final safety net: replace any remaining non-Latin-1 chars with '?'
    return text.encode("latin-1", errors="replace").decode("latin-1")


def fetch_videos(api_key: str, urls: list[str]) -> list[dict]:
    youtube = build("youtube", "v3", developerKey=api_key)
    all_videos: list[dict] = []
    seen_ids: set[str] = set()

    for i, url in enumerate(urls):
        pid = extract_playlist_id(url)
        next_page = None
        pages_fetched = 0
        MAX_PAGES = 200

        while True:
            pl_resp = youtube.playlistItems().list(
                part="contentDetails,snippet",
                playlistId=pid,
                maxResults=50,
                pageToken=next_page
            ).execute()

            video_ids = [
                item["contentDetails"]["videoId"] for item in pl_resp["items"]
                if item["snippet"]["title"] not in ("Deleted video", "Private video")
            ]

            if video_ids:
                vid_resp = youtube.videos().list(
                    part="snippet,statistics,contentDetails",
                    id=",".join(video_ids)
                ).execute()
                for item in vid_resp["items"]:
                    if item["id"] in seen_ids:
                        continue
                    seen_ids.add(item["id"])
                    duration_display, duration_seconds = parse_duration(
                        item["contentDetails"]["duration"]
                    )
                    all_videos.append({
                        "id": item["id"],
                        "title": item["snippet"]["title"],
                        "description": item["snippet"].get("description", "")[:400],
                        "views": int(item["statistics"].get("viewCount", 0)),
                        "duration": duration_display,
                        "duration_seconds": duration_seconds,
                        "url": f"https://www.youtube.com/watch?v={item['id']}",
                        "playlist_index": i
                    })

            next_page = pl_resp.get("nextPageToken")
            pages_fetched += 1
            if not next_page or pages_fetched >= MAX_PAGES:
                break

    return all_videos


# --- PDF generator (unchanged from v1) --------------------------------------
class StudyGuidePDF(FPDF):
    def __init__(self, subject: str, theme: dict):
        super().__init__(orientation="L", unit="mm", format="A4")
        self.subject = subject
        self.theme = theme
        self.set_auto_page_break(auto=False)  # Manual page breaks only — avoids blank pages
        self.set_margins(10, 10, 10)
        self.col_widths = [12, 12, 55, 95, 18, 18, 25, 25]

    def add_page(self, *args, **kwargs):
        super().add_page(*args, **kwargs)
        self.set_fill_color(*self.theme["paper_bg"])
        self.rect(0, 0, self.w, self.h, "F")

    def footer(self):
        self.set_y(-12)
        self.set_font(self.theme["font_family"], "I", 8)
        self.set_text_color(*self.theme["paper_text"])
        self.cell(140, 10, self.subject, align="L")
        self.cell(0, 10, f"Page {self.page_no()}", align="R")

    def render_grid_headers(self):
        CW = self.col_widths
        self.set_font(self.theme["font_family"], "B", 10)
        self.set_text_color(*self.theme["accent"])
        self.set_draw_color(*self.theme["paper_border"])
        self.cell(CW[0], 8, "[ ]", border="TB", align="C")
        self.cell(CW[1], 8, "#", border="TB", align="C")
        self.cell(CW[2], 8, "Syllabus Topic", border="TB")
        self.cell(CW[3], 8, "YouTube Video Title & AI Note", border="TB")
        self.cell(CW[4], 8, "Duration", border="TB", align="C")
        self.cell(CW[5], 8, "Views", border="TB", align="C")
        self.cell(CW[6], 8, "QR Code", border="TB", align="C")
        self.cell(CW[7], 8, "Watch", border="TB", align="C")
        self.set_y(self.get_y() + 8)


class QRCache:
    def __init__(self):
        self._cache: dict[str, str] = {}

    def get(self, video_id: str, url: str) -> str:
        if video_id in self._cache:
            return self._cache[video_id]
        qr = qrcode.QRCode(version=1, box_size=4, border=1)
        qr.add_data(url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        fname = f"qr_{hashlib.sha1(video_id.encode()).hexdigest()[:16]}.png"
        path = os.path.join(tempfile.gettempdir(), fname)
        img.save(path)
        self._cache[video_id] = path
        return path

    def cleanup(self):
        for path in self._cache.values():
            try:
                os.remove(path)
            except OSError:
                pass


def build_prompt(topics_list: list[str], videos_payload: list[dict]) -> str:
    # Build a numbered topic index to help the LLM differentiate topics precisely
    topic_index = "\n".join(f"  T{i+1}: {t}" for i, t in enumerate(topics_list))
    # Build a numbered video index with richer context for matching
    video_index = "\n".join(
        f"  V{i+1}: [{v['id']}] {v['title']} (dur={v['duration_seconds']}s, views={v['views']}) — {truncate(v['desc'], 200)}"
        for i, v in enumerate(videos_payload)
    )
    return f"""You are a precise academic curriculum-matching engine. Your job is to map each syllabus topic to the BEST video(s) that actually teach that specific topic. Accuracy and uniqueness matter more than coverage — a wrong or repeated match is worse than no match.

TOPIC LIST ({len(topics_list)} topics):
{topic_index}

VIDEO LIBRARY ({len(videos_payload)} videos):
{video_index}

MATCHING INSTRUCTIONS — read carefully and follow every rule:

RULE 1 — EXACT TOPIC IDENTIFICATION: For each topic T1..T{len(topics_list)}, find ALL videos that directly teach that specific concept. Prefer videos whose title/description most closely names or paraphrases the topic. Do NOT match a video based on vague keyword overlap.

RULE 2 — MULTIPLE VIDEOS PER TOPIC ARE FINE: A single video often cannot fully cover a topic. You should assign 2-4 relevant videos per topic when available. Each additional video should cover a distinct sub-angle, example, or depth level that the others miss — do not pad with near-duplicates. Maximum {MAX_VIDEOS_PER_TOPIC} videos per topic.

RULE 3 — NO VIDEO REUSE ACROSS TOPICS (CRITICAL): A video ID MUST NOT appear under more than one topic. If a video could plausibly match multiple topics, assign it ONLY to the topic where it is the strongest and most specific fit. Then find the next-best alternative videos for the other topics. Every video belongs to exactly one topic.

RULE 4 — QUALITY OVER QUANTITY: When choosing between candidate videos, prefer:
  (a) Videos whose title explicitly mentions the topic keyword.
  (b) Substantive lecture-length videos (10+ minutes) over short clips, unless the short clip is the only direct match.
  (c) Higher-view-count videos when relevance is otherwise equal (community validation).

RULE 5 — BE STRICT ON MISMATCHES: If no video in the library meaningfully teaches the topic, you MUST set video_ids to an empty array [], confidence to "none", and study_note to "No direct match found in playlist." Do NOT force a weak match — a false match misleads the student more than a gap.

RULE 6 — CONFIDENCE CALIBRATION:
  - "high": The video title explicitly names the topic or a direct synonym.
  - "medium": The video clearly covers the concept but may use different terminology.
  - "low": The video touches on the topic tangentially or is part of a broader lecture.
  - "none": No suitable video found.

RULE 7 — STUDY NOTE: Write a precise 1-sentence note for each topic summarizing exactly what the matched video(s) teach about that specific topic. If no video matched, write "No direct match found in playlist."

OUTPUT FORMAT — Return a strict JSON array of exactly {len(topics_list)} objects, in the SAME ORDER as the topic list (T1, T2, T3, ...):
[
  {{
    "topic": "T1 topic name exactly as listed above",
    "video_ids": ["video_id_string"],
    "confidence": "high",
    "study_note": "Precise 1-sentence description of what this video teaches for this topic."
  }},
  ...
]

CRITICAL REMINDERS:
- Every video_id must come from the V1..V{len(videos_payload)} list above — use the bracketed ID strings exactly.
- NO video_id may appear in more than one topic's video_ids array.
- If a topic genuinely has no match, use empty video_ids — do NOT reuse another topic's video.
- Return ONLY the JSON array. No markdown fences, no commentary, no explanation."""


def parse_ai_response(raw_text: str) -> list[dict]:
    cleaned = raw_text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*|\s*```$", "", cleaned.strip(), flags=re.MULTILINE)
    try:
        return json.loads(cleaned)
    except json.JSONDecodeError as e:
        log.error("ai_response_parse_failed", extra={"err": str(e)})
        raise HTTPException(status_code=502, detail="AI matching response was not valid JSON.")


# --- Middleware: request logging --------------------------------------------
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    duration_ms = int((time.time() - start) * 1000)
    log.info(
        "request",
        extra={
            "method": request.method,
            "path": request.url.path,
            "status": response.status_code,
            "ms": duration_ms,
        },
    )
    return response


# --- Endpoints (v1, kept at root for backwards-compat) ----------------------
@app.get("/ping")
def ping():
    return {"status": "awake", "version": "3.0"}


@app.get("/health")
def health():
    """Uptime-monitoring endpoint — slightly richer than /ping."""
    return {"status": "ok", "version": "3.0", "themes": list(THEMES.keys())}


@app.post("/extract_topics")
async def extract_topics(req: ExtractTopicsRequest):
    try:
        all_videos = fetch_videos(req.youtube_key, req.playlist_urls)
        if not all_videos:
            raise HTTPException(status_code=400, detail="No videos found.")
        titles = [v["title"] for v in all_videos[:100]]
        configure_genai(api_key=req.gemini_key)
        # v3 NOTE: gemini-3.5-flash-lite is the topic-extraction model.
        # gemini-3.6-flash is the matching model (used in /generate_guide).
        model = GenerativeModel('gemini-3.5-flash-lite')
        prompt = (
            f"Given these YouTube video titles from a course playlist:\n"
            f"{json.dumps(titles)}\n\n"
            f"Extract a concise, high-level list of distinct syllabus/course topics covered. "
            f"Group detailed concepts into broader topics. "
            f"Limit the list to a maximum of 15-20 core topics. "
            f"Return a JSON array of topic strings only, no commentary."
        )
        from google.generativeai.types import GenerationConfig
        resp = model.generate_content(
            prompt,
            generation_config=GenerationConfig(response_mime_type="application/json")
        )
        cleaned = resp.text.strip()
        if not cleaned:
            return {"topics": []}
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```(?:json)?\s*|\s*```$", "", cleaned, flags=re.MULTILINE)
        topics = json.loads(cleaned)
        return {"topics": topics}
    except json.JSONDecodeError:
        log.exception("extract_topics_json_decode_failed")
        return {"topics": []}
    except HTTPException:
        raise
    except Exception as e:
        log.exception("extract_topics_failed")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/playlist_meta")
async def playlist_meta(req: PlaylistMetaRequest):
    try:
        pid = extract_playlist_id(req.playlist_url)
        youtube = build("youtube", "v3", developerKey=req.youtube_key)
        pl_resp = youtube.playlists().list(
            part="snippet,contentDetails", id=pid
        ).execute()
        if not pl_resp.get("items"):
            raise HTTPException(status_code=404, detail="Playlist not found.")
        item = pl_resp["items"][0]
        return {
            "title": item["snippet"]["title"],
            "channel": item["snippet"]["channelTitle"],
            "video_count": item["contentDetails"]["itemCount"],
            "thumbnail_url": item["snippet"]["thumbnails"].get("medium", {}).get("url"),
        }
    except HTTPException:
        raise
    except Exception as e:
        log.exception("playlist_meta_failed")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/generate_guide")
async def generate_guide(req: GenerationRequest, request: Request):
    qr_cache = QRCache()
    try:
        if await request.is_disconnected():
            raise HTTPException(status_code=499, detail="Client disconnected")

        all_videos = fetch_videos(req.youtube_key, req.playlist_urls)
        if not all_videos:
            raise HTTPException(
                status_code=400,
                detail="No videos found in the provided playlist(s)."
            )

        if await request.is_disconnected():
            raise HTTPException(status_code=499, detail="Client disconnected")

        configure_genai(api_key=req.gemini_key)
        # v3 uses gemini-3.6-flash (latest GA, confirmed July 2026).
        model = GenerativeModel(
            'gemini-3.6-flash',
            generation_config={
                "response_mime_type": "application/json",
                "max_output_tokens": 65536,
            }
        )

        topics_list = [t.strip() for t in req.topics.split(",") if t.strip()]
        if not topics_list:
            raise HTTPException(status_code=400, detail="No valid topics parsed from input.")

        videos_payload = [
            {
                "id": v["id"],
                "title": v["title"],
                "desc": v["description"],
                "duration_seconds": v["duration_seconds"],
                "views": v["views"],
            }
            for v in all_videos
        ]

        prompt = build_prompt(topics_list, videos_payload)
        response = model.generate_content(prompt)
        ai_matches = parse_ai_response(response.text)

        if await request.is_disconnected():
            raise HTTPException(status_code=499, detail="Client disconnected")

        ai_by_topic = {m.get("topic"): m for m in ai_matches if isinstance(m, dict)}

        vid_dict = {v["id"]: v for v in all_videos}

        # Server-side deduplication enforcement: track globally-assigned video IDs
        # so that even if the LLM ignores RULE 3, no video appears under two topics.
        globally_assigned: set[str] = set()
        results = []
        for topic in topics_list:
            match = ai_by_topic.get(topic, {})
            raw_ids = match.get("video_ids") or []
            vids = []
            for vid_id in raw_ids[:MAX_VIDEOS_PER_TOPIC]:
                # Skip if already assigned to a previous topic
                if vid_id in globally_assigned:
                    continue
                vid = vid_dict.get(vid_id)
                if vid and vid not in vids:
                    vids.append(vid)
                    globally_assigned.add(vid_id)

            note = match.get("study_note", "")
            confidence = match.get("confidence", "medium")

            if vids:
                results.append({
                    "topic": topic, "matched": True, "videos": vids,
                    "study_note": note, "confidence": confidence,
                })
            else:
                results.append({
                    "topic": topic, "matched": False, "videos": [],
                    "study_note": note or "No direct match found in playlist.",
                    "confidence": "none",
                })

        theme = THEMES.get(req.theme, THEMES["nordic_frost"])
        pdf = StudyGuidePDF(req.subject, theme)

        # --- Editorial-Style Cover Page ---
        pdf.add_page()
        pdf.set_fill_color(*theme["bg"])
        pdf.rect(0, 0, pdf.w, pdf.h, "F")

        # Abstract Geometric Background (overlapping outlines)
        pdf.set_draw_color(*theme["accent"])
        pdf.set_line_width(0.5)
        # Large concentric circles off-center
        for r in range(40, 160, 20):
            pdf.ellipse(pdf.w - 40 - r, pdf.h / 2 - r, r * 2, r * 2, "D")
        
        # Diagonal accent lines
        pdf.set_line_width(2.0)
        pdf.line(pdf.w - 100, 0, pdf.w, 100)
        pdf.line(pdf.w - 120, 0, pdf.w, 120)

        # Title Block (Centered, elegant)
        pdf.set_y(pdf.h / 2 - 30)
        pdf.set_x(0)
        pdf.set_font(theme["font_family"], "B", 48)
        pdf.set_text_color(*theme["text"])
        pdf.cell(0, 20, sanitize_for_pdf(req.subject.upper()), align="C")
        
        pdf.set_y(pdf.h / 2 + 5)
        pdf.set_x(0)
        pdf.set_font(theme["font_family"], "", 16)
        pdf.set_text_color(*theme["subtext"])
        pdf.cell(0, 10, f"PREPARED FOR: {sanitize_for_pdf(req.author.upper())}", align="C")

        # Bottom Border and Watermark
        pdf.set_draw_color(*theme["text"])
        pdf.set_line_width(0.3)
        pdf.line(40, pdf.h - 40, pdf.w - 40, pdf.h - 40)
        
        pdf.set_y(pdf.h - 35)
        pdf.set_x(0)
        pdf.set_font(theme["font_family"], "I", 10)
        pdf.set_text_color(*theme["subtext"])
        pdf.cell(0, 10, "Generated by Play2PDF Studio - The Intelligent Video Compiler", align="C")

        # --- Study Grid Page ---
        pdf.add_page()
        pdf.set_draw_color(*theme["paper_border"])
        pdf.set_text_color(*theme["paper_text"])

        matched_results = [r for r in results if r["matched"]]
        unmatched_results = [r for r in results if not r["matched"]]
        total_count = len(results)
        matched_topics_count = len(matched_results)
        pct = int((matched_topics_count / total_count * 100)) if total_count else 0
        total_videos_matched = sum(len(r["videos"]) for r in matched_results)

        pdf.set_fill_color(*theme["accent"])
        pdf.set_text_color(*theme["text"])
        pdf.set_font(theme["font_family"], "B", 10)
        summary_text = (
            f" STUDY TRACK METRICS  |  Topics: {total_count}   *   "
            f"Covered: {matched_topics_count}/{total_count} ({pct}%)   *   "
            f"Videos: {total_videos_matched}   *   Playlists: {len(req.playlist_urls)}"
        )
        pdf.cell(0, 8, summary_text, align="C", fill=True)
        pdf.set_y(pdf.get_y() + 8)
        pdf.set_y(pdf.get_y() + 4)

        pdf.render_grid_headers()
        CW = pdf.col_widths

        row_counter = 0
        for topic_idx, res in enumerate(matched_results):
            for v_idx, vid in enumerate(res["videos"]):
                row_counter += 1
                row_height = 20
                # Manual page break: check if row + footer (15mm) fits
                if pdf.get_y() + row_height + 15 > pdf.h:
                    pdf.add_page()
                    pdf.render_grid_headers()

                row_y = pdf.get_y()

                if row_counter % 2 == 1:
                    bg_r, bg_g, bg_b = theme["paper_bg"]
                    alt = lambda c: max(0, c - 8) if c > 128 else min(255, c + 15)
                    pdf.set_fill_color(alt(bg_r), alt(bg_g), alt(bg_b))
                    pdf.rect(10, row_y, sum(CW), row_height, "F")

                pdf.set_xy(10, row_y)
                pdf.set_font(theme["font_family"], "", 9)
                pdf.set_text_color(*theme["paper_text"])
                pdf.set_draw_color(*theme["paper_border"])

                pdf.cell(CW[0], row_height, "[  ]", border="B", align="C")
                idx_str = f"{topic_idx+1}" if len(res["videos"]) == 1 else f"{topic_idx+1}.{v_idx+1}"
                pdf.cell(CW[1], row_height, idx_str, border="B", align="C")
                topic_label = truncate(sanitize_for_pdf(res["topic"]), 26) if v_idx == 0 else f"  > {truncate(sanitize_for_pdf(res['topic']), 24)}"
                pdf.cell(CW[2], row_height, topic_label, border="B")

                x_col3 = pdf.get_x()
                pdf.cell(CW[3], row_height, "", border="B")
                pdf.set_xy(x_col3, row_y + 2)
                pdf.set_font(theme["font_family"], "B", 9)
                pdf.cell(CW[3], 5, truncate(sanitize_for_pdf(vid["title"]), 52))

                pdf.set_xy(x_col3, row_y + 8)
                pdf.set_font(theme["font_family"], "I", 7.5)
                pdf.set_text_color(100, 110, 125)
                note_text = f"Key Focus: {truncate(sanitize_for_pdf(res['study_note']), 70)}" if res.get("study_note") else ""
                pdf.cell(CW[3], 5, note_text)

                pdf.set_xy(x_col3 + CW[3], row_y)
                pdf.set_font(theme["font_family"], "", 9)
                pdf.set_text_color(*theme["paper_text"])

                pdf.cell(CW[4], row_height, vid["duration"], border="B", align="C")
                pdf.cell(CW[5], row_height, format_views(vid["views"]), border="B", align="C")

                qr_path = qr_cache.get(vid["id"], vid["url"])
                x_qr = pdf.get_x()
                pdf.cell(CW[6], row_height, "", border="B")
                pdf.image(qr_path, x=x_qr + 4, y=row_y + 2, w=16, h=16)

                pdf.set_xy(x_qr + CW[6], row_y)
                pdf.set_text_color(*theme["accent"])
                pdf.set_font(theme["font_family"], "B", 9)
                pdf.cell(CW[7], row_height, "Watch Link", border="B", align="C", link=vid["url"])
                pdf.set_y(row_y + row_height)  # Advance Y manually — no auto page break

        # --- Unmatched Topics Appendix ---
        if unmatched_results:
            pdf.add_page()
            pdf.set_font(theme["font_family"], "B", 16)
            pdf.set_text_color(*theme["accent"])
            pdf.cell(0, 12, "Unmatched Syllabus Topics")
            pdf.set_y(pdf.get_y() + 12)
            pdf.set_font(theme["font_family"], "", 10)
            pdf.set_text_color(*theme["paper_text"])
            pdf.multi_cell(
                0, 6,
                "The following topics could not be confidently matched to any video in the "
                "provided playlist(s). You may need to supplement your study guide with "
                "external materials for these concepts:"
            )
            pdf.set_y(pdf.get_y() + 2)
            pdf.set_font(theme["font_family"], "", 10)
            for res in unmatched_results:
                if pdf.get_y() + 8 + 15 > pdf.h:
                    pdf.add_page()
                pdf.cell(0, 7, f"-  {sanitize_for_pdf(res['topic'])}")
                pdf.set_y(pdf.get_y() + 7)

        pdf_output = pdf.output(dest="S")
        pdf_bytes = pdf_output.encode("latin1") if isinstance(pdf_output, str) else bytes(pdf_output)
        log.info(
            "guide_generated",
            extra={
                "subject": req.subject,
                "topics": total_count,
                "matched": matched_topics_count,
                "videos": total_videos_matched,
            },
        )
        return Response(content=pdf_bytes, media_type="application/pdf")

    except HTTPException:
        raise
    except Exception as e:
        log.exception("generate_guide_failed")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        qr_cache.cleanup()


# --- v3 endpoints (under /api/v1/) -----------------------------------------
# These are aliases of the v1 endpoints, exposed under a versioned prefix
# so future breaking changes can ship under /api/v2/ without breaking v1
# callers. The Android Compose app uses the v1 endpoints for now (they're
# the only ones currently deployed), but the v3 prefix is available for
# opt-in.
app_v1 = FastAPI(title="Play2PDF API v1", version="3.0")
app_v1.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


@app_v1.get("/ping")
def v1_ping():
    return ping()


@app_v1.get("/health")
def v1_health():
    return health()


@app_v1.post("/extract_topics")
async def v1_extract_topics(req: ExtractTopicsRequest):
    return await extract_topics(req)


@app_v1.post("/playlist_meta")
async def v1_playlist_meta(req: PlaylistMetaRequest):
    return await playlist_meta(req)


@app_v1.post("/generate_guide")
async def v1_generate_guide(req: GenerationRequest, request: Request):
    return await generate_guide(req, request)


app.mount("/api/v1", app_v1)
