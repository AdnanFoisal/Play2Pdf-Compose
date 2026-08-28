"""
Golden-file harness for Play2PDF PDF generation.

Renders EVERY theme from fixed mock data (no network, no API keys — the
Gemini/YouTube paths are not exercised) and asserts the invariants that
must never regress:

  HARD failures (exit 1) — correctness of the pipeline itself:
    1. Every theme renders without exception.
    2. Every render has >= 2 pages (cover + grid).
    3. No blank pages: every page carries extractable text (the subject
       footer is drawn on every page, so a textless page == blank page).
    4. Text is extractable (searchable/copyable PDF, not outlines only).

  Contrast audit (exit 2) — WCAG AA, resolved by D3:
    Every fg/bg pair from theme_text_pairs() must reach 4.5:1.
    Exit code 2 (distinct from 1) while D3 is pending.

  Unicode audit (exit 3) — resolved by D1:
    Bengali/Cyrillic/em-dash sample text must survive to extraction
    (today's Latin-1-only fonts turn them into '?').

Usage:
    python test_themes.py             # run all checks, write renders
    python test_themes.py --keep      # keep testoutput/ directory

Renders land in backend/testoutput/<theme>.pdf for eyeballing.
"""
import io
import os
import sys
import tempfile

from pypdf import PdfReader

from server import THEMES, render_guide_pdf, theme_text_pairs

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "testoutput")

SUBJECT = "Data Structures & Algorithms"
AUTHOR = "Test Student"
PLAYLISTS = [
    "https://youtube.com/playlist?list=PLtest01",
    "https://youtube.com/playlist?list=PLtest02",
]

# Fixed mock results: matched topics (one with 2 videos), an unmatched
# topic (exercises the appendix), and Unicode titles (exercises D1).
UNICODE_TITLE = "\u09a1\u09c7\u099f\u09be \u09b8\u09cd\u099f\u09cd\u09b0\u09be\u0995\u099a\u09be\u09b0"  # Bengali
UNICODE_TOPIC = "\u0413\u0440\u0430\u0444\u044b \u0438 \u0434\u0435\u0440\u0435\u0432\u044c\u044f"  # Russian

MOCK_RESULTS = [
    {
        "topic": "Arrays & Pointers",
        "matched": True,
        "videos": [
            {"id": "vid1", "title": "Arrays Deep Dive \u2014 Lecture 1", "duration": "45:30",
             "views": 150000, "url": "https://youtube.com/watch?v=vid1"},
            {"id": "vid2", "title": "Pointer Arithmetic Tutorial", "duration": "12:05",
             "views": 42000, "url": "https://youtube.com/watch?v=vid2"},
        ],
        "study_note": "Covers memory layout and index arithmetic.",
        "confidence": "high",
    },
    {
        "topic": "Big-O, Big-Theta Notation",  # comma inside a topic (C6)
        "matched": True,
        "videos": [
            {"id": "vid3", "title": "Asymptotic Analysis", "duration": "38:11",
             "views": 98000, "url": "https://youtube.com/watch?v=vid3"},
        ],
        "study_note": "Defines upper and tight bounds.",
        "confidence": "medium",
    },
    {
        "topic": UNICODE_TOPIC,
        "matched": True,
        "videos": [
            {"id": "vid4", "title": UNICODE_TITLE, "duration": "52:00",
             "views": 7777, "url": "https://youtube.com/watch?v=vid4"},
        ],
        "study_note": "Graph representations.",
        "confidence": "low",
    },
    {
        "topic": "Quantum Sorting (does not exist in playlist)",
        "matched": False,
        "videos": [],
        "study_note": "No direct match found in playlist.",
        "confidence": "none",
    },
]

UNICODE_SAMPLES = [UNICODE_TITLE, UNICODE_TOPIC, "\u2014"]  # title, topic, em-dash


