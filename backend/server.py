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
    """JSON lines with the record's `extra` fields preserved.

    The old version dropped everything passed via `extra={...}` — the
    method/path/status/ms request metrics documented in the README never
    reached the logs.
    """

    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, Any] = {
            "ts": int(record.created),
            "level": record.levelname,
            "logger": record.name,
            "msg": record.getMessage(),
        }
        for key, value in record.__dict__.items():
            if key not in (
                "name", "msg", "args", "levelname", "levelno", "pathname",
                "filename", "module", "exc_info", "exc_text", "stack_info",
                "lineno", "funcName", "created", "msecs", "relativeCreated",
                "thread", "threadName", "processName", "process", "taskName",
                "message", "asctime",
            ) and not key.startswith("_"):
                try:
                    json.dumps(value)
                    payload[key] = value
                except (TypeError, ValueError):
                    payload[key] = repr(value)
        if record.exc_info:
            payload["exc"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False, default=str)

logging.basicConfig(level=logging.INFO)
handler = logging.StreamHandler()
handler.setFormatter(JsonFormatter())
logging.getLogger().handlers = [handler]
log = logging.getLogger("play2pdf")
# fpdf2 embeds fonts via fontTools, whose subsetter logs every subset at
# INFO — pure noise in the HF Space log stream. Keep warnings only.
logging.getLogger("fontTools.subset").setLevel(logging.WARNING)
logging.getLogger("fontTools").setLevel(logging.WARNING)


# --- FastAPI app + middleware ------------------------------------------------
app = FastAPI(title="Play2PDF API", version="3.1")

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
        "paper_bg": (255, 255, 255), "paper_text": (52, 58, 66), "paper_border": (203, 213, 225), "font_family": "Helvetica",
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
    # "portrait" (default, v3.1 redesign) or "grid_landscape" (legacy
    # pre-3.1 checklist grid). Optional so old clients change nothing.
    layout: str = "portrait"

    @field_validator("layout")
    @classmethod
    def _known_layout(cls, v):
        v = (v or "portrait").strip().lower()
        if v not in ("portrait", "grid_landscape"):
            raise ValueError("layout must be 'portrait' or 'grid_landscape'")
        return v

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

    NOTE (D1): the renderer now embeds Noto fonts and calls
    [prepare_text] instead — which applies these replacements and then
    per-glyph '?' substitution ONLY for characters the chosen font lacks.
    This function remains the ASCII-mapping table both paths share.
    """
    if not text:
        return text
    for char, replacement in _UNICODE_REPLACEMENTS.items():
        text = text.replace(char, replacement)
    # Final safety net: replace any remaining non-Latin-1 chars with '?'
    return text.encode("latin-1", errors="replace").decode("latin-1")


# --- Embedded Unicode fonts (D1) ------------------------------------------------
# Vendored under backend/fonts/ — Noto families with real glyph coverage so
# Bengali/Cyrillic/Greek titles render as text instead of '?' runs. PDFs
# stay searchable + copyable; fpdf2 subsets the fonts per document.
FONT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts")

_FONT_FILES: dict[str, dict[str, str]] = {
    "sans": {
        "": "NotoSans-Regular.ttf", "B": "NotoSans-Bold.ttf",
        "I": "NotoSans-Regular.ttf", "BI": "NotoSans-Bold.ttf",  # no italic cut — upright substitution
    },
    "serif": {
        "": "NotoSerif-Regular.ttf", "B": "NotoSerif-Bold.ttf",
        "I": "NotoSerif-Regular.ttf", "BI": "NotoSerif-Bold.ttf",
    },
    "bengali": {
        "": "NotoSansBengali-Regular.ttf", "B": "NotoSansBengali-Bold.ttf",
        "I": "NotoSansBengali-Regular.ttf", "BI": "NotoSansBengali-Bold.ttf",
    },
}

# Bengali block. (Devanagari/Thamil etc. would need their own families —
# out of scope; they degrade to '?' via prepare_text instead of crashing.)
_BENGALI = (0x0980, 0x09FF)


def register_fonts(pdf: FPDF) -> None:
    """Register every embedded family/style once on the document."""
    for family, styles in _FONT_FILES.items():
        for style, fname in styles.items():
            path = os.path.join(FONT_DIR, fname)
            if os.path.exists(path):
                pdf.add_font(family, style, path)


def theme_font_family(theme: dict) -> str:
    """Map a theme's legacy core-font hint to an embedded family.

    'Times' (serif character) -> NotoSerif; Helvetica/Courier -> NotoSans.
    """
    return "serif" if theme.get("font_family") == "Times" else "sans"


def font_for_text(text: str, base: str) -> str:
    """Pick the family that actually covers the string's script."""
    if any(_BENGALI[0] <= ord(c) <= _BENGALI[1] for c in (text or "")):
        return "bengali"
    return base


