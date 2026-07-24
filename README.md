# Play2PDF — Compose Rewrite 🚀

**The native Android rewrite of [Play2Pdf](https://github.com/AdnanFoisal/Play2Pdf), built with Kotlin + Jetpack Compose.**

The original app was written in Python + Flet — great for prototyping but
hit a hard ceiling on performance, asset pipeline reliability, and native
API access. This repo is the production-grade Kotlin + Jetpack Compose
rewrite that fixes all of that.

---

## 📐 The plan

**Read the full premium design plan: [`docs/PREMIUM_DESIGN_PLAN.md`](docs/PREMIUM_DESIGN_PLAN.md)**

It's a 1,500-line brutally honest audit of everything that made the old
app look amateur, plus a concrete implementation plan for the Compose
rewrite covering:

- Brand identity (color, logo, typography, spacing, radius).
- Asset inventory (adaptive icon, custom `ImageVector` set, Rive splash,
  PDF theme previews, sound effects).
- Visual system (real `Modifier.blur` glassmorphism, 3-layer neon glow,
  micro-typography, motion principles table).
- Design system in Compose (full `Color.kt`, `Theme.kt`, `Type.kt`,
  `Spacing.kt`, `Shape.kt`, `Motion.kt` source).
- Component polish (PrimaryButton, PremiumCard, GlassCard, AnimatedChip,
  custom BottomNavBar, ShimmerSkeleton).
- Screen-by-screen redesign (splash, 3-screen onboarding carousel,
  compile, history, settings, compiling).
- Micro-interactions (press depth, chip springs, page transitions,
  scroll-linked parallax, success confetti).
- Sound & haptics (`SoundManager` + `HapticsManager` with 11 patterns).
- 9-phase implementation order (18-25 days focused, 5-7 weeks part-time).
- Asset generation toolkit (Figma, Rive, LottieFiles, Phosphor, etc.).
- 30-item pre-ship quality checklist.

---

## 🏗️ Status

🚧 **Pre-implementation** — design plan complete, code coming soon.

The FastAPI backend at https://adnanfoisal-play2pdf.hf.space (source in
the [original repo](https://github.com/AdnanFoisal/Play2Pdf/tree/main/backend))
will be reused as-is — it already uses **Gemini 3.6 Flash** (latest GA)
and the YouTube Data API v3.

---

## 🛠️ Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose 1.7+ |
| DI | Hilt |
| Async | Coroutines + Flow |
| Network | Retrofit + OkHttp + Moshi |
| Local DB | Room |
| Preferences | DataStore |
| Image loading | Coil 3 |
| Animations | Compose Animation + Lottie + Rive |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## 🚀 Quick start (once implementation begins)

```bash
git clone https://github.com/AdnanFoisal/Play2Pdf-Compose.git
cd Play2Pdf-Compose
open -a "Android Studio" .   # or: studio .
```

Then let Gradle sync, plug in a device (or start an emulator), and hit
Run. The app will hit the public backend at
`https://adnanfoisal-play2pdf.hf.space` by default — configurable in
Settings once the app is built.

---

## 📄 License

© 2026 Adnan Foisal. All Rights Reserved.