def wcag_ratio(fg, bg) -> float:
    """WCAG 2.1 relative-luminance contrast ratio."""
    def lum(c):
        def f(v):
            v = v / 255.0
            return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
        return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2])
    la, lb = lum(fg), lum(bg)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def check_render(theme_name, keep_files, layout="portrait"):
    """Render one theme in one layout; return list of HARD failure strings."""
    failures = []
    try:
        pdf_bytes = render_guide_pdf(
            subject=SUBJECT, author=AUTHOR, playlist_urls=PLAYLISTS,
            results=MOCK_RESULTS, theme_name=theme_name, layout=layout,
        )
    except Exception as e:  # noqa: BLE001 — any exception is a failure
        return [f"[{layout}] render raised {type(e).__name__}: {e}"]

    reader = PdfReader(io.BytesIO(pdf_bytes))
    if len(reader.pages) < 3:
        failures.append(f"[{layout}] only {len(reader.pages)} page(s), expected >= 3 (cover+contents+grid)")

    blank = [
        i for i, page in enumerate(reader.pages)
        if not (page.extract_text() or "").strip()
    ]
    if blank:
        failures.append(f"[{layout}] blank page(s) at index {blank}")

    all_text = "\n".join((p.extract_text() or "") for p in reader.pages)
    if SUBJECT.split(" & ")[0] not in all_text:
        failures.append(f"[{layout}] subject text not extractable")
    if layout == "portrait" and "Contents" not in all_text:
        failures.append("[portrait] TOC page missing")

    if keep_files:
        suffix = "" if layout == "portrait" else f"_{layout}"
        with open(os.path.join(OUT_DIR, f"{theme_name}{suffix}.pdf"), "wb") as f:
            f.write(pdf_bytes)

    return failures


def audit_contrast():
    """Per-theme pair audit; returns {(theme, pair): ratio} below 4.5."""
    failing = {}
    for name, theme in THEMES.items():
        for label, fg, bg in theme_text_pairs(theme):
            ratio = wcag_ratio(fg, bg)
            if ratio < 4.5:
                failing[(name, label)] = ratio
    return failing


def audit_unicode():
    """Render once and check Unicode survives to extraction (needs D1 fonts)."""
    pdf_bytes = render_guide_pdf(
        subject=SUBJECT, author=AUTHOR, playlist_urls=PLAYLISTS,
        results=MOCK_RESULTS, theme_name="nordic_frost",
    )
    text = "\n".join(
        (p.extract_text() or "") for p in PdfReader(io.BytesIO(pdf_bytes)).pages
    )
    missing = [s for s in UNICODE_SAMPLES if s not in text]
    return missing


def main():
    keep = "--keep" in sys.argv
    if keep:
        os.makedirs(OUT_DIR, exist_ok=True)

    print(f"Rendering {len(THEMES)} themes x 2 layouts...")
    hard_failures = {}
    for layout in ("portrait", "grid_landscape"):
        for name in THEMES:
            fails = check_render(name, keep, layout)
            status = "ok" if not fails else "FAIL"
            print(f"  {status:4} [{layout:15}] {name}")
            if fails:
                hard_failures[f"{name}/{layout}"] = fails

    print("\nContrast audit (WCAG AA 4.5:1)...")
    contrast = audit_contrast()
    by_theme = {}
    for (theme, pair), ratio in contrast.items():
        by_theme.setdefault(theme, []).append((pair, ratio))
    print(f"  {len(THEMES) - len(by_theme)}/{len(THEMES)} themes fully compliant")
    for theme, pairs in by_theme.items():
        detail = ", ".join(f"{p}={r:.2f}" for p, r in pairs)
        print(f"  FAIL {theme}: {detail}")

    print("\nUnicode audit (D1)...")
    missing = audit_unicode()
    if missing:
        print(f"  pending D1 — not preserved: {[ascii(m) for m in missing]}")
    else:
        print("  ok — all samples preserved")

    exit_code = 0
    if hard_failures:
        print(f"\nHARD FAILURES in {len(hard_failures)} theme(s):")
        for theme, fails in hard_failures.items():
            for f in fails:
                print(f"  {theme}: {f}")
        exit_code = 1
    elif contrast:
        print(f"\nContrast failures on {len(by_theme)} theme(s) — pending D3 (exit 2).")
        exit_code = 2
    elif missing:
        print("\nUnicode pending D1 (exit 3).")
        exit_code = 3
    else:
        print("\nALL CHECKS PASS.")
    sys.exit(exit_code)


if __name__ == "__main__":
    main()
