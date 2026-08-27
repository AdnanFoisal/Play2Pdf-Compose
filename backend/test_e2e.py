"""
End-to-end endpoint test for the generate_guide HTTP surface.

Monkeys-patches fetch_videos + GenerativeModel so the FULL request path
runs (validation, topic parsing, AI-response parsing, render, response
headers) with zero network. Run after any change to the endpoint wiring.

Usage: python test_e2e.py
"""
import json
import sys

import server
from fastapi.testclient import TestClient

FAKE_VIDEOS = [
    {"id": "vid1", "title": "Arrays Deep Dive \u2014 Lecture 1",
     "description": "Memory layout", "duration": "45:30", "duration_seconds": 2730,
     "views": 150000, "url": "https://youtube.com/watch?v=vid1"},
    {"id": "vid2", "title": "\u09a1\u09c7\u099f\u09be \u09b8\u09cd\u099f\u09cd\u09b0\u09be\u0995\u099a\u09be\u09b0",
     "description": "Bengali DS intro", "duration": "52:00", "duration_seconds": 3120,
     "views": 7777, "url": "https://youtube.com/watch?v=vid2"},
]

AI_MATCHES = [
    {"topic": "Arrays & Pointers", "video_ids": ["vid1"], "confidence": "high",
     "study_note": "Covers memory layout."},
    {"topic": "Big-O, Big-Theta Notation", "video_ids": ["vid1"], "confidence": "medium",
     "study_note": "Bounds."},
    {"topic": "\u0413\u0440\u0430\u0444\u044b \u0438 \u0434\u0435\u0440\u0435\u0432\u044c\u044f", "video_ids": ["vid2"], "confidence": "low",
     "study_note": "Graphs."},
]


class _FakeResp:
    text = json.dumps(AI_MATCHES)


class FakeGenerativeModel:
    def __init__(self, *args, **kwargs):
        pass

    def generate_content(self, prompt):
        return _FakeResp()


def main():
    server.fetch_videos = lambda api_key, urls: FAKE_VIDEOS
    server.GenerativeModel = FakeGenerativeModel

    client = TestClient(server.app)
    failures = []

    # --- liveness ---
    for path in ("/ping", "/health"):
        r = client.get(path)
        print(f"GET {path:15} -> {r.status_code} {r.json()}")
        if r.status_code != 200:
            failures.append(f"{path} returned {r.status_code}")

    # --- generate: newline-delimited topics incl. a comma-bearing topic ---
    req = {
        "youtube_key": "fake", "gemini_key": "fake",
        "subject": "Data Structures", "author": "Test Student",
        "playlist_urls": ["https://youtube.com/playlist?list=PLx"],
        "topics": "Arrays & Pointers\nBig-O, Big-Theta Notation\n\u0413\u0440\u0430\u0444\u044b \u0438 \u0434\u0435\u0440\u0435\u0432\u044c\u044f\nUnmatched Topic",
        "theme": "nordic_frost",
    }
    r = client.post("/generate_guide", json=req)
    ctype = r.headers.get("content-type", "")
    print(f"POST /generate_guide -> {r.status_code} ({ctype}, {len(r.content)} bytes)")
    if r.status_code != 200 or "application/pdf" not in ctype or not r.content.startswith(b"%PDF"):
        failures.append(f"generate_guide bad response: {r.status_code} {ctype[:40]}")

    # --- v1 alias ---
    r2 = client.post("/api/v1/generate_guide", json=req)
    print(f"POST /api/v1/generate_guide -> {r2.status_code} ({len(r2.content)} bytes)")
    if r2.status_code != 200 or not r2.content.startswith(b"%PDF"):
        failures.append(f"/api/v1/generate_guide bad: {r2.status_code}")

    # --- legacy comma-separated topics still parse ---
    req_legacy = dict(req, topics="Arrays & Pointers,Big-O")
    rl = client.post("/generate_guide", json=req_legacy)
    print(f"POST legacy comma topics -> {rl.status_code}")
    if rl.status_code != 200:
        failures.append(f"legacy comma topics failed: {rl.status_code} {rl.text[:120]}")

    # --- empty playlists rejected ---
    rb = client.post("/generate_guide", json=dict(req, playlist_urls=[]))
    print(f"POST empty playlists -> {rb.status_code} (expect 422)")
    if rb.status_code != 422:
        failures.append(f"empty playlists should 422, got {rb.status_code}")

    if failures:
        print("\nFAILURES:")
        for f in failures:
            print(" -", f)
        sys.exit(1)
    print("\nE2E: ALL CHECKS PASS")


if __name__ == "__main__":
    main()