def prepare_text(text: str, pdf: FPDF, family: str) -> str:
    """Make *text* renderable in *family*: keep every glyph the font
    actually covers (em-dashes, curly quotes, accents, Bengali... render
    as themselves), ASCII-fold the ones it lacks (<=, ->, ...), and only
    then fall back to '?' — never tofu.
    """
    if not text:
        return text
    font = pdf.fonts.get(family)
    if font is None or not getattr(font, "cmap", None):
        # Core-font document (no embedded family resolved) — full fold.
        return sanitize_for_pdf(text)
    cmap = font.cmap
    out = []
    for c in text:
        if c in "\n\r\t" or ord(c) in cmap:
            out.append(c)
        else:
            out.append(_UNICODE_REPLACEMENTS.get(c, "?"))
    return "".join(out)


# --- WCAG-safe derived colours (D3) ---------------------------------------------
def _rel_lum(rgb: tuple[int, int, int]) -> float:
    def f(v: int) -> float:
        v = v / 255.0
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(rgb[0]) + 0.7152 * f(rgb[1]) + 0.0722 * f(rgb[2])


def contrast_ratio(fg: tuple[int, int, int], bg: tuple[int, int, int]) -> float:
    la, lb = _rel_lum(fg), _rel_lum(bg)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def _blend(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def readable_on(
    base: tuple[int, int, int], bg: tuple[int, int, int], target: float = 4.5
) -> tuple[int, int, int]:
    """Nudge *base* toward black or white (whichever is closer) until it
    reaches *target* contrast on *bg*. Returns *base* unchanged if it
    already passes. Preserves hue as much as possible.
    """
    if contrast_ratio(base, bg) >= target:
        return base
    dark_hit = light_hit = None  # (blend_t, colour)
    for step in range(1, 21):
        t = step / 20
        cand = _blend(base, (0, 0, 0), t)
        if contrast_ratio(cand, bg) >= target:
            dark_hit = (t, cand)
            break
    for step in range(1, 21):
        t = step / 20
        cand = _blend(base, (255, 255, 255), t)
        if contrast_ratio(cand, bg) >= target:
            light_hit = (t, cand)
            break
    if dark_hit and light_hit:
        return dark_hit[1] if dark_hit[0] <= light_hit[0] else light_hit[1]
    if dark_hit:
        return dark_hit[1]
    if light_hit:
        return light_hit[1]
    return base


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


# --- PDF generator ------------------------------------------------------------
class StudyGuidePDF(FPDF):
    def __init__(self, subject: str, theme: dict, orientation: str = "L"):
        super().__init__(orientation=orientation, unit="mm", format="A4")
        # NOT `self.subject` — fpdf2 stores document metadata in self.subject
        # (set via set_subject), which would clobber the footer text.
        self.guide_subject = subject
        self.theme = theme
        self.set_auto_page_break(auto=False)  # Manual page breaks only — avoids blank pages
        self.set_margins(10, 10, 10)
        self.col_widths = [12, 12, 55, 95, 18, 18, 25, 25]

        # D1: embedded Unicode fonts + the theme's resolved base family.
        register_fonts(self)
        self.base_family = theme_font_family(theme)

        # D3: WCAG-safe derived foregrounds — every text colour the renderer
        # paints comes from this dict, and theme_text_pairs() derives the
        # same values, so the audit can never drift from reality.
        paper = theme["paper_bg"]
        self.fg = {
            "cover_title": readable_on(theme["text"], theme["bg"]),
            "cover_sub":   readable_on(theme["subtext"], theme["bg"]),
            "summary":     readable_on(theme["text"], theme["accent"]),
            "header":      readable_on(theme["accent"], paper),
            "body":        readable_on(theme["paper_text"], paper),
            "link":        readable_on(theme["accent"], paper),
        }

    def add_page(self, *args, **kwargs):
        super().add_page(*args, **kwargs)
        self.set_fill_color(*self.theme["paper_bg"])
        self.rect(0, 0, self.w, self.h, "F")

    def footer(self):
        self.set_y(-12)
        subj_fam = font_for_text(self.guide_subject, self.base_family)
        self.set_font(subj_fam, "I", 8)
        self.set_text_color(*self.fg["body"])
        self.cell(140, 10, prepare_text(self.guide_subject, self, subj_fam), align="L")
        self.cell(0, 10, f"Page {self.page_no()}", align="R")

    def render_grid_headers(self):
        CW = self.col_widths
        self.set_font(self.base_family, "B", 10)
        self.set_text_color(*self.fg["header"])
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
    return f"""You are an academic curriculum matching AI. Match each of the {len(topics_list)} syllabus topics to the most relevant YouTube video(s) from the provided list, to build a complete study guide.

Topics ({len(topics_list)} items, in order): {json.dumps(topics_list)}
Videos ({len(videos_payload)} items): {json.dumps(videos_payload)}

Matching Rules:
1. Primary factor: direct semantic coverage of the topic's core concept in the video's title/description.
2. A topic may be matched to ONE or MULTIPLE videos (max {MAX_VIDEOS_PER_TOPIC}). If a topic is broad, feel free to include several relevant videos that collectively cover it well.
3. Quality preference: each video includes "duration_seconds" and "views". When multiple videos are similarly relevant, prefer substantive lecture-length videos over very short (<3 minute) teaser/intro clips, unless the short clip is clearly the best or only match.
4. Avoid reusing the exact same video for many unrelated topics; only reuse a video across topics if it genuinely covers both (e.g. a combined "Big-O / Big-Omega / Big-Theta" lecture covering three separate notation topics is fine to reuse).
5. Strictness: if no video meaningfully addresses the topic, set "video_ids": [], "confidence": "none", and "study_note": "No direct match found in playlist."
6. Provide a 1-sentence, concise "study_note" per topic describing the key concept(s) the matched video(s) teach.

Return a strict JSON array of exactly {len(topics_list)} objects, in the SAME ORDER as the topics list above:
[
  {{
    "topic": "Syllabus topic name",
    "video_ids": ["video_id_1", "video_id_2"],
    "confidence": "high|medium|low|none",
    "study_note": "1 concise sentence summarizing what key concepts are taught."
  }}
]
Return ONLY the JSON array, no markdown fences, no commentary."""


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
    return {"status": "awake", "version": "3.1"}


@app.get("/health")
def health():
    """Uptime-monitoring endpoint — slightly richer than /ping."""
    return {"status": "ok", "version": "3.1", "themes": list(THEMES.keys())}


@app.get("/themes")
def themes():
    """The real, server-authoritative theme palettes.

    The in-app mini previews were hand-tuned in Kotlin and drifted from
    the actual PDF output on 11 of 21 themes (e.g. princeton_math
    previews white but its cover is deep blue #1E3A8A). Clients should
    render previews from THIS endpoint — single source of truth.

    Each theme exposes both surfaces the PDF actually paints: the cover
    (bg/text/subtext/accent) and the grid page (paper_bg/paper_text/
    paper_border/accent). Colours are [r, g, b] lists (0-255).
    """
    payload = {}
    for name, t in THEMES.items():
        payload[name] = {
            "font_family": t["font_family"],
            "cover": {
                "bg": list(t["bg"]),
                "text": list(t["text"]),
                "subtext": list(t["subtext"]),
                "accent": list(t["accent"]),
            },
            "page": {
                "bg": list(t["paper_bg"]),
                "text": list(t["paper_text"]),
                "border": list(t["paper_border"]),
                "accent": list(t["accent"]),
            },
        }
    return {"version": "3.1", "themes": payload}


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

        # Newline-delimited topics (current Android clients): a topic may
        # itself contain a comma ("Big-O, Big-Theta"). Fall back to comma-
        # splitting for old clients that send one comma-separated line.
        if "\n" in req.topics:
            candidates = req.topics.split("\n")
        else:
            candidates = req.topics.split(",")
        topics_list = [t.strip() for t in candidates if t.strip()]
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
        results = []
        for topic in topics_list:
            match = ai_by_topic.get(topic, {})
            raw_ids = match.get("video_ids") or []
            vids = []
            for vid_id in raw_ids[:MAX_VIDEOS_PER_TOPIC]:
                vid = vid_dict.get(vid_id)
                if vid and vid not in vids:
                    vids.append(vid)

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

        pdf_bytes = render_guide_pdf(
            subject=req.subject,
            author=req.author,
            playlist_urls=req.playlist_urls,
            results=results,
            theme_name=req.theme,
            layout=req.layout,
        )

        matched_results = [r for r in results if r["matched"]]
        log.info(
            "guide_generated",
            extra={
                "subject": req.subject,
                "topics": len(results),
                "matched": len(matched_results),
                "videos": sum(len(r["videos"]) for r in matched_results),
            },
        )
        return Response(content=pdf_bytes, media_type="application/pdf")

    except HTTPException:
        raise
    except Exception as e:
        log.exception("generate_guide_failed")
        raise HTTPException(status_code=500, detail=str(e))


# --- PDF rendering (pure — exercised by test_themes.py for every theme) -----

def _set_pdf_metadata(pdf: FPDF, subject: str, author: str) -> None:
    """D6 — proper document metadata (title/author show in viewers + search)."""
    pdf.set_title(subject)
    pdf.set_author(author)
    pdf.set_creator("Play2PDF - The Intelligent Video Compiler")
    pdf.set_subject("YouTube study guide")
    pdf.set_keywords(f"{subject}, study guide, youtube, syllabus")


def render_guide_pdf(
    subject: str,
    author: str,
    playlist_urls: list[str],
    results: list[dict],
    theme_name: str,
    layout: str = "portrait",
) -> bytes:
    """Render the study guide PDF from matched *results*.

    Pure with respect to network and AI APIs — the endpoint handles the
    YouTube + Gemini calls and hands over finished match data. This seam
    is what test_themes.py exercises across all 21 themes x 2 layouts.
    """
    if layout == "grid_landscape":
        return _render_landscape(subject, author, playlist_urls, results, theme_name)
    return _render_portrait(subject, author, playlist_urls, results, theme_name)


def _render_landscape(
    subject: str,
    author: str,
    playlist_urls: list[str],
    results: list[dict],
    theme_name: str,
) -> bytes:
    """Legacy pre-3.1 output: landscape A4, 8-column checklist grid.
    Kept verbatim for `layout: "grid_landscape"` clients."""
    theme = THEMES.get(theme_name, THEMES["nordic_frost"])
    qr_cache = QRCache()
    try:
        pdf = StudyGuidePDF(subject, theme)
        base = pdf.base_family
        fg = pdf.fg
        _set_pdf_metadata(pdf, subject, author)

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
        title_fam = font_for_text(subject, base)
        pdf.set_y(pdf.h / 2 - 30)
        pdf.set_x(0)
        pdf.set_font(title_fam, "B", 48)
        pdf.set_text_color(*fg["cover_title"])
        pdf.cell(0, 20, prepare_text(subject.upper(), pdf, title_fam), align="C")

        author_fam = font_for_text(author, base)
        pdf.set_y(pdf.h / 2 + 5)
        pdf.set_x(0)
        pdf.set_font(author_fam, "", 16)
        pdf.set_text_color(*fg["cover_sub"])
        pdf.cell(0, 10, f"PREPARED FOR: {prepare_text(author.upper(), pdf, author_fam)}", align="C")

        # Bottom Border and Watermark
        pdf.set_draw_color(*theme["text"])
        pdf.set_line_width(0.3)
        pdf.line(40, pdf.h - 40, pdf.w - 40, pdf.h - 40)

        pdf.set_y(pdf.h - 35)
        pdf.set_x(0)
        pdf.set_font(base, "I", 10)
        pdf.set_text_color(*fg["cover_sub"])
        pdf.cell(0, 10, "Generated by Play2PDF Studio - The Intelligent Video Compiler", align="C")

        # --- Study Grid Page ---
        pdf.add_page()
        pdf.set_draw_color(*theme["paper_border"])
        pdf.set_text_color(*fg["body"])

        matched_results = [r for r in results if r["matched"]]
        unmatched_results = [r for r in results if not r["matched"]]
        total_count = len(results)
        matched_topics_count = len(matched_results)
        pct = int((matched_topics_count / total_count * 100)) if total_count else 0
        total_videos_matched = sum(len(r["videos"]) for r in matched_results)

        pdf.set_fill_color(*theme["accent"])
        pdf.set_text_color(*fg["summary"])
        pdf.set_font(base, "B", 10)
        summary_text = (
            f" STUDY TRACK METRICS  |  Topics: {total_count}   *   "
            f"Covered: {matched_topics_count}/{total_count} ({pct}%)   *   "
            f"Videos: {total_videos_matched}   *   Playlists: {len(playlist_urls)}"
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
                    alt_bg = alt_row_bg(theme["paper_bg"])
                    pdf.set_fill_color(*alt_bg)
                    pdf.rect(10, row_y, sum(CW), row_height, "F")

                pdf.set_xy(10, row_y)
                pdf.set_font(base, "", 9)
                pdf.set_text_color(*fg["body"])
                pdf.set_draw_color(*theme["paper_border"])

                pdf.cell(CW[0], row_height, "[  ]", border="B", align="C")
                idx_str = f"{topic_idx+1}" if len(res["videos"]) == 1 else f"{topic_idx+1}.{v_idx+1}"
                pdf.cell(CW[1], row_height, idx_str, border="B", align="C")
                topic_fam = font_for_text(res["topic"], base)
                topic_label = truncate(prepare_text(res["topic"], pdf, topic_fam), 26) if v_idx == 0 else f"  > {truncate(prepare_text(res['topic'], pdf, topic_fam), 24)}"
                pdf.set_font(topic_fam, "", 9)
                pdf.cell(CW[2], row_height, topic_label, border="B")

                x_col3 = pdf.get_x()
                pdf.cell(CW[3], row_height, "", border="B")
                title_fam2 = font_for_text(vid["title"], base)
                pdf.set_xy(x_col3, row_y + 2)
                pdf.set_font(title_fam2, "B", 9)
                pdf.cell(CW[3], 5, truncate(prepare_text(vid["title"], pdf, title_fam2), 52))

                pdf.set_xy(x_col3, row_y + 8)
                pdf.set_font(base, "I", 7.5)
                pdf.set_text_color(*fg["body"])
                note_raw = f"Key Focus: {res['study_note']}" if res.get("study_note") else ""
                note_text = truncate(prepare_text(note_raw, pdf, base), 70)
                pdf.cell(CW[3], 5, note_text)

                pdf.set_xy(x_col3 + CW[3], row_y)
                pdf.set_font(base, "", 9)
                pdf.set_text_color(*fg["body"])

                pdf.cell(CW[4], row_height, vid["duration"], border="B", align="C")
                pdf.cell(CW[5], row_height, format_views(vid["views"]), border="B", align="C")

                qr_path = qr_cache.get(vid["id"], vid["url"])
                x_qr = pdf.get_x()
                pdf.cell(CW[6], row_height, "", border="B")
                pdf.image(qr_path, x=x_qr + 4, y=row_y + 2, w=16, h=16)

                pdf.set_xy(x_qr + CW[6], row_y)
                pdf.set_text_color(*fg["link"])
                pdf.set_font(base, "B", 9)
                pdf.cell(CW[7], row_height, "Watch Link", border="B", align="C", link=vid["url"])
                pdf.set_y(row_y + row_height)  # Advance Y manually — no auto page break

        # --- Unmatched Topics Appendix ---
        if unmatched_results:
            pdf.add_page()
            pdf.set_font(base, "B", 16)
            pdf.set_text_color(*fg["header"])
            pdf.cell(0, 12, "Unmatched Syllabus Topics")
            pdf.set_y(pdf.get_y() + 12)
            pdf.set_font(base, "", 10)
            pdf.set_text_color(*fg["body"])
            pdf.multi_cell(
                0, 6,
                "The following topics could not be confidently matched to any video in the "
                "provided playlist(s). You may need to supplement your study guide with "
                "external materials for these concepts:"
            )
            pdf.set_y(pdf.get_y() + 2)
            pdf.set_font(base, "", 10)
            for res in unmatched_results:
                if pdf.get_y() + 8 + 15 > pdf.h:
                    pdf.add_page()
                res_fam = font_for_text(res["topic"], base)
                pdf.set_font(res_fam, "", 10)
                pdf.cell(0, 7, f"-  {prepare_text(res['topic'], pdf, res_fam)}")
                pdf.set_y(pdf.get_y() + 7)

        # fpdf2 >= 2.8 returns a bytearray directly (the old `dest="S"` call
        # was deprecated in 2.2 and removed-direction since 2.8).
        return bytes(pdf.output())
    finally:
        qr_cache.cleanup()


def _render_portrait(
    subject: str,
    author: str,
    playlist_urls: list[str],
    results: list[dict],
    theme_name: str,
) -> bytes:
    """D2 — the v3.1 default: portrait A4, topic-grouped sections.

    Upgrades over the legacy grid:
      - real `multi_cell` wrapping — no fixed-char truncation, ever
      - D4 refreshed editorial cover with a metrics strip
      - D5 table of contents page + PDF outline bookmarks (start_section)
      - D6 document metadata
      - per-topic confidence chips + AI study notes
    """
    theme = THEMES.get(theme_name, THEMES["nordic_frost"])
    qr_cache = QRCache()
    try:
        pdf = StudyGuidePDF(subject, theme, orientation="P")
        base = pdf.base_family
        fg = pdf.fg
        _set_pdf_metadata(pdf, subject, author)

        margin = 18.0
        content_w = pdf.w - 2 * margin  # 174mm
        matched = [r for r in results if r["matched"]]
        unmatched = [r for r in results if not r["matched"]]
        total_videos = sum(len(r["videos"]) for r in matched)

        # ---------- Cover (D4) ----------
        pdf.add_page()
        pdf.set_fill_color(*theme["bg"])
        pdf.rect(0, 0, pdf.w, pdf.h, "F")

        # Concentric accent geometry, anchored top-right
        pdf.set_draw_color(*theme["accent"])
        pdf.set_line_width(0.5)
        for r in (28, 46, 64, 82):
            pdf.ellipse(pdf.w - margin - 2 * r, 8 + 46 - r, 2 * r, 2 * r, "D")
        pdf.set_line_width(1.6)
        pdf.line(pdf.w - 70, 0, pdf.w, 70)
        pdf.line(pdf.w - 88, 0, pdf.w, 88)

        # Title (auto-shrink so long subjects never overflow)
        title_fam = font_for_text(subject, base)
        title = prepare_text(subject.upper(), pdf, title_fam)
        size = 44 if len(title) <= 22 else (34 if len(title) <= 40 else 26)
        pdf.set_y(96)
        pdf.set_font(title_fam, "B", size)
        pdf.set_text_color(*fg["cover_title"])
        pdf.multi_cell(content_w, size * 0.55, title, align="C")

        author_fam = font_for_text(author, base)
        pdf.set_y(pdf.get_y() + 6)
        pdf.set_font(author_fam, "", 15)
        pdf.set_text_color(*fg["cover_sub"])
        pdf.multi_cell(content_w, 9, f"PREPARED FOR: {prepare_text(author.upper(), pdf, author_fam)}", align="C")

        # Metrics strip (four blocks on an accent band)
        strip_y = pdf.h - 74
        pdf.set_fill_color(*theme["accent"])
        pdf.rect(margin, strip_y, content_w, 22, "F")
        pdf.set_font(base, "B", 16)
        pdf.set_text_color(*fg["summary"])
        cells = [
            (str(len(results)), "topics"),
            (f"{len(matched)}/{len(results)}", "covered"),
            (str(total_videos), "videos"),
            (str(len(playlist_urls)), "playlists"),
        ]
        cw = content_w / 4
        for i, (num, label) in enumerate(cells):
            x = margin + i * cw
            pdf.set_xy(x, strip_y + 3)
            pdf.cell(cw, 9, num, align="C")
            pdf.set_font(base, "", 8.5)
            pdf.set_xy(x, strip_y + 12)
            pdf.cell(cw, 6, label.upper(), align="C")
            pdf.set_font(base, "B", 16)
            pdf.set_text_color(*fg["summary"])

        # Footer rule + watermark
        pdf.set_draw_color(*theme["text"])
        pdf.set_line_width(0.3)
        pdf.line(40, pdf.h - 40, pdf.w - 40, pdf.h - 40)
        pdf.set_y(pdf.h - 34)
        pdf.set_font(base, "I", 9.5)
        pdf.set_text_color(*fg["cover_sub"])
        pdf.cell(0, 8, "Generated by Play2PDF Studio - The Intelligent Video Compiler", align="C")

        # ---------- Contents (D5) ----------
        pdf.add_page()
        pdf.set_margins(margin, margin, margin)
        pdf.set_xy(margin, margin + 6)
        pdf.set_font(base, "B", 22)
        pdf.set_text_color(*fg["header"])
        pdf.cell(0, 12, "Contents")
        pdf.set_y(pdf.get_y() + 16)

        def _render_toc(doc: FPDF, sections: list) -> None:
            doc.set_font(base, "", 10.5)
            doc.set_text_color(*fg["body"])
            for s in sections:
                label = prepare_text(str(getattr(s, "name", s)), doc, base)
                doc.cell(0, 7.6, label, align="L")
                doc.set_y(doc.get_y() + 7.6)

        try:
            pdf.insert_toc_placeholder(_render_toc, pages=1)
        except Exception:  # noqa: BLE001 — TOC is an enhancement, never fatal
            pass

        # ---------- Topic sections ----------
        for idx, res in enumerate(matched):
            pdf.start_section(prepare_text(res["topic"], pdf, base))
            # Keep headers with at least one video row: need ~46mm
            if pdf.get_y() + 52 > pdf.h - 16:
                pdf.add_page()
            top_y = pdf.get_y() + 4

            # Section header band
            pdf.set_fill_color(*theme["accent"])
            pdf.rect(margin, top_y, content_w, 11, "F")
            pdf.set_xy(margin + 4, top_y + 1.5)
            pdf.set_font(base, "B", 12.5)
            pdf.set_text_color(*fg["summary"])
            pdf.cell(content_w - 40, 8, f"{idx + 1}.  {prepare_text(res['topic'], pdf, base)}")
            # Confidence chip
            conf = (res.get("confidence") or "medium").upper()
            pdf.set_xy(margin + content_w - 38, top_y + 2)
            pdf.set_font(base, "B", 8)
            pdf.cell(34, 7, conf, align="R")
            pdf.set_y(top_y + 14)

            # AI study note
            if res.get("study_note"):
                pdf.set_font(base, "I", 9.5)
                pdf.set_text_color(*fg["body"])
                pdf.multi_cell(content_w, 5.6, prepare_text(res["study_note"], pdf, base))
                pdf.set_y(pdf.get_y() + 3)

            # Video rows
            for v_i, vid in enumerate(res["videos"]):
                row_h = 24
                if pdf.get_y() + row_h > pdf.h - 16:
                    pdf.add_page()
                y = pdf.get_y()

                # zebra background for odd rows
                if v_i % 2 == 0:
                    pdf.set_fill_color(*alt_row_bg(theme["paper_bg"]))
                    pdf.rect(margin, y, content_w, row_h, "F")

                # checkbox
                pdf.set_xy(margin + 3, y + row_h / 2 - 3)
                pdf.set_font(base, "B", 11)
                pdf.set_text_color(*fg["body"])
                pdf.cell(7, 6, "[  ]")

                # title + meta (wrapped, never truncated)
                vid_fam = font_for_text(vid["title"], base)
                pdf.set_xy(margin + 13, y + 3)
                pdf.set_font(vid_fam, "B", 10)
                pdf.multi_cell(content_w - 13 - 30, 5.4, prepare_text(vid["title"], pdf, vid_fam))
                pdf.set_xy(margin + 13, y + row_h - 9)
                pdf.set_font(base, "", 8.5)
                pdf.set_text_color(*fg["body"])
                pdf.cell(content_w - 13 - 30, 5,
                         f"{vid['duration']}   |   {format_views(vid['views'])} views")

                # QR (right) + watch link
                qr_path = qr_cache.get(vid["id"], vid["url"])
                pdf.image(qr_path, x=margin + content_w - 22, y=y + 3, w=18, h=18)
                pdf.set_xy(margin + content_w - 24, y + row_h - 9)
                pdf.set_font(base, "B", 8.5)
                pdf.set_text_color(*fg["link"])
                pdf.cell(24, 5, "Watch", align="R", link=vid["url"])

                pdf.set_y(y + row_h + 2)

            pdf.set_y(pdf.get_y() + 4)

        # ---------- Unmatched appendix ----------
        if unmatched:
            pdf.add_page()
            pdf.set_font(base, "B", 16)
            pdf.set_text_color(*fg["header"])
            pdf.cell(0, 12, "Unmatched Syllabus Topics")
            pdf.set_y(pdf.get_y() + 14)
            pdf.set_font(base, "", 10)
            pdf.set_text_color(*fg["body"])
            pdf.multi_cell(content_w, 6,
                           "The following topics could not be confidently matched to any video "
                           "in the provided playlist(s). You may need to supplement your study "
                           "guide with external materials for these concepts:")
            pdf.set_y(pdf.get_y() + 3)
            for res in unmatched:
                if pdf.get_y() + 8 > pdf.h - 16:
                    pdf.add_page()
                res_fam = font_for_text(res["topic"], base)
                pdf.set_font(res_fam, "", 10)
                pdf.cell(0, 7, f"-  {prepare_text(res['topic'], pdf, res_fam)}")
                pdf.set_y(pdf.get_y() + 7)

        return bytes(pdf.output())
    finally:
        qr_cache.cleanup()


def alt_row_bg(paper_bg: tuple[int, int, int]) -> tuple[int, int, int]:
    """The zebra-stripe row background derived from paper_bg (grid loop)."""
    def alt(c: int) -> int:
        return max(0, c - 8) if c > 128 else min(255, c + 15)
    return tuple(alt(c) for c in paper_bg)


def theme_text_pairs(theme: dict) -> list[tuple[str, tuple[int, int, int], tuple[int, int, int]]]:
    """Every fg/bg colour pair the renderer paints.

    Mirrors StudyGuidePDF.fg exactly (same readable_on derivations) — the
    WCAG audit in test_themes.py consumes this, so the audit can never
    drift from what the renderer actually paints. Any new set_text_color
    combo must appear in both places.
    """
    paper = theme["paper_bg"]
    body = readable_on(theme["paper_text"], paper)
    header = readable_on(theme["accent"], paper)
    return [
        ("cover_title",    readable_on(theme["text"], theme["bg"]), theme["bg"]),
        ("cover_subtitle", readable_on(theme["subtext"], theme["bg"]), theme["bg"]),
        ("summary_bar",    readable_on(theme["text"], theme["accent"]), theme["accent"]),
        ("grid_header",    header,                                paper),
        ("grid_body",      body,                                  paper),
        ("grid_body_alt",  body,                                  alt_row_bg(paper)),
        ("study_note",     body,                                  paper),
        ("watch_link",     header,                                paper),
        ("footer",         body,                                  paper),
    ]


# --- v3 endpoints (under /api/v1/) -----------------------------------------
# These are aliases of the v1 endpoints, exposed under a versioned prefix
# so future breaking changes can ship under /api/v2/ without breaking v1
# callers. The Android Compose app uses the v1 endpoints for now (they're
# the only ones currently deployed), but the v3 prefix is available for
# opt-in.
app_v1 = FastAPI(title="Play2PDF API v1", version="3.1")
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


@app_v1.get("/themes")
def v1_themes():
    return themes()


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
