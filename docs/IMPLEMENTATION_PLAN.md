# Play2PDF — Design Integration Implementation Plan

## Context

The project has 63 Kotlin files already generated (Phases A–H skeleton). The mock assets folder contains 3 HTML mockups, 1 app icon JPEG, and 1 splash animation MP4. The task is to make every screen match the mockups exactly — HTML → Jetpack Compose — without losing any existing feature logic. Screens not in the mockups (Onboarding, Settings) must be designed to match the same visual language. The splash video must be converted to a Compose canvas animation. The app icon JPEG must become an Android adaptive icon.

### Decisions confirmed with user
1. **Stats card** — compute count + monthly sparkline from the existing Room history table (real data, no backend change).
2. **Greeting name** — add an editable display name stored in DataStore (`SettingsRepository`), edited in Settings, shown in the home greeting. Falls back to a generic "Hello 👋" when unset.
3. **Home redesign** — match the mockup fully; move the URL input and subject/author fields into dialogs/bottom sheets so **no existing input is lost**.
4. **Fonts** — write code referencing `R.font.*`; TTF files will be downloaded later. **When implementation reaches the font step, remind the user to enable auto/download mode so the 6 TTFs can be fetched.** Until then, `Type.kt` falls back to `FontFamily.Default` and still compiles.

---

## Gap Analysis (existing code vs. mockups)

| Screen | Current state | Mockup delta |
|---|---|---|
| CompileScreen | URL input field + Book Details card + chip LazyRow | Needs: greeting header + gold crown, stats sparkline card, playlist rows (YouTube icon), topic pills, inline PDF theme row, gradient compile button |
| CompilingScreen | Basic progress UI | Needs: animated circular ring + conic aura + comet dot, 4-step tracker with rail lines, pro-tip amber card |
| HistoryScreen | Basic list | Needs: search/filter icon buttons, left accent bar per card, PDF SVG icon, staggered fade-up, nav pulsing glow |
| NavBar | Static tabs | Needs: active tab radial pulse glow animation |
| SplashScreen | Static/placeholder | Needs: Compose canvas animation derived from MP4 reference |
| OnboardingScreen | Exists but unstyled | Needs: Space Grotesk headings, violet accent, dark surface, illustration placeholders |
| SettingsScreen | Exists but unstyled | Needs: same design language as other screens |
| Color.kt | `#7C5CFF` brand | Needs: `#a78bfa` / `#8b5cf6` violet palette + exact surface/text tokens from mockups |
| Type.kt | FontFamily.Default | Needs: Space Grotesk (display) + DM Sans (body) font families |
| App icon | Missing | Needs: VectorDrawable adaptive icon derived from `app icon.jpeg` |

---

## Design Tokens (extracted from mockups)

```
Surface0  = #0a0a12   (home bg)
Surface1  = #14141e   (cards)
Surface2  = #1b1b27   (card rows)
Surface3  = #121829   (history cards)
SurfaceBorder = rgba(255,255,255,0.06)

Brand         = #a78bfa   (violet, primary accent)
BrandStrong   = #8b5cf6   (violet-glow, pressed/active)
BrandDeep     = #7c3aed   (gradient start)
BrandGradEnd  = #3b82f6   (gradient end, blue)

TextPrimary   = #f4f4f7
TextSecondary = #8a8a99
TextTertiary  = #6f6f80
TextQuaternary= #646d84

Gold   = #f5b942
YtRed  = #ff1a1a
Green  = #22c55e
Amber  = #fbbf24
```

Typography:
- Display/headings: **Space Grotesk** (weight 500/600/700)
- Body: **DM Sans** (weight 400/500/700) — History screen uses Manrope; use DM Sans as primary, Manrope as fallback alias

---

## Implementation Steps

