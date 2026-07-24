# Play2PDF — Premium Design Plan (Kotlin + Jetpack Compose Edition)

> **Honesty mode: ON.** This document is not a back-pat. It is a frank
> list of everything currently making Play2PDF look like a 48-hour
> hackathon entry instead of a $9.99/mo product, and exactly what to
> do about each one — implemented against the **Kotlin + Jetpack
> Compose** tech stack that we are migrating to from the old
> Python/Flet prototype.

**Document version:** 2.0 (Kotlin/Compose rewrite)
**Last updated:** 2026-07-24
**Tech stack:** Kotlin 2.0+, Jetpack Compose 1.7+, Material 3,
Coroutines, Hilt, Room, Retrofit/Ktor, Coil, Lottie/Rive
**Backend stack:** Unchanged — FastAPI + Gemini 3.6 Flash + YouTube
Data API v3 + fpdf2 + qrcode
**Audience:** You, the designer you hire, and the AI that helps you
implement it.

---

## Table of Contents

1. [Why Rewrite in Kotlin + Jetpack Compose?](#1-why-rewrite-in-kotlin--jetpack-compose)
2. [The Brutal Truth (Where We Are Today)](#2-the-brutal-truth-where-we-are-today)
3. [What "Premium" Actually Means](#3-what-premium-actually-means)
4. [Project Architecture](#4-project-architecture)
5. [Brand Identity — The Foundation](#5-brand-identity--the-foundation)
6. [Asset Inventory — What To Produce](#6-asset-inventory--what-to-produce)
7. [Visual System — Color, Type, Space, Motion](#7-visual-system--color-type-space-motion)
8. [Design System in Compose](#8-design-system-in-compose)
9. [Component-Level Premium Polish](#9-component-level-premium-polish)
10. [Screen-by-Screen Premium Redesign](#10-screen-by-screen-premium-redesign)
11. [Micro-Interactions & Motion Design](#11-micro-interactions--motion-design)
12. [Sound & Haptics](#12-sound--haptics)
13. [Implementation Order & Priority](#13-implementation-order--priority)
14. [Asset Generation Toolkit](#14-asset-generation-toolkit)
15. [Quality Checklist](#15-quality-checklist)

---

## 1. Why Rewrite in Kotlin + Jetpack Compose?

The original app was written in Python + Flet. Flet was a great
prototyping choice — it got us from zero to a working app in a
weekend. But Flet's design ceiling is low:

- **Performance ceiling.** Flet renders Flutter Web on desktop and
  Flutter Mobile on Android — but every UI update goes through a
  Python ↔ Dart message bridge. At 60fps scrolling this becomes
  visible jank.
- **Asset pipeline hell.** Flet's asset pipeline is brittle (the
  JPEGs-as-PNGs bug we hit, the broken Android adaptive icons, the
  silent Web CanvasKit paint-drop on multi-shadow rgba containers).
  Each Flet version changes the rules.
- **Native API access.** Things like proper HapticFeedback patterns,
  SystemUI controller, Android 13 themed icons, predictive back
  gestures, share sheets, deep links — all require native code which
  Flet doesn't expose.
- **Custom drawing.** Custom splash animations, real BackdropFilter
  blur, neon glow with multiple stacked RenderEffect layers — Flet
  does these badly or not at all. Compose does them natively.

Jetpack Compose is the right answer because:
- **Native performance.** Compose compiles to Kotlin bytecode and
  runs on the JVM directly. No bridge.
- **Modern declarative UI.** Same mental model as Flutter (which
  Flet mimics) but with Kotlin syntax — easier to migrate.
- **Full Android API access.** Everything Android supports is
  available natively — predictive back, themed icons, edge-to-edge,
  Material You dynamic colors, system bar styling, predictive
  animation, etc.
- **Best-in-class tooling.** Android Studio + Compose previews +
  Layout Inspector + Database Inspector + Network Inspector — all
  free, all first-party.
- **Long-term supported.** Google has committed to Compose as the
  primary Android UI framework.

This rewrite also fixes the original "looks like slop" problem at the
source: we ship a real brand, real custom icons, real motion design,
and real polish — none of which was possible inside Flet's constraints.

---

## 2. The Brutal Truth (Where We Are Today)

This audit applies to the old Flet app. The Compose rewrite is the
opportunity to NOT inherit any of these.

### 2.1 The "assets" folder was actively sabotaging the app

- Three of the four "icon" files were **the same file** byte-for-byte
  (all 462,063 bytes).
- All asset files were **JPEGs renamed to .png** — no alpha channel,
  which is why the Android adaptive icon pipeline silently refused
  them.
- The "hero banner" was a 1024×1024 square being asked to fill a wide
  slot — squashed on every device.
- The "empty state illustration" was a stock-photo JPEG.

**Status:** Already deleted in commit `5102647` of the old repo.
The Compose rewrite starts asset-clean.

### 2.2 The "logo" is not a logo

The current splash icon is a Material `PLAY_ARROW` icon inside a
purple rounded square. That is not a logo. Anyone who sees it knows
no designer touched this.

A real logo communicates the brand in silhouette — at 16×16, in
black-and-white, in a favicon. See §5.4 for the spec.

### 2.3 Every icon in the app is a stock Material Icon

Material Icons are visually fine but they scream **"I am free"** to
anyone with design taste. Every premium app (Linear, Things 3, Notion,
Bear, Craft) ships a custom icon set with its own weight and stroke
width.

The Compose rewrite will use a custom `ImageVector` icon set loaded
via custom Kotlin definitions.

### 2.4 The "glassmorphism" is fake

Real glassmorphism uses `BackdropFilter` to blur whatever is behind
the surface. The old Flet code painted a translucent `LinearGradient`
on top of the background — that's a tinted overlay, not blur.

Compose has a real `Modifier.blur()` (RenderEffect-backed on Android
12+) and `BackdropFilter`-equivalent via `Modifier.graphicsLayer` +
`RenderEffect.createBlurEffect`. See §7.1.

### 2.5 The "neon glow" looks like a CSS box-shadow

Real neon glow needs 3 stacked shadows (inner core, mid halo, outer
falloff). The old code painted a single 40px-blur shadow at 40% alpha.

Compose uses a custom `Modifier.drawBehind` with a multi-layer
`Brush.radialGradient` (see §7.2 for the helper).

### 2.6 Typography hierarchy is "use Poppins everywhere"

Not a type system. Premium typography has 6–8 distinct roles each
with carefully tuned size, weight, letter-spacing, line-height, and
color contrast. See §5.5 for the table.

### 2.7 Spacing is random

Old tokens: `XXS=2, XS=4, SM=6, MD=10, LG=14, XL=18, XXL=24`. Not
the 8-pt grid every premium system uses. New scale: `4, 8, 12, 16,
24, 32, 48, 64`. See §5.6.

### 2.8 No empty states with personality

Premium apps use empty states to **delight** — custom illustration,
warm copy line, suggested next action. This is 30% of perceived
quality on first launch.

### 2.9 Loading states are a `ProgressRing`

A spinning Material `ProgressRing` is the "loading" of last resort.
Premium apps use **skeleton screens** or branded loading animations.

### 2.10 The bottom nav has no personality

Stock Material 3 nav bar. Premium apps ship custom navs with custom
indicator shapes, custom active/inactive transitions, per-tab haptic
patterns.

### 2.11 No micro-interactions

Old app had one spring on one button. Premium apps have dozens of
micro-interactions. Compose's animation APIs (`animateFloatAsState`,
`AnimatedContent`, `AnimatedVisibility`, `rememberInfiniteTransition`,
`updateTransition`) make this trivial — there is no excuse.

### 2.12 The PDF themes are 13 swatches with no preview polish

13 themes shown as a 2-column grid of cards each with three tiny
16×16 color squares. Should show miniatures of the actual PDF cover
page (see §6.8).

---

## 3. What "Premium" Actually Means

Before the plan, define the bar. "Premium" is the **absence of
anything that breaks the spell**:

1. **Every pixel is intentional.** Nothing is default. Every radius,
   every shadow, every line height, every animation curve was chosen,
   not inherited.
2. **The brand is recognizable in silhouette.** Hide the name, hide
   the colors — could a user still tell which app this is?
3. **Motion has weight.** Things don't just appear. They settle, they
   bounce, they ease. Curves match real-world physics.
4. **Empty states are designed, not absent.** First-run UX is the most
   expensive real estate in the app.
5. **Errors are graceful.** 404, offline, quota-exceeded — every one
   designed, friendly, on-brand.
6. **The little things are right.** Status bar tint matches the app
   background. Splash → first-screen transition is seamless. Haptic
   feedback on every primary action.
7. **Performance is part of premium.** 60fps scroll, sub-200ms tap
   response, splash that doesn't outstay its welcome.

---

## 4. Project Architecture

```
Play2Pdf-Compose/
├── app/                                    ← Android app module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/adnanfoisal/play2pdf/
│   │   │   │   ├── Play2PdfApp.kt          ← Application class, Hilt
│   │   │   │   ├── MainActivity.kt         ← Single-activity entry
│   │   │   │   ├── core/
│   │   │   │   │   ├── designsystem/       ← Theme, tokens, components
│   │   │   │   │   │   ├── theme/
│   │   │   │   │   │   │   ├── Color.kt    ← Brand palette
│   │   │   │   │   │   │   ├── Theme.kt    ← Compose theme
│   │   │   │   │   │   │   ├── Type.kt     ← Typography roles
│   │   │   │   │   │   │   └── Shape.kt    ← Corner radius scale
│   │   │   │   │   │   ├── tokens/
│   │   │   │   │   │   │   ├── Spacing.kt  ← 8-pt grid
│   │   │   │   │   │   │   ├── Elevation.kt← Layered shadow tokens
│   │   │   │   │   │   │   └── Motion.kt   ← Animation curves + durations
│   │   │   │   │   │   ├── components/
│   │   │   │   │   │   │   ├── Button.kt   ← PrimaryButton, GhostButton
│   │   │   │   │   │   │   ├── Card.kt     ← PremiumCard, GlassCard
│   │   │   │   │   │   │   ├── TextField.kt← FilledTextField, SearchField
│   │   │   │   │   │   │   ├── Chip.kt     ← AnimatedChip
│   │   │   │   │   │   │   ├── Icon.kt     ← AppIcon, custom ImageVector set
│   │   │   │   │   │   │   ├── NavBar.kt   ← Custom bottom nav
│   │   │   │   │   │   │   ├── Skeleton.kt ← Shimmer skeleton loaders
│   │   │   │   │   │   │   └── ...
│   │   │   │   │   │   └── effects/
│   │   │   │   │   │       ├── Modifier.neonGlow.kt
│   │   │   │   │   │       ├── Modifier.glassBlur.kt
│   │   │   │   │   │       ├── Modifier.pressScale.kt
│   │   │   │   │   │       └── Modifier.haptic.kt
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── api/            ← Retrofit API client
│   │   │   │   │   │   │   ├── Play2PdfApi.kt
│   │   │   │   │   │   │   └── dtos/       ← Response DTOs
│   │   │   │   │   │   ├── db/             ← Room database
│   │   │   │   │   │   │   ├── Play2PdfDatabase.kt
│   │   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   │   ├── HistoryDao.kt
│   │   │   │   │   │   │   │   └── SettingsDao.kt
│   │   │   │   │   │   │   └── entities/
│   │   │   │   │   │   ├── prefs/          ← DataStore preferences
│   │   │   │   │   │   │   └── SettingsRepository.kt
│   │   │   │   │   │   └── repository/
│   │   │   │   │   │       ├── CompileRepository.kt
│   │   │   │   │   │       ├── HistoryRepository.kt
│   │   │   │   │   │       └── ConnectionRepository.kt
│   │   │   │   │   ├── domain/            ← Use cases + models
│   │   │   │   │   │   ├── model/
│   │   │   │   │   │   │   ├── Playlist.kt
│   │   │   │   │   │   │   ├── Topic.kt
│   │   │   │   │   │   │   ├── PdfHistory.kt
│   │   │   │   │   │   │   └── Theme.kt
│   │   │   │   │   │   └── usecase/
│   │   │   │   │   │       ├── CompileGuideUseCase.kt
│   │   │   │   │   │       ├── TestConnectionUseCase.kt
│   │   │   │   │   │       └── ...
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── navigation/
│   │   │   │   │       │   └── Play2PdfNavHost.kt
│   │   │   │   │       ├── splash/
│   │   │   │   │       │   ├── SplashScreen.kt
│   │   │   │   │       │   └── SplashViewModel.kt
│   │   │   │   │       ├── onboarding/
│   │   │   │   │       │   └── OnboardingScreen.kt
│   │   │   │   │       ├── compile/
│   │   │   │   │       │   ├── CompileScreen.kt
│   │   │   │   │       │   ├── CompileViewModel.kt
│   │   │   │   │       │   └── components/
│   │   │   │   │       │       ├── PlaylistInputCard.kt
│   │   │   │   │       │       ├── TopicChipsCard.kt
│   │   │   │   │       │       ├── FeaturedPlaylistsRow.kt
│   │   │   │   │       │       └── ...
│   │   │   │   │       ├── history/
│   │   │   │   │       │   ├── HistoryScreen.kt
│   │   │   │   │       │   ├── HistoryViewModel.kt
│   │   │   │   │       │   └── components/
│   │   │   │   │       │       └── HistoryListItem.kt
│   │   │   │   │       ├── settings/
│   │   │   │   │       │   ├── SettingsScreen.kt
│   │   │   │   │       │   └── SettingsViewModel.kt
│   │   │   │   │       └── compiling/
│   │   │   │   │           ├── CompilingScreen.kt
│   │   │   │   │           └── CompilingViewModel.kt
│   │   │   │   └── di/
│   │   │   │       ├── AppModule.kt       ← Network, DB, DataStore
│   │   │   │       ├── RepositoryModule.kt
│   │   │   │       └── UseCaseModule.kt
│   │   │   ├── res/
│   │   │   │   ├── drawable/               ← Vector icons, illustrations
│   │   │   │   ├── drawable-nodpi/         ← Splash logo, empty states
│   │   │   │   ├── mipmap-anydpi-v26/      ← Adaptive icon (XML)
│   │   │   │   ├── mipmap-hdpi/            ← Legacy icon PNGs
│   │   │   │   ├── values/
│   │   │   │   │   ├── themes.xml          ← Material 3 base theme
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── strings.xml
│   │   │   │   ├── values-night/
│   │   │   │   │   └── themes.xml          ← Dark theme overrides
│   │   │   │   ├── font/                   ← Geist, Geist Mono, Fraunces
│   │   │   │   ├── raw/                    ← Lottie / Rive animations
│   │   │   │   │   ├── splash_logo.riv
│   │   │   │   │   ├── success_confetti.json
│   │   │   │   │   └── ...
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       └── data_extraction_rules.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                            ← Unit tests
│   │   └── androidTest/                    ← UI tests (Compose Test)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml                  ← Version catalog
│   └── wrapper/
├── build.gradle.kts                        ← Root build script
├── settings.gradle.kts
├── gradle.properties
├── docs/
│   └── PREMIUM_DESIGN_PLAN.md              ← This file
├── backend/                                ← Keep FastAPI backend here too
│   ├── server.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
└── README.md
```

### Tech stack decisions

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Kotlin 2.0+ | Modern, concise, null-safe, coroutine-native. |
| UI | Jetpack Compose 1.7+ | Declarative UI, modern Android standard. |
| DI | Hilt | First-party Android DI, simple for our scope. |
| Async | Coroutines + Flow | Idiomatic Kotlin, replaces RxJava. |
| Network | Retrofit + OkHttp + Moshi | Industry standard, well-documented. |
| Local DB | Room | First-party ORM, type-safe SQL. |
| Preferences | DataStore (Preferences) | Async, type-safe, replaces SharedPreferences. |
| Image loading | Coil 3 | Compose-native, fast, lightweight. |
| Animations | Compose Animation + Lottie + Rive | Native + Lottie for designer-exported animations. |
| Navigation | Navigation Compose | First-party, type-safe. |
| Testing | JUnit4 + Compose Test + Truth | Standard stack. |
| Min SDK | 26 (Android 8.0) | 95%+ market share, gives us adaptive icons + adaptive fonts. |
| Target SDK | 35 (Android 15) | Latest, predictive back, themed icons. |

---

## 5. Brand Identity — The Foundation

### 5.1 Brand name treatment

**Play2PDF** — keep as is. The "2" is the brand signature; render it
in the brand accent color (see §5.3) every time it appears.

### 5.2 Brand promise (one sentence)

> "Turn any YouTube playlist into a print-ready, LaTeX-grade PDF
> study guide — in one tap."

### 5.3 Brand color — pick ONE and commit

| Role | Hex | Use |
|------|-----|-----|
| Brand | `#7C5CFF` | Buttons, accents, active states, logo |
| Brand dark | `#5B3FD6` | Pressed states |
| Brand tint | `#7C5CFF22` | Subtle backgrounds, chip backgrounds |
| Brand glow | `#7C5CFF66` | Glow shadows |
| Ink | `#F4F4F5` | Primary text |
| Ink muted | `#A1A1AA` | Secondary text |
| Ink faint | `#71717A` | Captions, disabled |
| Surface 0 | `#09090B` | App background |
| Surface 1 | `#18181B` | Cards |
| Surface 2 | `#27272A` | Elevated cards, chips |
| Surface 3 | `#3F3F46` | Pressed states |
| Success | `#10B981` | Emerald |
| Warning | `#F59E0B` | Amber |
| Danger | `#EF4444` | Red |

### 5.4 Logo spec — the single most important asset

**Concept:** A single-color mark that reads as "playlist → document".

**Construction (left to right):**
1. **Three stacked horizontal bars** of decreasing width (40px, 28px,
   16px at 1024px scale), each 8px tall, 12px apart — these represent
   a video playlist.
2. **An arrow or fold crease** — a 4px-thick diagonal line going from
   the top of the third bar down-right to a 45° page-corner fold.
   This is the "transformation" gesture.
3. **A page corner** — the top-right of the mark has a folded-down
   triangle (16×16px), revealing the page beneath.

**Style:**
- Single weight, single color (Brand `#7C5CFF` on light, white on dark).
- 2px stroke everywhere, no fills.
- Sharp corners on the bars (video thumbnails).
- Rounded corners on the page (PDF output).
- Geometric, not illustrative.

**Tests the logo must pass:**
- ✅ Readable at 16×16 (favicon size).
- ✅ Readable at 32×32 (status bar icon).
- ✅ Readable at 48×48 (in-app icon).
- ✅ Readable in single-color white (on dark backgrounds).
- ✅ Readable in single-color black (on light backgrounds).
- ✅ Readable in silhouette (no color, just alpha).
- ✅ Recognizable when blurred to 4px (the "glance test").

**Deliverables:**
- `logo_mark.svg` — single-color vector, 1024×1024 viewBox.
- `logo_mark_full.svg` — mark + wordmark "Play2PDF" side-by-side.
- `logo_mark_stacked.svg` — mark above wordmark, for splash and onboarding.
- PNG exports at 16, 32, 48, 96, 192, 512, 1024 (each in white, black,
  and brand color = 21 PNG files).

### 5.5 Typography — pick a real pairing

**Recommended pairing (free):**
- **Display + Body:** **Geist** (by Vercel) — modern geometric sans.
- **Code/Numbers:** **Geist Mono**.
- **Optional accent:** **Fraunces** (variable serif) for editorial
  moments (PDF cover preview, About page).

**Type roles:**

| Role | Font | Size | Weight | Line height | Letter spacing | Color |
|------|------|------|--------|-------------|----------------|-------|
| Display | Geist | 40 | 700 | 1.1 | -0.02em | Ink |
| Title 1 | Geist | 28 | 600 | 1.2 | -0.01em | Ink |
| Title 2 | Geist | 22 | 600 | 1.3 | -0.01em | Ink |
| Title 3 | Geist | 18 | 600 | 1.3 | 0 | Ink |
| Body | Geist | 15 | 400 | 1.5 | 0 | Ink muted |
| Body small | Geist | 13 | 400 | 1.45 | 0 | Ink muted |
| Caption | Geist | 12 | 500 | 1.4 | 0.02em | Ink faint |
| Micro / label | Geist | 11 | 600 | 1.4 | 0.08em uppercase | Brand |
| Code / number | Geist Mono | 14 | 500 | 1.5 | 0 | Ink |
| Stat number | Geist | 48 | 700 | 1 | -0.02em | Ink |

**Uppercase + 8% letter-spacing on micro labels** is the single
fastest way to make UI feel premium (Linear, Stripe, Vercel all do
this).

### 5.6 Spacing — adopt the 8-pt grid

| Token | Value | Use |
|-------|-------|-----|
| `space.0` | 0 | — |
| `space.1` | 4 | Tight inline (icon + text gap) |
| `space.2` | 8 | Default inline |
| `space.3` | 12 | Card padding (small) |
| `space.4` | 16 | Card padding (default) |
| `space.5` | 24 | Card-to-card vertical |
| `space.6` | 32 | Section-to-section |
| `space.7` | 48 | Major section break |
| `space.8` | 64 | Tablet screen-edge padding |

### 5.7 Radius — 3-step scale

| Token | Value | Use |
|-------|-------|-----|
| `radius.sm` | 8 | Chips, badges, inputs |
| `radius.md` | 12 | Cards, list items |
| `radius.lg` | 20 | Sheets, modals |
| `radius.pill` | 999 | Pills, primary buttons |

---

## 6. Asset Inventory — What To Produce

### 6.1 Android adaptive icon

Three layers, each 1024×1024 PNG with alpha. Outer 18% per side is
masked off (safe zone = inner 66%).

| File | Spec |
|------|------|
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | `<adaptive-icon>` XML referencing foreground + background |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | VectorDrawable, logo mark in brand color, 108×108dp viewport (inner 72dp safe zone) |
| `app/src/main/res/drawable/ic_launcher_background.xml` | VectorDrawable, solid `#09090B` or radial gradient |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | VectorDrawable, single-color white silhouette (for Android 13+ themed icons) |

**Use VectorDrawable XML (not PNG)** for adaptive icons — it scales
perfectly at every density and supports Android 13's themed icon
system.

### 6.2 Splash icon (native Android splash)

| File | Spec |
|------|------|
| `app/src/main/res/drawable/splash_icon.xml` | VectorDrawable, logo mark in brand color, sized 120×120dp. Used by Android 12+ `SplashScreen` API before Compose boots. |

### 6.3 In-app logo

| File | Spec |
|------|------|
| `app/src/main/res/drawable/logo_mark.xml` | VectorDrawable, the logo mark. |
| `app/src/main/res/drawable/logo_wordmark.xml` | VectorDrawable, the "Play2PDF" wordmark with brand-colored "2". |

### 6.4 Custom icon set (Kotlin ImageVector)

Custom `ImageVector` definitions in Kotlin for ~24 icons. Each is a
composable function returning an `ImageVector`, defined with the
`materialIcon` builder DSL.

| Icon | File | Concept |
|------|------|---------|
| `AppIcons.Playlist` | `IconPlaylist.kt` | 3 stacked bars, decreasing width |
| `AppIcons.Topic` | `IconTopic.kt` | Bookmark with center dot |
| `AppIcons.Book` | `IconBook.kt` | Open book, 2 lines per page |
| `AppIcons.Compile` | `IconCompile.kt` | Document + corner sparkle |
| `AppIcons.History` | `IconHistory.kt` | Clock at 10:10 |
| `AppIcons.Settings` | `IconSettings.kt` | 6-tooth gear |
| `AppIcons.Search` | `IconSearch.kt` | Magnifier, 2px stroke |
| `AppIcons.Filter` | `IconFilter.kt` | 3 lines, decreasing width |
| `AppIcons.Bell` | `IconBell.kt` | Bell + indicator dot |
| `AppIcons.Pdf` | `IconPdf.kt` | Document + corner fold |
| `AppIcons.More` | `IconMore.kt` | 3 vertical dots |
| `AppIcons.Delete` | `IconDelete.kt` | Trash can |
| `AppIcons.Download` | `IconDownload.kt` | Down arrow + tray |
| `AppIcons.OpenExternal` | `IconOpenExternal.kt` | Box + up-right arrow |
| `AppIcons.Key` | `IconKey.kt` | Key |
| `AppIcons.Wifi` | `IconWifi.kt` | Wifi arcs |
| `AppIcons.Cloud` | `IconCloud.kt` | Cloud |
| `AppIcons.User` | `IconUser.kt` | Person silhouette |
| `AppIcons.Close` | `IconClose.kt` | X, rounded caps |
| `AppIcons.Check` | `IconCheck.kt` | Checkmark |
| `AppIcons.Error` | `IconError.kt` | X in circle |
| `AppIcons.Plus` | `IconPlus.kt` | + rounded caps |
| `AppIcons.Play` | `IconPlay.kt` | Triangle play |
| `AppIcons.Sparkle` | `IconSparkle.kt` | 4-point star |

**Style spec:** 24×24 viewBox, 2px stroke, rounded caps, single color
via `tint = LocalContentColor.current`.

**Why Kotlin ImageVector (not SVG):** ImageVector composes faster
than SVG-as-Image, supports `tint`, integrates with Compose's `Icon()`
composable, and is fully type-safe at compile time.

### 6.5 Empty-state illustrations

Three custom vector illustrations. Style: geometric line art with one
accent color, 4px stroke, transparent or Surface 1 background.

| File | Size (dp) | Used when |
|------|-----------|-----------|
| `app/src/main/res/drawable/empty_history.xml` | 240×180 | History tab empty |
| `app/src/main/res/drawable/empty_playlists.xml` | 240×180 | Compile tab, playlists empty |
| `app/src/main/res/drawable/empty_topics.xml` | 240×180 | Compile tab, topics empty |

### 6.6 Splash motion asset

| File | Spec |
|------|------|
| `app/src/main/res/raw/splash_logo.riv` | Rive animation, 2 seconds, 60fps. Logo mark draws itself in (stroke-dashoffset), then a single pulse of brand glow radiates outward, then settles. |

If Rive is too complex, fall back to:
| File | Spec |
|------|------|
| `app/src/main/res/raw/splash_logo.json` | Lottie JSON exported from After Effects. |

### 6.7 Sound effects (optional but huge)

| File | Used when |
|------|-----------|
| `app/src/main/res/raw/sfx_tap.wav` | Button tap |
| `app/src/main/res/raw/sfx_chip_add.wav` | Chip added |
| `app/src/main/res/raw/sfx_chip_remove.wav` | Chip removed |
| `app/src/main/res/raw/sfx_success.wav` | PDF compiled |
| `app/src/main/res/raw/sfx_error.wav` | Compilation failed |
| `app/src/main/res/raw/sfx_nav.wav` | Tab change |

**Format:** ≤ 50ms, ≤ 5KB, mono, 44.1kHz 16-bit WAV.

### 6.8 PDF cover-page preview thumbnails

For each of the 13 PDF themes, render a 220×280 PNG showing what the
cover page actually looks like.

```
app/src/main/res/drawable-nodpi/
├── pdf_theme_tufte_scholar.png
├── pdf_theme_princeton_math.png
├── pdf_theme_midnight_terminal.png
├── pdf_theme_cambridge_emerald.png
├── pdf_theme_bauhaus_geometric.png
├── pdf_theme_swiss_stark.png
├── pdf_theme_oxford_burgundy.png
├── pdf_theme_deep_space.png
├── pdf_theme_mit_tech.png
├── pdf_theme_wharton_ledger.png
├── pdf_theme_sumi_ink.png
├── pdf_theme_renaissance_gold.png
└── pdf_theme_warm_sunset_dark.png
```

Generate by running each theme through the existing PDF backend once,
then `pdftoppm` or `pypdfium2` to render the cover page to PNG.

---

## 7. Visual System — Color, Type, Space, Motion

### 7.1 Real backdrop blur (glassmorphism)

Compose has real `Modifier.blur()` (RenderEffect-backed on Android
12+). For glassmorphism cards:

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .blur(radius = 20.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        content()
    }
}
```

For Android < 12, fall back to `RenderEffect.createBlurEffect` via
`Modifier.graphicsLayer { renderEffect = ... }`.

### 7.2 Real neon glow (multi-layer)

Real neon needs 3 stacked shadows. Here's the `Modifier` extension:

```kotlin
fun Modifier.neonGlow(
    color: Color = BrandColor,
    intensity: Float = 1f,
): Modifier = this.drawBehind {
    // Inner core — saturated, small radius
    drawCircle(
        color = color.copy(alpha = 0.8f * intensity),
        radius = size.minDimension * 0.4f * intensity,
        center = Offset(size.width / 2, size.height / 2),
    )
    // Mid halo
    drawCircle(
        color = color.copy(alpha = 0.4f * intensity),
        radius = size.minDimension * 0.6f * intensity,
        center = Offset(size.width / 2, size.height / 2),
    )
    // Outer falloff
    drawCircle(
        color = color.copy(alpha = 0.1f * intensity),
        radius = size.minDimension * 0.9f * intensity,
        center = Offset(size.width / 2, size.height / 2),
    )
}
```

Use on the splash logo, primary CTA, active nav indicator, success
checkmark. NOWHERE else.

### 7.3 Micro-typography in Compose

```kotlin
@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.88.sp,  // 8% of 11sp
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
fun StatNumber(value: Int, modifier: Modifier = Modifier) {
    Text(
        text = value.toString(),
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.96).sp,  // -2% of 48sp
        modifier = modifier,
    )
}
```

### 7.4 Motion principles — Compose animation APIs

| Interaction | Compose API | Curve | Duration |
|-------------|-------------|-------|----------|
| Button press scale | `animateFloatAsState` | `FastOutSlowInEasing` | 100ms |
| Button release | `animateFloatAsState` | `FastOutSlowInEasing` | 150ms |
| Card hover (desktop) | `animateFloatAsState` | `FastOutSlowInEasing` | 200ms |
| Chip add spring | `AnimatedVisibility` + `spring` | `Spring.DampingRatioMediumBouncy` | 250ms |
| Chip remove | `AnimatedVisibility` + `spring` | `Spring.DampingRatioNoBouncy` | 200ms |
| Page transition (forward) | `AnimatedContent` + `slideIntoContainer` | `FastOutSlowInEasing` | 300ms |
| Page transition (back) | `AnimatedContent` + `slideOutOfContainer` | `FastOutSlowInEasing` | 250ms |
| Modal open | `AnimatedVisibility` + `spring` | `Spring.DampingRatioMediumBouncy` | 350ms |
| Modal close | `AnimatedVisibility` | `FastOutSlowInEasing` | 200ms |
| Bottom sheet open | `ModalBottomSheet` | `SwipeableV2Defaults.AnimationSpec` | 400ms |
| Splash logo draw-in | `Lottie/Rive` | linear | 1200ms |
| Splash logo pulse | `rememberInfiniteTransition` | `FastOutSlowInEasing` | 800ms |
| Loading skeleton shimmer | `rememberInfiniteTransition` | linear | 1500ms |
| Success checkmark draw | `Lottie/Rive` | `FastOutSlowInEasing` | 600ms |
| Error shake | `Animatable` + keyframes | `FastOutSlowInEasing` | 400ms |

**Critical rule:** NEVER use the default `tween(300)`. Always specify
`FastOutSlowInEasing` (or `LinearOutSlowInEasing` for entrances,
`FastOutLinearInEasing` for exits).

---

## 8. Design System in Compose

### 8.1 `Color.kt`

```kotlin
package com.adnanfoisal.play2pdf.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Brand
val BrandColor = Color(0xFF7C5CFF)
val BrandDarkColor = Color(0xFF5B3FD6)
val BrandTint = Color(0x227C5CFF)
val BrandGlow = Color(0x667C5CFF)

// Ink (text)
val Ink = Color(0xFFF4F4F5)
val InkMuted = Color(0xFFA1A1AA)
val InkFaint = Color(0xFF71717A)

// Surface scale
val Surface0 = Color(0xFF09090B)
val Surface1 = Color(0xFF18181B)
val Surface2 = Color(0xFF27272A)
val Surface3 = Color(0xFF3F3F46)

// Semantic
val SuccessColor = Color(0xFF10B981)
val WarningColor = Color(0xFFF59E0B)
val DangerColor = Color(0xFFEF4444)
```

### 8.2 `Theme.kt`

```kotlin
package com.adnanfoisal.play2pdf.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Play2PdfColorScheme = darkColorScheme(
    primary = BrandColor,
    onPrimary = Ink,
    primaryContainer = BrandDarkColor,
    onPrimaryContainer = Ink,
    secondary = BrandColor,
    onSecondary = Ink,
    secondaryContainer = Surface2,
    onSecondaryContainer = Ink,
    tertiary = BrandColor,
    onTertiary = Ink,
    background = Surface0,
    onBackground = Ink,
    surface = Surface1,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = InkMuted,
    surfaceTint = BrandColor,
    outline = InkFaint,
    outlineVariant = Surface3,
    error = DangerColor,
    onError = Ink,
    errorContainer = DangerColor,
    onErrorContainer = Ink,
)

@Composable
fun Play2PdfTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Surface0.toArgb()
            window.navigationBarColor = Surface0.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = Play2PdfColorScheme,
        typography = Play2PdfTypography,
        shapes = Play2PdfShapes,
        content = content,
    )
}
```

### 8.3 `Type.kt`

```kotlin
package com.adnanfoisal.play2pdf.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.adnanfoisal.play2pdf.R

val GeistFontFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

val GeistMonoFontFamily = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_bold, FontWeight.Bold),
)

val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val Play2PdfTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.88.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.24.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.22.sp,
    ),
)
```

### 8.4 `Spacing.kt` (8-pt grid)

```kotlin
package com.adnanfoisal.play2pdf.core.designsystem.tokens

import androidx.compose.ui.unit.dp

object Spacing {
    val space0 = 0.dp
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 24.dp
    val space6 = 32.dp
    val space7 = 48.dp
    val space8 = 64.dp
}
```

### 8.5 `Shape.kt` (3-step scale)

```kotlin
package com.adnanfoisal.play2pdf.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Play2PdfShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 20.dp
    val pill = 999.dp
}
```

### 8.6 `Motion.kt` (curves + durations)

```kotlin
package com.adnanfoisal.play2pdf.core.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.tween

object Motion {
    // Easings
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1f)
    val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val LinearOutSlowIn = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val FastOutLinearIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    // Durations
    const val DurationFast = 150
    const val DurationMedium = 300
    const val DurationSlow = 500

    fun <T> fastOutSlowIn(durationMs: Int = DurationMedium): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMs, easing = EaseInOut)

    fun <T> linearOutSlowIn(durationMs: Int = DurationMedium): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMs, easing = LinearOutSlowIn)

    fun <T> fastOutLinearIn(durationMs: Int = DurationMedium): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMs, easing = FastOutLinearIn)
}
```

---

## 9. Component-Level Premium Polish

### 9.1 `PrimaryButton` (replaces `AppButton`)

```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.Primary,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = Motion.fastOutSlowIn(100),
        label = "pressScale",
    )

    val (bgColor, fgColor) = when (variant) {
        ButtonVariant.Primary -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        ButtonVariant.Secondary -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        ButtonVariant.Success -> SuccessColor to Ink
        ButtonVariant.Ghost -> Color.Transparent to Ink
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.pill))
            .background(
                if (variant == ButtonVariant.Primary) {
                    Brush.linearGradient(
                        colors = listOf(BrandColor, BrandDarkColor),
                    )
                } else {
                    Brush.linearGradient(listOf(bgColor, bgColor))
                }
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(Radius.pill),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = fgColor,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = fgColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = text,
                    color = fgColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.15).sp,
                )
            }
        }
    }
}
```

### 9.2 `PremiumCard` (replaces `AppCard`)

```kotlin
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember(onClick) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.99f
            isHovered -> 1.005f
            else -> 1f
        },
        animationSpec = Motion.fastOutSlowIn(200),
        label = "cardScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(Radius.md),
            )
            .shadow(
                elevation = if (isHovered) 6.dp else 4.dp,
                shape = RoundedCornerShape(Radius.md),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.4f),
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(Spacing.space4),
    ) {
        content()
    }
}
```

### 9.3 `GlassCard`

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(Color.Black.copy(alpha = 0.5f))
            .blur(20.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(Radius.lg),
            )
            .padding(Spacing.space4),
    ) {
        content()
    }
}
```

