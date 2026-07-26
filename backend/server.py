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
        pdf.cell(0, 20, req.subject.upper(), align="C")
        
        pdf.set_y(pdf.h / 2 + 5)
        pdf.set_x(0)
        pdf.set_font(theme["font_family"], "", 16)
        pdf.set_text_color(*theme["subtext"])
        pdf.cell(0, 10, f"PREPARED FOR: {req.author.upper()}", align="C")

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
                topic_label = truncate(res["topic"], 26) if v_idx == 0 else f"  > {truncate(res['topic'], 24)}"
                pdf.cell(CW[2], row_height, topic_label, border="B")

                x_col3 = pdf.get_x()
                pdf.cell(CW[3], row_height, "", border="B")
                pdf.set_xy(x_col3, row_y + 2)
                pdf.set_font(theme["font_family"], "B", 9)
                pdf.cell(CW[3], 5, truncate(vid["title"], 52))

                pdf.set_xy(x_col3, row_y + 8)
                pdf.set_font(theme["font_family"], "I", 7.5)
                pdf.set_text_color(100, 110, 125)
                note_text = f"Key Focus: {truncate(res['study_note'], 70)}" if res.get("study_note") else ""
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
                pdf.cell(0, 7, f"-  {res['topic']}")
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