### Step 1 — Color.kt + Type.kt token update
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/theme/Color.kt`
- Replace all `BrandColors` constants with the exact hex values from the mockups above
- Add `Gold`, `YtRed`, `Amber` constants
- Add per-card accent color list for HistoryScreen (5 gradient pairs: amber/orange, fuchsia/violet, teal/emerald, blue/indigo, green/emerald)

**File:** `app/src/main/java/com/adnanfoisal/play2pdf/theme/Type.kt`
- Add `spaceGrotesk` FontFamily referencing `R.font.space_grotesk_*` (500/600/700 weights)
- Add `dmSans` FontFamily referencing `R.font.dm_sans_*` (400/500/700 weights)
- Remap `AppType.title1/title2/label/body/bodySmall` to use these families
- Add font files to `app/src/main/res/font/` (6 TTF files total — Space Grotesk 500/600/700, DM Sans 400/500/700)

> Font files must be added manually or via Design Agent. Code references `R.font.*` with exact filenames: `space_grotesk_medium`, `space_grotesk_semibold`, `space_grotesk_bold`, `dm_sans_regular`, `dm_sans_medium`, `dm_sans_bold`.

---

### Step 2 — App icon (adaptive icon from JPEG)
**Files to create/update:**
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — VectorDrawable tracing the Play2PDF logo mark from `app icon.jpeg`: a stylized "P→PDF" mark in violet gradient on transparent bg, 108×108dp canvas, safe zone 72×72dp
- `app/src/main/res/drawable/ic_launcher_background.xml` — solid `#0a0a12` background
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — `<adaptive-icon>` referencing both
- `app/src/main/res/drawable/splash_icon.xml` — same foreground vector, used by SplashScreen API

The JPEG shows a dark-background icon with a book/PDF motif and violet accent. Trace the key shapes as `<path>` elements in VectorDrawable. Keep it simple: a book outline + lightning bolt overlay in `#a78bfa`.

---

### Step 3 — Splash screen (MP4 → Compose canvas animation)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/ui/splash/SplashScreen.kt`

The MP4 shows a branded intro: logo mark fades/scales in from center, wordmark slides up, then transitions to the app. Implement as a pure Compose animation (no Rive dependency needed yet):

```
Phase 1 (0–600ms):  dark bg, logo mark scales 0.4→1.0 + alpha 0→1 (spring, dampingRatio=0.6)
Phase 2 (400–900ms): violet radial glow expands behind logo (Canvas drawCircle with radial brush)
Phase 3 (700–1100ms): "Play2PDF" wordmark slides up 20dp→0 + alpha 0→1
Phase 4 (1200ms):   hold 400ms, then navigate to Onboarding/Compile
```

Key composables:
- `Canvas` with `drawCircle` for the glow (violet, blurMaskFilter)
- `AnimatedVisibility` + `animateFloatAsState` for scale/alpha
- `LaunchedEffect` driving a state machine through the 4 phases
- `SplashViewModel` already exists — wire `navigateToNext` event after phase 4