### 9.4 `AnimatedChip`

```kotlin
@Composable
fun AnimatedChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BrandTint,
) {
    val haptics = LocalHapticFeedback.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            initialScale = 0f,
        ) + fadeIn(),
        exit = scaleOut(
            animationSpec = tween(200, easing = Motion.EaseIn),
            targetScale = 0f,
        ) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(color)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(Radius.pill),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = text,
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        visible = false
                        onRemove()
                    },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Remove $text",
                        tint = InkMuted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
```

### 9.5 Custom `BottomNavBar`

```kotlin
@Composable
fun Play2PdfBottomBar(
    destinations: List<Play2PdfDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface1)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
            )
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val isSelected = currentRoute == destination.route
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.92f,
                    animationSpec = Motion.fastOutSlowIn(150),
                    label = "navIconScale",
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) BrandColor else InkFaint,
                    animationSpec = Motion.fastOutSlowIn(200),
                    label = "navIconColor",
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigate(destination.route)
                            }
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BrandTint else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = destination.label,
                        color = iconColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
```

### 9.6 `ShimmerSkeleton`

```kotlin
@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val brush = Brush.linearGradient(
        colors = listOf(Surface2, Surface3, Surface2),
        start = Offset(translateAnim * 1000, 0f),
        end = Offset((translateAnim * 1000) + 200, 200f),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}
```

