# Play2PDF 🚀

**The native Android rewrite of [Play2Pdf](https://github.com/AdnanFoisal/Play2Pdf), built with Kotlin + Jetpack Compose.**

The original app was written in Python + Flet — great for prototyping but
hit a hard ceiling on performance, asset pipeline reliability, and native
API access. This repo is the production-grade Kotlin + Jetpack Compose
rewrite that fixes all of that.

**Status: implemented and deployed.** All screens (splash, onboarding,
compile, history, settings, compiling) are live, wired to the FastAPI
backend, and shipped through CI (`assembleDebug` on every push).

---

## 🎨 Brand

The canonical brand is the **green #1DB954 family** — see
[`docs/BRAND.md`](docs/BRAND.md). (The violet palette in the HTML
mockups and older plan docs is superseded.)

Typography: **Space Grotesk** (display) + **DM Sans** (body), bundled as
variable TTFs — no downloadable fonts, no Play Services dependency.

---

## 📐 The plan

- [`docs/BRAND.md`](docs/BRAND.md) — canonical colors, fonts, surfaces
- [`docs/newplans/FINAL_IMPLEMENTATION_PLAN.md`](docs/newplans/FINAL_IMPLEMENTATION_PLAN.md) — the implementation plan this rewrite followed
- [`docs/old plans/PREMIUM_DESIGN_PLAN.md`](docs/old%20plans/PREMIUM_DESIGN_PLAN.md) — the original roles-divided master plan

---

## 🏗️ Architecture

| Layer | Choice |
|-------|--------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose (Material 3) |
| DI | Hilt |
| Async | Coroutines + Flow |
| Network | Retrofit + OkHttp + Moshi |
| Local DB | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

Backend (FastAPI + Gemini + YouTube Data API v3) lives in [`backend/`](backend/README.md)
and is deployed at https://adnanfoisal-play2pdf.hf.space — see its README
for the full API surface, including `GET /themes` (server-authoritative
theme palettes the app renders live previews from) and the v3.1 portrait
PDF layout with TOC, bookmarks, embedded Unicode fonts and WCAG-AA-safe
colors across all 21 themes.

---

## 🚀 Quick start

```bash
git clone https://github.com/AdnanFoisal/Play2Pdf-Compose.git
cd Play2Pdf-Compose
# open in Android Studio, or:
./gradlew assembleDebug
```

Plug in a device (or start an emulator) and hit Run. The app hits the
public backend at `https://adnanfoisal-play2pdf.hf.space` by default —
configurable in Settings (YouTube Data v3 + Gemini API keys are entered
per-device in Settings and sent per request; nothing is stored
server-side).

---

## 📄 License

© 2026 Adnan Foisal. All Rights Reserved.