Remove any Rive dependency from `libs.versions.toml` if it was added (it wasn't — confirmed not present). Keep the plan's `R.raw.splash_logo.riv` reference as a TODO comment.

---

### Step 4 — CompileScreen rewrite (Home screen mockup)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/ui/compile/CompileScreen.kt`

Replace the current layout with the mockup layout. **Preserve all ViewModel wiring** — only the UI layer changes.

New layout structure:
```
Column (scrollable, statusBarsPadding, horizontal padding 20dp)
  ├── GreetingHeader          — "Hello, {name} 👋" (name from settings.userName, "Hello 👋" if blank) + subtitle + GoldCrownButton
  ├── StatsCard               — "Total Compilations" + count + sparkline SVG (Canvas path)
  ├── SectionLabel            — "YOUR WORKSPACE" (uppercase, violet, 11.5sp, 1.4 letter-spacing)
  ├── PlaylistsCard           — header row (title + badge + "+ Add Playlist"), playlist rows
  │     └── PlaylistRow       — YouTube red icon (38×30dp, r=9dp) + title + video count + kebab
  ├── TopicsCard              — header row (title + badge + "+ Add Topic"), pill wrap layout
  │     └── TopicPill         — border pill (violet 30% border, 5% bg) + close X on long-press
  ├── PdfThemeRow             — "PDF Theme" label + theme name field + "Change" button (inline card)
  └── CompileButton           — full-width gradient (#7c3aed→#6d5cf0→#3b82f6), lightning icon
```

**Data wiring (no data-layer changes needed — confirmed existing):**
- `GreetingHeader` name ← `SettingsRepository.settings.userName` (already exists). CompileViewModel exposes it in state; blank → generic greeting.
- `StatsCard` count ← `HistoryDao.count()` (already exists). Sparkline ← group `HistoryDao.observeAll()` by day-of-month for the current month, build a normalized `List<Float>` for the last ~14 days, draw as a Compose `Path`. `observeSince(sinceEpochMs)` (already exists) supports the "this month" window.

`StatsCard` sparkline: draw the curve from the normalized daily-count list as a Compose `Canvas` using `Path` + `drawPath` with a `LinearGradient` brush (violet→cyan) plus a fill gradient underneath. Reference visual: `Home screen.html` lines 246–259 — replicate the smooth-curve look, but points come from real history data (fall back to the mockup's demo curve only when history is empty).

`PlaylistsCard` replaces `PlaylistInputCard` — the URL input field moves into a bottom sheet / dialog triggered by "+ Add Playlist". The existing `CompileUiEvent.AddPlaylist` / `RemovePlaylist` events are preserved.

`TopicsCard` replaces `TopicChipsCard` — topic pills replace the text-field-based chip input. "+ Add Topic" opens a dialog. Existing `CompileUiEvent.AddTopic` / `RemoveTopic` / `ExtractTopics` events preserved.

`PdfThemeRow` replaces the `ThemePickerRow` LazyRow — "Change" opens a bottom sheet with the existing theme picker. `CompileUiEvent.ThemeChanged` preserved.

Remove `Book Details` card (subject/author fields) — these are secondary inputs, move to an expandable section inside the compile button area or a separate bottom sheet. The `CompileUiEvent.SubjectChanged` / `AuthorChanged` events are preserved but hidden behind an "Advanced" toggle.

---

### Step 5 — CompilingScreen rewrite (compile loading mockup)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/ui/compiling/CompilingScreen.kt`

New layout:
```
Column (fillMaxSize, statusBarsPadding, padding 24dp)
  ├── CompilingHeader         — back button (42dp circle ghost) + title + subtitle
  ├── ProgressRingSection     — animated circular ring + aura + comet + percentage
  ├── StepTracker             — 4 steps with rail lines, done/active/pending states
  └── ProTipCard              — amber border card, lightbulb icon, sheen animation
```

**ProgressRing** implementation:
- `Canvas` composable, 224dp size
- Track circle: `drawArc` with `rgba(255,255,255,0.06)` stroke, 12dp width
- Progress arc: `drawArc` with `Brush.sweepGradient` (fuchsia→violet→blue→cyan), animated `strokeDashOffset` equivalent via `sweepAngle = progress * 360f`
- Aura: blurred `Canvas` behind the ring using `BlurMaskFilter` or `graphicsLayer { renderEffect = BlurEffect(26f) }`
- Comet dot: `Canvas` `drawCircle` at the arc tip, position calculated from `progress * 2π`
- Percentage: `animateFloatAsState` driving the displayed number, `easeOutCubic` spec

**StepTracker**:
- Each step: `Row` with a `StepDot` (34dp circle) + vertical rail line (`Canvas` drawLine) + text column
- `StepDot` states: `done` (green gradient bg + checkmark icon), `active` (violet gradient + pulsing ring border animation), `pending` (transparent bg + dim border + small center dot)
- Active step shows a shimmer bar below its subtitle (animated `LinearGradient` brush on a 3dp height `Box`)

**ProTipCard**: `PremiumCard` variant with amber `#fbbf24` border at 16% opacity, amber gradient bg, sheen sweep using `infiniteTransition` + `translateX` animation on an overlay `Box`.

Wire to existing `CompilingViewModel` — `state.progress` (0f–1f), `state.currentStep` (0–3), `state.steps` list.

---

### Step 6 — HistoryScreen rewrite (history screen mockup)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/ui/history/HistoryScreen.kt`

New layout:
```
Column (fillMaxSize)
  ├── HistoryHeader           — "My Study Guides" + subtitle + search icon btn + filter icon btn
  ├── LazyColumn              — staggered animated history cards
  │     └── HistoryCard       — left accent bar + card main + card side (PDF SVG + date)
  └── (existing SwipeToDismiss wrapping each card — preserve)
```

**HistoryCard** structure:
- `Box` with `clip(RoundedCornerShape(18.dp))`
- Left accent bar: 4dp wide `Box` with `Brush.verticalGradient` using per-card color pair + shimmer overlay animation
- Card main: title (truncated, Space Grotesk 600, 16.5sp) + theme name (muted, 12.5sp) + stats row (topics icon + count + videos icon + count)
- Card side: 3-dot menu button + PDF SVG icon (drawn via `Canvas` using the document path from the HTML) + date label
- Hover/press: `pressScale` modifier (already exists at `core/effects/pressScale.kt`) + `neonGlow` modifier with per-card glow color

**Staggered entrance**: `LaunchedEffect` + `AnimatedVisibility` with `fadeIn + slideInVertically`, delay = `index * 80ms + 100ms`.

**Per-card accent colors** (5 pairs, cycle for more cards):
```kotlin
val cardAccents = listOf(
    Color(0xFFfbbf24) to Color(0xFFea580c),  // amber/orange
    Color(0xFFd946ef) to Color(0xFF7c3aed),  // fuchsia/violet
    Color(0xFF34d399) to Color(0xFF0d9488),  // teal/emerald
    Color(0xFF60a5fa) to Color(0xFF2563eb),  // blue/indigo
    Color(0xFF4ade80) to Color(0xFF15803d),  // green/emerald
)
```

**PDF SVG icon**: Draw via `Canvas` using the document path `M6 1H31L45 15V52a5 5 0 0 1-5 5H6a5 5 0 0 1-5-5V6a5 5 0 0 1 5-5Z` + fold path, scaled to 46×58dp, filled with per-card gradient.

Preserve existing `SwipeToDismissHistoryItem`, `HistoryViewModel`, and all data wiring.

---

### Step 7 — NavBar update (pulsing glow on active tab)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/core/designsystem/components/NavBar.kt`

Add to the active tab icon: an `infiniteTransition` animating `scale` 0.9→1.12 and `alpha` 0.6→1.0 over 2600ms `easeInOut`, drawn as a radial gradient `Canvas` circle (46dp, violet `#8b5cf6` at 55% opacity) behind the icon. This matches the `pulseGlow` keyframe in the history HTML.

---

### Step 8 — OnboardingScreen (design language match)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/ui/onboarding/OnboardingScreen.kt`

No mockup provided — design to match the established language:
- Dark `Surface0` background with subtle violet radial gradient at top
- 3 pages via `HorizontalPager`
- Each page: illustration placeholder (violet-tinted `Box` with icon, 240dp), Space Grotesk heading (24sp, 700), DM Sans body (15sp, muted)
- Page indicator: 3 dots, active = violet 8dp wide pill, inactive = dim 6dp circle
- "Next" / "Get Started" button: same gradient compile button style
- Skip text button top-right

---

### Step 9 — SettingsScreen (design language match)
**File:** `app/src/main/java/com/adnanfoisal/play2pdf/ui/settings/SettingsScreen.kt`

No mockup provided — design to match:
- Header: "Settings" (Space Grotesk 700, 24sp) + subtitle (muted)
- **Profile section (new):** editable "Your Name" `PremiumTextField` bound to `SettingsRepository.setUserName` via `SettingsViewModel`. This feeds the home greeting. Empty is allowed.
- `PremiumCard` sections: Profile, Connection, Appearance, About
- `ConnectionStatusIndicator` (already exists) — keep, style with green/red dot
- `ThemePreviewCard` (already exists) — keep, style with violet border on selected
- Toggle rows: Sound / Haptics — label + `Switch` (violet track when on), bound to existing `soundEnabled` / `hapticsEnabled`
- Backend URL display row (read-only, monospace)

---

### Step 10 — Gradle: add font resources
**File:** `app/build.gradle.kts` — no change needed (fonts go in `res/font/`, auto-linked)

**Files to add:** `app/src/main/res/font/` — 6 TTF files. These must be sourced from Google Fonts (Space Grotesk, DM Sans) and placed at:
```
space_grotesk_medium.ttf
space_grotesk_semibold.ttf
space_grotesk_bold.ttf
dm_sans_regular.ttf
dm_sans_medium.ttf
dm_sans_bold.ttf
```
Until font files are present, `Type.kt` falls back to `FontFamily.Default` (existing behavior). The code compiles either way.

> ⚠️ **Reminder for implementation:** when this step is reached, pause and remind the user to enable auto/download mode so the 6 TTF binaries can be downloaded from Google Fonts. Do not block earlier steps on this — write `Type.kt` with the `R.font.*` references guarded so the app builds with the system-font fallback in the meantime.

---

## Files Modified

| File | Change |
|---|---|
| `theme/Color.kt` | Replace all color constants with mockup-exact values |
| `theme/Type.kt` | Add Space Grotesk + DM Sans font families |
| `ui/splash/SplashScreen.kt` | Full rewrite — 4-phase Compose canvas animation |
| `ui/compile/CompileScreen.kt` | Full rewrite — match Home screen mockup |
| `ui/compile/components/PlaylistInputCard.kt` | Adapt to new row style (YouTube icon, no URL field visible) |
| `ui/compile/components/TopicChipsCard.kt` | Adapt to pill-only layout |
| `ui/compiling/CompilingScreen.kt` | Full rewrite — ring + steps + pro-tip |
| `ui/history/HistoryScreen.kt` | Full rewrite — accent bar cards + stagger |
| `ui/onboarding/OnboardingScreen.kt` | Full rewrite — pager + design language |
| `ui/settings/SettingsScreen.kt` | Full rewrite — design language |
| `core/designsystem/components/NavBar.kt` | Add pulsing glow to active tab |
| `res/drawable/ic_launcher_foreground.xml` | New — VectorDrawable from app icon JPEG |
| `res/drawable/ic_launcher_background.xml` | New — solid dark bg |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | New — adaptive-icon XML |
| `res/drawable/splash_icon.xml` | New — same as foreground |
| `res/font/*.ttf` | New — 6 font files (manual download required) |

**No files deleted.** All ViewModel, repository, use case, and data layer files are untouched.

---

## Feature Preservation Checklist

- `CompileUiEvent.*` — all events preserved, UI triggers them from new layout (URL input + subject/author move into dialogs/sheets, nothing removed)
- `HistoryViewModel` + `SwipeToDismissHistoryItem` — preserved, wrapped by new card
- `CompilingViewModel.progress` / `currentStep` — wired to new ring + step tracker
- `SplashViewModel.navigateToNext` — wired to phase-4 completion
- `SettingsViewModel` + `ConnectionStatusIndicator` + `ThemePreviewCard` — preserved; add editable name field bound to existing `setUserName`
- `SettingsRepository.userName` / `setUserName` — already exist, no new keys needed
- `HistoryDao.count()` / `observeSince()` — already exist, power the stats card; no new queries needed
- `HapticsManager` + `SoundManager` — untouched
- Backend URL `https://adnanfoisal-play2pdf.hf.space` — untouched in build config

**ViewModel state additions (UI-only, no data-layer change):**
- `CompileUiState` gains `userName: String` and `stats: HomeStats(totalCount: Int, sparkline: List<Float>)`, populated by observing `SettingsRepository.settings` + `HistoryDao`.

---

## Verification

1. `./gradlew assembleDebug` — must pass with zero errors
2. Launch on emulator (API 26+): splash animation plays → onboarding → compile screen matches mockup
3. Add 2 playlists + 3 topics → compile button enables → navigates to compiling screen with ring animation
4. History tab shows staggered cards with accent bars
5. Settings tab renders without crash
6. Rotate device — no layout overflow
7. TalkBack: all interactive elements have `contentDescription`