---

## 10. Screen-by-Screen Premium Redesign

### 10.1 Splash screen

- Rive/Lottie animation of the logo drawing itself in (1200ms).
- After 600ms, cross-fade in the "Play2PDF" wordmark (with brand-colored "2").
- After 800ms, fade in the tagline.
- Hold for 2.5s total.
- Fade out + slight scale up (1.0 → 1.05) over 400ms → navigate to onboarding.
- Background: solid Surface0 (no gradient).

### 10.2 Onboarding (3-screen carousel)

- HorizontalPager with 3 pages.
- Page 1: "Drop a playlist. Get a study guide." + illustration.
- Page 2: "AI maps every topic to the right video." + illustration.
- Page 3: "Print-ready. LaTeX-grade. Yours." + illustration.
- 3-dot page indicator (brand color for active).
- Skip button (top-right, ghost style) on all pages.
- Get Started button (primary, gradient) only on page 3.
- Slide animations on page change.

### 10.3 Compile tab

- **Sticky top app bar** with "Play2PDF" wordmark + notification bell + search icon.
- **Greeting line** ("Good morning, Adnan") + current date subtitle.
- **Header banner** with stat card (compilations count + topics mastered, tabular figures).
- **Playlists card** with filled text field + paste button + neon-purple "+" button.
- **Topics card** with chips (auto-assigned colored dots based on topic hash).
- **Featured Playlists row** showing last 3 compilations as mini PDF cover previews.
- **Book details card** (subject + author).
- **Sticky bottom Compile button** with gradient + drop shadow + on-press scale + haptic.
- **Empty state** when no playlists AND no topics: illustration + "Add your first playlist" + upward arrow.

### 10.4 History tab

- **Animated search field** (expands from icon to full bar on tap).
- **Filter chips** (All / This week / This month / By subject) — brand-tinted when active.
- **List header** "My Study Guides" + sort icon.
- **List items** with mini PDF cover preview (40×56) + title + date + topic count + kebab menu.
- **Swipe-to-delete** with red background + trash icon (scales up + rotates as user swipes).
- **Undo SnackBar** for 5 seconds after delete (Gmail-style — no confirmation dialog).
- **Empty state** with illustration + "Your study guides will live here" + CTA.

### 10.5 Settings tab

- **4 section headers** (uppercase, letter-spaced labels): PROFILE / API CREDENTIALS / BACKEND CONNECTION / PDF THEME.
- **Inline auto-save** (debounced 500ms after typing stops, with "Saved ✓" indicator).
- **Per-field test buttons** for API keys (with ✓/✗ validation indicator).
- **Live backend status indicator** (auto-pings on app launch, shows green/yellow/red dot + latency).
- **PDF theme grid** with full cover-page previews (not just color swatches).
- **About section** at bottom: app version, Gemini model in use, Reset data, Send feedback.

### 10.6 Compiling screen

- **Branded loader** (logo mark drawing itself in on loop).
- **Conversational status**: "Waking up the server...", "Fetching 47 videos...", "Asking Gemini to match 12 topics...", "Generating your PDF...", "Saving to your device...".
- **Step checklist** with status icons (pending/active/done/error).
- **Cancel button** (top-left, ghost) with confirmation dialog.
- **Success state**: green checkmark + "Your study guide is ready!" + PDF cover preview + "Open PDF" + "Save to Downloads".
- **Error state**: red X + "Something went wrong" + error in code-style box + "Try again" + "Copy error".

---

## 11. Micro-Interactions & Motion Design

### 11.1 Press depth (everywhere)

```kotlin
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.97f,
    durationMs: Int = 100,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = Motion.fastOutSlowIn(durationMs),
        label = "pressScale",
    )
    this.scale(scale).clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {},
    )
}
```

### 11.2 Page transition (Navigation Compose)

```kotlin
NavHost(
    navController = navController,
    startDestination = "splash",
    enterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = Motion.fastOutSlowIn(300),
        ) + fadeIn(animationSpec = Motion.fastOutSlowIn(300))
    },
    exitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = Motion.fastOutSlowIn(250),
        ) + fadeOut(animationSpec = Motion.fastOutSlowIn(250))
    },
    popEnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = Motion.fastOutSlowIn(250),
        ) + fadeIn(animationSpec = Motion.fastOutSlowIn(250))
    },
    popExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = Motion.fastOutSlowIn(300),
        ) + fadeOut(animationSpec = Motion.fastOutSlowIn(300))
    },
)
```

### 11.3 Scroll-linked parallax

```kotlin
@Composable
fun HeaderBanner(historyCount: Int) {
    val scrollState = LocalScrollState.current
    val scrollProgress by remember {
        derivedStateOf { (scrollState.value / 200f).coerceIn(0f, 1f) }
    }
    val scale by animateFloatAsState(
        targetValue = 1f - (scrollProgress * 0.08f),
        label = "headerScale",
    )
    val alpha by animateFloatAsState(
        targetValue = 1f - (scrollProgress * 0.4f),
        label = "headerAlpha",
    )

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha),
    ) {
        // ...content...
    }
}
```

### 11.4 Success celebration (Lottie confetti)

```kotlin
@Composable
fun SuccessConfetti(
    trigger: Boolean,
    modifier: Modifier = Modifier,
) {
    if (trigger) {
        LottieAnimation(
            composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success_confetti)),
            iterations = 1,
            modifier = modifier.fillMaxSize(),
        )
    }
}
```

---

## 12. Sound & Haptics

### 12.1 Haptics manager

```kotlin
@Singleton
class HapticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun light() = vibrate(duration = 10, amplitude = 50)
    fun medium() = vibrate(duration = 20, amplitude = 100)
    fun heavy() = vibrate(duration = 50, amplitude = 200)
    fun success() = pattern(longArrayOf(0, 50, 50, 50, 50, 100))
    fun error() = vibrate(duration = 100, amplitude = 250)

    private fun vibrate(duration: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, amplitude.coerceAtMost(255))
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun pattern(timings: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}
```

### 12.2 Sound manager

```kotlin
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sounds = mutableMapOf<SoundEffect, Int>()

    init {
        sounds[SoundEffect.TAP] = soundPool.load(context, R.raw.sfx_tap, 1)
        sounds[SoundEffect.CHIP_ADD] = soundPool.load(context, R.raw.sfx_chip_add, 1)
        sounds[SoundEffect.CHIP_REMOVE] = soundPool.load(context, R.raw.sfx_chip_remove, 1)
        sounds[SoundEffect.SUCCESS] = soundPool.load(context, R.raw.sfx_success, 1)
        sounds[SoundEffect.ERROR] = soundPool.load(context, R.raw.sfx_error, 1)
        sounds[SoundEffect.NAV] = soundPool.load(context, R.raw.sfx_nav, 1)
    }

    fun play(effect: SoundEffect, volume: Float = 0.3f) {
        sounds[effect]?.let { id ->
            soundPool.play(id, volume, volume, 1, 0, 1f)
        }
    }
}

enum class SoundEffect { TAP, CHIP_ADD, CHIP_REMOVE, SUCCESS, ERROR, NAV }
```

### 12.3 Haptic patterns table

| Event | Pattern |
|-------|---------|
| Light tap | `light()` (10ms, 50 amplitude) |
| Medium tap | `medium()` (20ms, 100 amplitude) |
| Success | `success()` (light → 50ms → medium → 50ms → heavy) |
| Error | `error()` (100ms, 250 amplitude — single buzz) |
| Warning | `light()` → 30ms gap → `light()` |
| Chip add | `light()` → 30ms gap → `medium()` |
| Chip remove | `medium()` → 30ms gap → `light()` |
| Page nav | `light()` (10ms) |
| Modal open | `medium()` (20ms) |
| Modal close | `light()` (10ms) |
| Pull-to-refresh trigger | `medium()` (30ms) |

---

## 13. Implementation Order & Priority

### Phase 1 — Brand foundation (1-2 days, no code)

- [ ] Lock the brand color (`#7C5CFF`).
- [ ] Commission or draw the logo mark (§5.4).
- [ ] Pick the type pairing (Geist recommended, §5.5).
- [ ] Write down the type roles table (§5.5) and print it.

### Phase 2 — Asset production (3-5 days, mostly external)

- [ ] Logo mark SVG + PNG exports (§5.4, §6.3).
- [ ] Android adaptive icon: foreground, background, monochrome VectorDrawable XML (§6.1).
- [ ] Splash icon VectorDrawable (§6.2).
- [ ] Custom icon set: 24 Kotlin ImageVector definitions (§6.4).
- [ ] Empty-state illustrations: 3 VectorDrawables (§6.5).
- [ ] Splash Rive animation (§6.6).
- [ ] PDF theme preview thumbnails: 13 PNGs (§6.8).
- [ ] (Optional) Sound effects: 6 WAV files (§6.7).
- [ ] (Optional) Success confetti Lottie (§11.4).

### Phase 3 — Project scaffold (1 day)

- [ ] Create new Android Studio project with Empty Compose Activity.
- [ ] Set up `build.gradle.kts` with version catalog.
- [ ] Configure Hilt, Room, Retrofit, Coil, Navigation Compose, Lottie/Rive.
- [ ] Create the directory structure from §4.
- [ ] Set up `Color.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Motion.kt` from §8.
- [ ] Configure edge-to-edge + system bar styling.
- [ ] Set up Android 12+ SplashScreen API.

### Phase 4 — Design system components (2-3 days)

- [ ] `PrimaryButton` with variants (§9.1).
- [ ] `PremiumCard` with hover/press states (§9.2).
- [ ] `GlassCard` with real `Modifier.blur` (§9.3).
- [ ] `AnimatedChip` with spring (§9.4).
- [ ] `Play2PdfBottomBar` with custom indicator (§9.5).
- [ ] `ShimmerSkeleton` loader (§9.6).
- [ ] `Modifier.neonGlow` (§7.2).
- [ ] `Modifier.pressScale` (§11.1).
- [ ] Custom `ImageVector` icon set (§6.4).

### Phase 5 — Data layer (2 days)

- [ ] Define `dataclass` domain models (Playlist, Topic, PdfHistory, Theme).
- [ ] Set up Retrofit API client pointing at existing FastAPI backend.
- [ ] Set up Room database (History table, Settings table).
- [ ] Set up DataStore preferences (API keys, theme, onboarding complete).
- [ ] Write repository implementations (CompileRepository, HistoryRepository).
- [ ] Write Hilt modules for DI.

### Phase 6 — Screens (5-7 days)

- [ ] Splash (§10.1).
- [ ] Onboarding carousel (§10.2).
- [ ] Compile tab (§10.3).
- [ ] History tab (§10.4).
- [ ] Settings tab (§10.5).
- [ ] Compiling screen (animated loader + success/error states).
- [ ] Wire Navigation Compose with transitions (§11.2).

### Phase 7 — Micro-interactions (2 days)

- [ ] Tap feedback everywhere (§11.1).
- [ ] Chip springs.
- [ ] Page transitions (§11.2).
- [ ] Scroll-linked animations (§11.3).
- [ ] Success celebration (§11.4).
- [ ] Empty-state delight.
- [ ] Long-press context menus.
- [ ] Pull-to-refresh.

### Phase 8 — Sound & haptics (1 day)

- [ ] Implement `SoundManager` (§12.2).
- [ ] Implement `HapticsManager` (§12.1).
- [ ] Wire into all interactions per §12.3.
- [ ] Add Settings toggles for sound/haptics.

### Phase 9 — Polish & QA (2 days)

- [ ] Performance audit — 60fps scroll, sub-200ms tap response (Macrobench + Perfetto).
- [ ] Accessibility audit — TalkBack, WCAG AA contrast, touch target sizes.
- [ ] Real device testing on 3+ screen sizes (small phone, large phone, tablet).
- [ ] Slow-network testing (Android emulator network throttling).
- [ ] Low-battery mode testing.
- [ ] Predictive back gesture (Android 14+).
- [ ] Themed icons (Android 13+).
- [ ] Edge-to-edge camera cutout handling.

**Total estimated time:** 18-25 days of focused work, or 5-7 weeks
part-time. This is what "premium" actually costs.

---

## 14. Asset Generation Toolkit

### 14.1 Logo design

- **Figma** (free) — design as vector, export as SVG → convert to VectorDrawable via Android Studio's "Vector Asset" import.
- **Adobe Illustrator** (paid) — industry standard.
- **Affinity Designer** ($70 one-time) — best value alternative.
- **Hire a designer:** Dribbble, Behance, Fiverr ($50-500 for logo + brand kit).

### 14.2 Custom icon set (Kotlin ImageVector)

Two approaches:

**Approach A: SVG → VectorDrawable → ImageVector**
1. Design icons in Figma at 24×24 viewBox, 2px stroke.
2. Export as SVG.
3. In Android Studio: `res` → `New` → `Vector Asset` → import SVG.
4. Android Studio generates VectorDrawable XML.
5. Use the `materialIcon` DSL to wrap as `ImageVector`.

**Approach B: Hand-code ImageVector**
- Use the `materialIcon` DSL to define icons directly in Kotlin.
- More work, but full control.

**Fork starting points:**
- **Phosphor Icons** (free, MIT) — 6,000+ icons at 1.5px stroke. Fork and customize.
- **Lucide** (free, ISC) — 1,000+ icons, fork of Feather.

**Hire a designer:** $200-500 for 24-icon custom set on Dribbble.

### 14.3 Illustrations

- **LottieFiles** (free + paid) — Lottie animations and stills.
- **unDraw** (free, MIT) — open-source illustrations, recolor to match brand.
- **Storyset** (free) — customizable illustrations.
- **Blush** (free + paid) — customizable illustration packs.
- **Hire an illustrator:** $300-1,000 for 3 custom illustrations.

### 14.4 Splash animation

- **Rive** (free) — modern standard for app animations. Exports `.riv` files, ~10KB each. Rive has a native Compose library: `app.cash.rive:rive-android`.
- **Lottie** + After Effects (paid AE) — exports `.json`. Lottie Compose library: `com.airbnb.android:lottie-compose`.
- **Hire a motion designer:** $200-500 for 2-second splash.

### 14.5 Sound effects

- **Freesound** (free, CC0) — search "UI tap", "UI click", "notification". Filter ≤ 1 second.
- **Pixabay Sounds** (free).
- **Zapsplat** (free with attribution).
- **UI Sounds** (paid, $19) — premium UI pack, 200 sounds.
- **Hire a sound designer:** $50-200 for 6-sound UI pack.

### 14.6 PDF theme preview thumbnails

Generate via Python script using the existing FastAPI backend:

```python
import pypdfium2 as pdfium
import requests

THEMES = [
    "tufte_scholar", "princeton_math", "midnight_terminal",
    "cambridge_emerald", "bauhaus_geometric", "swiss_stark",
    "oxford_burgundy", "deep_space", "mit_tech", "wharton_ledger",
    "sumi_ink", "renaissance_gold", "warm_sunset_dark",
]

BACKEND = "https://adnanfoisal-play2pdf.hf.space"

for theme in THEMES:
    resp = requests.post(f"{BACKEND}/generate_guide", json={
        "youtube_key": "<your key>",
        "gemini_key": "<your key>",
        "subject": "Data Structures & Algorithms",
        "author": "Student / Creator",
        "playlist_urls": ["https://www.youtube.com/playlist?list=<your playlist>"],
        "topics": "Arrays, Linked Lists, Stacks",
        "theme": theme,
    }, timeout=300)
    pdf = pdfium.PdfDocument.from_bytes(resp.content)
    page = pdf[0]
    bitmap = page.render(scale=2.0)
    pil_image = bitmap.to_pil()
    pil_image.save(f"pdf_theme_{theme}.png")
    print(f"Generated: pdf_theme_{theme}.png")
```

Place output PNGs in `app/src/main/res/drawable-nodpi/`.

---

## 15. Quality Checklist

Before shipping ANY screen:

### 15.1 Visual

- [ ] No default Material Icons — all icons are custom `ImageVector`.
- [ ] No "card with 1px border" — every card has layered shadows.
- [ ] No hardcoded colors — all from `MaterialTheme.colorScheme`.
- [ ] No hardcoded sizes — all from `Spacing.*`, `Radius.*`.
- [ ] No `Color.Black` / `Color.White` literals — all from `Color.kt`.
- [ ] All uppercase labels have 8% letter-spacing.
- [ ] All long text has `maxLines` + `overflow = TextOverflow.Ellipsis`.

### 15.2 Motion

- [ ] Every interactive element has press scale (0.97, 100ms).
- [ ] Every page transition uses specified curve (not default).
- [ ] Every modal open/close has spring.
- [ ] Every list item add/remove has spring.
- [ ] No `Thread.sleep` in animations — always `delay()` in coroutines.
- [ ] No layout thrashing during animations.

### 15.3 Performance

- [ ] 60fps scroll on a mid-range device (verify with Macrobench).
- [ ] Sub-200ms tap response.
- [ ] No layout jank when keyboard opens (`WindowInsets.imeAnimationSource`).
- [ ] Splash exits under 3 seconds.
- [ ] App cold-start to first usable screen under 2 seconds.
- [ ] No "white flash" between splash and first screen.
- [ ] Use `derivedStateOf` for expensive state derivations.
- [ ] Use `key()` for list items to prevent unnecessary recomposition.

### 15.4 Accessibility

- [ ] All text contrast ≥ 4.5:1 (WCAG AA).
- [ ] All interactive elements ≥ 48×48dp touch target.
- [ ] All icons have `contentDescription` (or `null` if decorative).
- [ ] Focus order is logical (top-to-bottom, left-to-right).
- [ ] Focus ring visible on every interactive element.
- [ ] `Modifier.semantics` used where appropriate.
- [ ] Reduced motion respected (`Settings.Global.ANIMATOR_DURATION_SCALE`).

### 15.5 Copy

- [ ] No Lorem Ipsum.
- [ ] No TODO/FIXME in user-visible strings.
- [ ] No technical jargon (say "server URL", not "backend_url").
- [ ] Conversational tone in loading states ("Waking up the server...").
- [ ] Error messages tell the user what to do next.

### 15.6 Brand consistency

- [ ] Logo appears in splash, onboarding, app icon, About — all the same.
- [ ] Brand color in: primary CTA, active nav, focus ring, links, brand-tinted chips. NOWHERE else.
- [ ] Wordmark "Play2PDF" always has the "2" in brand color.
- [ ] No emoji in user-visible copy.

### 15.7 Native Android

- [ ] Edge-to-edge layout (`WindowCompat.setDecorFitsSystemWindows(window, false)`).
- [ ] System bars tinted to match app background.
- [ ] Predictive back gesture (Android 14+).
- [ ] Themed icons (Android 13+, `ic_launcher_monochrome`).
- [ ] Per-app language preferences (Android 13+).
- [ ] Adaptive icon for all launcher shapes.
- [ ] Splash screen via Android 12+ SplashScreen API.
- [ ] Deep link support (`<intent-filter>` for `play2pdf://` scheme).
- [ ] Notification channel for "PDF ready" notifications.

---

## Final note

This document is intentionally long and detailed because "premium"
lives in the details. Every section corresponds to a specific thing
that currently makes the app look amateur, and every fix is concrete
enough to implement directly in Kotlin + Jetpack Compose.

The bar is not "good enough for an Android app". The bar is "would
Linear / Arc / Things 3 / Craft ship this on iOS?" If the answer is
no, keep working.

When in doubt: **fewer things, done better**. A screen with 3
beautifully-designed composables beats a screen with 10 generic
ones every single time.

— End of document —
