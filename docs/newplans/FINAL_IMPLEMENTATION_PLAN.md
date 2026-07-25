# Play2PDF — Final Implementation Plan
### The definitive build spec. Claude has full creative direction.
Active repo: `C:\Users\adnan\Downloads\Play2PDF redefined\` (root only, `main` branch)
Split into **Part A (code — Claude)** and **Part B (assets — separate agent)**.

---

## 0. North Star

**Who:** A student who opens this app and feels like they've been handed a *powerful, precise tool* — something that makes studying feel less like a chore and more like leveling up.

**Feel:** Premium, confident, snappy. The reference points you gave — Speechify's "install-me-now" icon energy, YouTube's snappy responsive splash, Duolingo's playful-but-tight motion — all point the same direction: **fast feedback, purposeful motion, zero lag, nothing decorative that doesn't earn its place.**

**Motion law (applies everywhere):**
- Micro-interactions (press, tap, toggle): **snappy** — 120–160ms, no bounce, `EmphasizedDecelerate`.
- Transitions (screen, sheet, morph): **mid** — 300–450ms, one subtle settle, never sluggish.
- Ambient loops (aura, shimmer, pulse): slow and calm so they never compete with interaction.
- Rule: *interaction is instant, atmosphere is slow.* Never the reverse.

---

## 1. Creative Direction (my calls, since you gave me the wheel)

### 1.1 Color & Atmosphere
Your mockup colors are a *starting sample*. Here's the refined system I'm committing to — a deep near-black base with a violet→indigo→cyan accent arc. It reads premium (Speechify/Linear territory) and the cyan keeps it from feeling like every other purple app.

**Unified base + per-screen atmosphere.** One shared background token, but each screen gets a distinct *radial glow placement* so it feels alive without fragmenting the palette.

| Token | Hex | Use |
|-------|-----|-----|
| `Bg` | `#08080F` | Unified app base (slightly deeper than the sample) |
| `GlowViolet` | `#1E1733` | Top radial atmosphere (Home, Settings) |
| `GlowIndigo` | `#101A2E` | Bottom-right radial (History) |
| `GlowDeep` | `#0B0A18` | Compiling mid-stop |
| Accent primary | `#A78BFA` | Violet (unchanged — it's good) |
| Accent strong | `#8B5CF6` | Pressed/active |
| Accent arc | `#7C3AED → #6D5CF0 → #3B82F6 → #22D3EE` | Gradients, ring, CTA |
| Cyan spark | `#22D3EE` | Comet, sparkline tip, "alive" accents |

**Signature depth rule:** elevation = *colored glow matching the element's accent*, never a neutral gray drop shadow. This is the single most important visual signature — it's what makes cards feel like they're lit from within.

### 1.2 Typography — final call
**Space Grotesk** for display/headings (it earns the "tool" feeling — geometric, confident), **Inter** for body instead of DM Sans. Reason: Inter is the most legible UI face on Android at 13–15sp, renders razor-sharp, and pairs cleanly with Space Grotesk's geometry. DM Sans and Manrope both read slightly soft/informal for a "precision tool." Inter is the pro choice. (Part B sources Inter + Space Grotesk weight files.)

### 1.3 The splash transition — my recommendation (replaces the flip)
You didn't like the turn/flip. Here's what I'll build instead — **"Ingest & Crystallize":**

1. **Play button pulses in** (scale 0.6→1.0, snappy spring, ~250ms) with a soft violet glow behind it.
2. **The play triangle "pours"** — it dissolves into 3–4 particles/lines that stream downward and *stack into horizontal document lines* (the play shape literally becomes the text lines of a PDF). ~500ms.
3. **The document outline draws itself** around those lines (a stroke that traces the page + folded corner, like a signature being signed). ~400ms.
4. **A single cyan sweep** passes across the page (the "it's ready" shimmer) and the violet glow blooms. ~300ms.
5. **Wordmark rises** beneath with the tagline. ~350ms.

Total ~2.4s, no dead hold. It tells the product story — *video becomes document* — without a literal flip, and the "pour + crystallize" reads far more premium. This is a Compose Canvas + particle animation (no Lottie needed, fully themeable). If the existing `opening animation.json` Lottie turns out more polished when I run it, I'll A/B on device and keep the winner.

### 1.4 PDF Theme Preview (your explicit request — great idea)
A **horizontal carousel of mini PDF "page" previews** appears in two places:
- **Home tab**, directly above/around the PDF Theme selector — each theme rendered as a ~90×120dp miniature page showing its actual fonts, colors, heading style, and a few lines of body text.
- **Theme picker sheet** — the same previews, larger, tappable to select.

Each mini-preview is a real Compose-drawn page (not an image), so it always reflects the true theme. Selecting one animates a violet ring + subtle lift. This lets anyone *see* what they'll get before compiling. Themes come from `PdfTheme.entries` — I'll render a faithful mini for each (Academic, Minimal, Modern, etc.).

### 1.5 Dialogs → **Modal Bottom Sheets**
For Add Playlist / Add Topic / Advanced / Theme picker: native-feeling **bottom sheets** that slide up, dark-themed, with a drag handle, `PremiumTextField`, and the accent-glow CTA. This is what a user intuitively expects on Android in 2026 and it feels far more premium than a centered dialog. Sheets get a scrim + spring-in (mid speed).

### 1.6 Bottom nav
**Frosted gradient fade** applied app-wide (not just History) — the bar fades from transparent into a dark blur so content scrolls *under* it. Active tab: violet label + the pulse glow (kept, it's good). Inactive tabs: muted, but get a **quick 120ms scale+color settle** when you land on them so the bar feels responsive, not dead.

### 1.7 Sound & Haptics — my recommendation: **yes, tastefully**
- **Haptics on meaningful moments only:** compile start (medium tick), each step complete (light tick), success (double tick), error (sharp buzz), theme select (light tick). NOT every button — that gets numbing.
- **Sound: subtle and optional (default ON, toggle in Settings):** a soft "whoosh" on compile start, a gentle rising 3-note chime on success, a low tap on step completion. Nothing on ordinary taps. Part B sources 3–4 short royalty-free UI sounds. Keeps the "leveling up" feeling.

### 1.8 Extra improvements I'm adding (you asked me to suggest freely)
- **Compile success = a moment.** Confetti already exists — I'll pair it with the success chime, a PDF-icon "pop + settle," and a one-line encouraging message ("Your study guide is ready 🎓"). Make the payoff feel earned.
- **Empty states with personality.** History empty state: a friendly illustrated PDF + "Your library starts with one compile." Not a bare icon.
- **Skeleton shimmer** on playlist metadata fetch (already have `ShimmerSkeleton`) so loading never feels broken.
- **Pull-to-refresh** on History (component exists) with a custom violet spinner.
- **Progress ring "breathing" + comet** on Compiling (already spec'd) — plus the percentage counts up with easing, not linearly, so it feels organic.
- **Haptic + scale on the Compile CTA** so the primary action feels physical and rewarding.
- **Consistent 42dp circular ghost buttons** extracted into one reusable component (currently duplicated) so every icon button behaves identically.
- **Reduce-motion respect:** honor the OS "remove animations" accessibility setting — swap ambient loops for static states. Premium apps do this.

<!-- SECTION:CREATIVE:DONE -->
---

## PART A — Code Work (Claude executes)

### A0 — Foundation fixes (do first, 1 pass)

**`theme/Type.kt`**
- Fix `@Composable get()` on FontFamily — move to top-level `val` (critical perf bug, creates new instances every recomposition)
- Update font references to weight-specific files once Part B delivers them: `R.font.space_grotesk_medium/semibold/bold`, `R.font.inter_regular/medium/semibold/bold`
- Remove legacy stubs (`GeistFamily`, `GeistMonoFamily`, `FrauncesFamily`)
- Add `inter` FontFamily alongside SpaceGrotesk; update all body/caption/label roles to use Inter

**`theme/Color.kt`**
- Deepen base: `Bg = Color(0xFF08080F)` (replaces Surface0 as the unified app background)
- Add atmospheric tokens: `GlowViolet = Color(0xFF1E1733)`, `GlowIndigo = Color(0xFF101A2E)`, `GlowDeep = Color(0xFF0B0A18)`
- Add `SurfaceBorderStrong = Color(0x1FFFFFFF)` (hover/active borders)
- Unify text colors to Home values across all screens (remove per-screen variation)

**`theme/Shape.kt`**
- Add mockup-exact radii: `card = 18.dp`, `row = 13.dp`, `button = 15.dp`, `pill22 = 22.dp`, `ytIcon = 9.dp`

**`tokens/Spacing.kt`**
- Add off-grid tokens used by mockups: `smPlus = 9.dp`, `mdMinus = 14.dp`, `mdPlus = 18.dp`, `lgMinus = 20.dp`, `lgPlus = 22.dp`

**`core/designsystem/components/`**
- Extract a shared `GhostIconButton(icon, onClick, modifier)` composable (42dp circle, `rgba(255,255,255,0.05)` bg, 1px border, pressScale 0.92) — currently duplicated in CompileScreen, HistoryScreen, CompilingScreen

---

### A1 — SplashScreen — "Ingest & Crystallize" (2 passes)

**Pass 1 — New animation sequence:**
Replace the `scaleX` flip with the "Ingest & Crystallize" sequence:
1. Play button pulses in: `scale 0.6→1.0`, snappy spring (`DampingRatioNoBouncy`, `StiffnessMedium`), alpha 0→1, ~250ms
2. Triangle "pours": 3 particle lines animate from triangle vertices downward, converging into 3 horizontal document lines. Implemented as `Canvas` with `Animatable<Float>` driving path interpolation, ~500ms, `EmphasizedDecelerate`
3. Document outline draws itself: a `PathEffect.dashPathEffect` stroke traces the page outline + folded corner from 0→full length, ~400ms
4. Cyan sweep: a `Brush.linearGradient` shimmer sweeps left→right across the document face, ~300ms
5. Violet glow blooms behind the document: radial gradient alpha 0→0.6, scale 0.4→1.0, ~300ms
6. Wordmark rises: `slideInVertically { it/6 } + fadeIn`, 350ms, `EmphasizedDecelerate`
7. Navigate at 2400ms total — no dead hold

Replace sequential `delay()` with a single `Animatable<Float>` timeline (0f→1f over 2400ms) with `keyframes {}` block — drift-free.

**Pass 2 — Lottie A/B:**
- Copy `mock assests/opening animation.json` → `app/src/main/res/raw/opening_animation.json`
- Add `LottieAnimation` variant gated by `const val USE_LOTTIE_SPLASH = false`
- Run both on device, pick the better one, delete the other, remove the flag

**Files:** `ui/splash/SplashScreen.kt`, `app/src/main/res/raw/opening_animation.json`

---

### A2 — CompileScreen (2 passes)

**Pass 1 — Token cleanup + dialog overhaul:**
- Replace all raw `dp`/`sp` literals with `Spacing.*`, `AppShape.*`, `AppType.*`
- Fix top padding to `Spacing.mdMinus` (14dp, was 12dp)
- Replace 4 `AlertDialog` + `OutlinedTextField` dialogs with `ModalBottomSheet`:
  - Each sheet: drag handle, `Surface1` bg, `PremiumTextField`, accent-glow CTA button
  - Spring-in: `spring(DampingRatioLowBouncy, StiffnessMedium)` on `sheetState`
  - Theme picker sheet: horizontal `LazyRow` of `PdfThemeMiniPreview` cards (see A5)

**Pass 2 — PDF Theme mini-preview on home tab:**
- Add `PdfThemePreviewRow` composable directly in `CompileScreen` above the theme selector row
- Horizontal `LazyRow` of `PdfThemeMiniPreview(theme, selected, onSelect)` cards
- Each card: 90×120dp, `AppShape.medium` radius, drawn entirely in Compose (no images):
  - Header bar in theme's accent color
  - 2 "heading" lines (Space Grotesk weight, theme color)
  - 3 "body" lines (Inter, muted)
  - Footer line
  - Selected state: violet ring border + 2dp lift shadow with violet glow
  - Tap to select with `pressScaleClickable` (scale 0.95)

**Files:** `ui/compile/CompileScreen.kt`, new `ui/compile/components/PdfThemeMiniPreview.kt`

---

### A3 — CompilingScreen (3 passes)

**Pass 1 — Atmospheric background:**
- Replace flat bg with `Box` layers:
  1. `Color(0xFF060509)` fill
  2. `Brush.radialGradient(listOf(Color(0xFF1a1730), Color.Transparent))` centered at `(0.5f, 0f)`, radius `height * 0.75f`
  3. `Brush.radialGradient(listOf(Color(0xFF0c1a2c).copy(alpha=0.45f), Color.Transparent))` at `(1f, 1f)`, radius `height * 0.55f`
- Fix aura: `Brush.sweepGradient` (not radial) + `infiniteRepeatable` `rotationZ` 0→360 over 9000ms `LinearEasing` + `Modifier.blur(26.dp)` on API 31+, skip on lower
- Add ring arc glow: two additional `drawArc` calls at `strokeW * 1.8f` width, `Fuchsia.copy(alpha=0.18f)` and `Cyan.copy(alpha=0.12f)`, drawn before the main arc
- Add percentage text glow via `Modifier.neonGlow(BrandColors.BrandStrong, radius=24.dp, alpha=0.35f)`

**Pass 2 — Step animations:**
- Staggered step entrance: `AnimatedVisibility` with `fadeIn(tween(550)) + slideInVertically { it/5 }`, delays 350/500/650/800ms
- Active step pulse ring: `rememberInfiniteTransition` → `scale 0.85→1.5`, `alpha 0.8→0`, 1800ms, drawn as `Canvas` circle behind the active dot
- Header entrance: `AnimatedVisibility(visible=true, enter=fadeIn(tween(600)) + slideInVertically { -it/4 })`
- Ring pop-in: `animateFloatAsState(0.85f→1.0f, tween(800, easing=EmphasizedDecelerate))` on first composition, `Modifier.scale(ringScale)`

**Pass 3 — Pro-tip micro-details + responsive sizing:**
- Sheen skew: `graphicsLayer { rotationZ = -3f }` on sheen overlay
- Bulb flicker: `rememberInfiniteTransition` → alpha keyframes `1.0→0.75→1.0→0.85→1.0` over 3500ms
- Responsive ring: `if (LocalConfiguration.current.screenHeightDp < 700) 180.dp else 224.dp`
- Done step dot: add `Modifier.shadow(elevation=8.dp, spotColor=BrandColors.Green.copy(alpha=0.5f))`
- Active step dot: add `Modifier.shadow(elevation=10.dp, spotColor=BrandColors.BrandStrong.copy(alpha=0.6f))`

**Files:** `ui/compiling/CompilingScreen.kt`

---

### A4 — HistoryScreen (2 passes)

**Pass 1 — Atmosphere + card polish:**
- Background: `Color(0xFF05070d)` base + two radial overlays (`#131a2c` top, `#0e1422` bottom-right)
- Card top gradient overlay: `Brush.verticalGradient(listOf(Color.White.copy(alpha=0.025f), Color.Transparent), endY = 0.6f * cardHeight)` inside `HistoryCard`
- Accent bar shimmer: `rememberInfiniteTransition` → `translationX` sweep on a white gradient overlay inside the 4dp bar, 3400ms `LinearEasing`
- Nav bar gradient: `Brush.verticalGradient(listOf(Color(0x00080B14), Color(0x99080B14)))` in `Play2PdfBottomBar`

**Pass 2 — Card interactions + search/filter:**
- Card press glow: `interactionSource.collectIsPressedAsState()` → `Modifier.shadow(16.dp, spotColor=accentTop.copy(alpha=0.45f), ambientColor=Color.Black.copy(alpha=0.6f))` when pressed
- PDF icon press: `animateFloatAsState` for `rotation` (-3f pressed, 0f rest) + `scale` (1.04f pressed, 1f rest) via `graphicsLayer`
- Search: animated `SearchBar` that expands inline (height 0→56dp, `animateDpAsState`, 200ms) on search icon tap, backed by `HistoryViewModel.setQuery()`
- Filter: `ModalBottomSheet` with sort options (Newest/Oldest/A-Z) backed by `HistoryViewModel.setSort()`

**Files:** `ui/history/HistoryScreen.kt`, `core/designsystem/components/NavBar.kt`

---

### A5 — SettingsScreen + OnboardingScreen (1 pass each)

**SettingsScreen:**
- Atmospheric background (same multi-layer radial pattern, `GlowViolet` at top)
- Add subtitle: `"Configure your keys and preferences"` in `AppType.bodySmall` / `TextSecondary`
- `SectionHeader` color: `Brand` (was `TextTertiary`) — matches CompileScreen eyebrow pattern
- Add `PdfThemePreviewRow` (same component from A2) in the PDF Theme section so users can preview themes here too

**OnboardingScreen:**
- Atmospheric background
- Active dot: animate width `8.dp→24.dp` (`animateDpAsState`, 200ms) + `AppShape.pill` shape
- Page parallax-fade: `graphicsLayer { alpha = lerp(0.5f, 1f, 1f - abs(pageOffset)); translationX = lerp(32f, 0f, 1f - abs(pageOffset)) }` on each page
- Illustration slot: `painterResource(page.illustrationRes)` — each `OnboardingPage` data class carries its drawable ID (Part B delivers the 3 PNGs)

**Files:** `ui/settings/SettingsScreen.kt`, `ui/onboarding/OnboardingScreen.kt`

---

### A6 — Compile success moment (1 pass)

- Pair `SuccessConfetti` with a PDF icon "pop + settle": `animateFloatAsState(0f→1.1f→1.0f)` spring on the check icon
- Add encouraging message: `"Your study guide is ready 🎓"` in `AppType.title2`
- Trigger success haptic: `HapticsManager.doubleClick()` on `CompilingPhase.Success`
- Trigger success sound: `SoundManager.playSuccess()` on `CompilingPhase.Success` (if sound enabled)

**Files:** `ui/compiling/CompilingScreen.kt`, `core/haptics/HapticsManager.kt`, `core/sound/SoundManager.kt`

---

### A7 — Reduce-motion + accessibility (1 pass)

- Read `LocalReduceMotion` (via `LocalContext` + `Settings.Global.TRANSITION_ANIMATION_SCALE`) — if animations disabled, replace all `rememberInfiniteTransition` loops with static states
- Verify `contentDescription` on every interactive element
- Verify all touch targets ≥ 48dp
- Verify `pressScaleClickable` on every tappable element
- Remove all remaining `// TODO` comments that are now resolved

**Files:** All screens + `core/effects/`

---

### A8 — Polish + commit (1 pass, final)

- Grep for raw `Color(0x` outside `Color.kt` — replace with tokens
- Grep for raw `.dp` literals outside token files — replace with `Spacing.*` or `AppShape.*`
- `./gradlew assembleDebug` — zero errors, zero warnings
- Commit: `feat(ui): complete visual overhaul — atmospheric backgrounds, animations, PDF theme previews, bottom sheets`

---

### Part A execution order

```
A0 (foundation fixes)
  ↓
A1 Pass 1 (splash new animation)
A2 Pass 1 (compile dialogs → sheets)     ← parallel
  ↓
A3 Pass 1 (compiling background)
A4 Pass 1 (history background)           ← parallel
  ↓
A3 Pass 2 (step animations)
A4 Pass 2 (card interactions)            ← parallel
A5 (settings + onboarding)               ← parallel
  ↓
A2 Pass 2 (PDF theme mini-previews)
A3 Pass 3 (pro-tip micro-details)        ← parallel
A6 (success moment)                      ← parallel
  ↓
[Wait for Part B: Inter + Space Grotesk TTFs]
  ↓
A0 font wiring update (Type.kt)
  ↓
A1 Pass 2 (Lottie A/B evaluation)
  ↓
A7 (reduce-motion + accessibility)
  ↓
A8 (polish + commit)
```

<!-- SECTION:PARTA:DONE -->
---

## PART B — Asset Work (separate agent executes)

> **Context for the Part B agent — read this first:**
> This is a Jetpack Compose Android app.
> - Windows path: `C:\Users\adnan\Downloads\Play2PDF redefined\`
> - Git Bash path: `/c/Users/adnan/Downloads/Play2PDF redefined/`
> - Package: `com.adnanfoisal.play2pdf`
> - Branch: `main`, remote: `origin`
> - Build: Gradle + `gradle/libs.versions.toml` version catalog
> - All Android assets go in `app/src/main/res/`
> - Android resource filenames MUST be lowercase, underscores only, no hyphens/spaces
> - After each task: `git add <files>`, `git commit -m "<message>"`, `git push origin main`
> - Do NOT touch Kotlin source in `app/src/main/java/` except where explicitly noted (AppIcons.kt). Part A owns the code.

### B1 — Fonts (HIGHEST PRIORITY — blocks Part A font wiring)

Download from Google Fonts and place in `app/src/main/res/font/`:

| Filename | Source | Weight |
|----------|--------|--------|
| `space_grotesk_medium.ttf` | Space Grotesk | 500 |
| `space_grotesk_semibold.ttf` | Space Grotesk | 600 |
| `space_grotesk_bold.ttf` | Space Grotesk | 700 |
| `inter_regular.ttf` | Inter | 400 |
| `inter_medium.ttf` | Inter | 500 |
| `inter_semibold.ttf` | Inter | 600 |
| `inter_bold.ttf` | Inter | 700 |

Both families are free/OFL-licensed on fonts.google.com. Keep the existing `space_grotesk.ttf` and `dm_sans.ttf` for now (Part A removes DM Sans references once Inter is wired).

Commit: `feat(fonts): add Space Grotesk + Inter weight files`

### B2 — Custom icon set (parallel with B1)

Source **Phosphor Icons** (MIT, 2px stroke) "Regular" weight from https://phosphoricons.com/. Convert each SVG → Android VectorDrawable XML (Android Studio SVG import, or `svg2vectordrawable` CLI). Place in `app/src/main/res/drawable/` named `ic_<slot>.xml`.

Icons needed (26 — the `AppIcons.kt` slots): playlist, topic, book, compile(bolt), history(clock), settings(gear), search, filter, bell, pdf, more, delete, download, open_external, key, wifi, cloud, user, close, check, error, plus, play, sparkle, inbox, arrow_forward, arrow_back.

Then update `core/designsystem/icons/AppIcons.kt`: replace each `Icons.Filled.*` placeholder with `ImageVector.vectorResource(R.drawable.ic_<slot>)`. Keep the icon shape recognizable — a student must instantly parse what each does.

Commit: `feat(icons): custom 2px-stroke Phosphor icon set`

### B3 — Onboarding illustrations (parallel)

3 illustrations, **abstract-geometric style** (shapes + gradients, no characters — matches the premium tool aesthetic, ages well, no cultural/character baggage). Dark or transparent background, violet→cyan accent arc to match the app.

- `onboarding_1` — "Add your YouTube playlists": stylized stacked video cards / playlist motif
- `onboarding_2` — "AI extracts the key topics": abstract neural/sparkle burst analyzing a shape
- `onboarding_3` — "Get a beautiful PDF study guide": a glowing document with lines crystallizing (echo the splash "crystallize" motif for cohesion)

Deliver as VectorDrawable XML (preferred) in `app/src/main/res/drawable/`: `onboarding_1.xml`, `onboarding_2.xml`, `onboarding_3.xml`. If raster, PNG at 960×720px in `drawable-xxxhdpi/`.

Commit: `feat(assets): onboarding illustrations`

### B4 — Sound effects (parallel, ~4 short clips)

Source royalty-free UI sounds (freesound.org CC0, or similar). Short, subtle, premium — no cartoonish beeps. Place in `app/src/main/res/raw/`:

| Filename | Sound | When |
|----------|-------|------|
| `sfx_compile_start.mp3` | soft whoosh, ~400ms | compile begins |
| `sfx_step_done.mp3` | low tap, ~150ms | each step completes |
| `sfx_success.mp3` | gentle rising 3-note chime, ~800ms | PDF ready |
| `sfx_error.mp3` | soft descending tone, ~500ms | compile fails |

Keep files small (<50KB each). Part A wires these into `SoundManager`.

Commit: `feat(sound): UI sound effects`

### B5 — App icon (Speechify-energy, lower priority)

Reference: `mock assests/app icon.jpeg` + the "makes you want to install immediately" Speechify feel. Design an adaptive icon: bold, simple, high-contrast — a play/document mark on a rich violet→indigo gradient. Deliver:
- `app/src/main/res/drawable/ic_launcher_foreground.xml` (VectorDrawable, the mark)
- `app/src/main/res/drawable/ic_launcher_background.xml` (the gradient)
- Verify `mipmap-anydpi-v26/ic_launcher.xml` references both

The mark must be legible at 48dp on a home screen. Bold silhouette, minimal detail.

Commit: `feat(icon): premium adaptive launcher icon`

### Part B order
```
B1 (fonts) ← blocks Part A font wiring, do first
B2, B3, B4 ← all parallel
B5 (app icon) ← last
```

<!-- SECTION:PARTB:DONE -->
---

## PART C — Verification checklist (run after A8 + B5)

### C1 — Build
- [ ] `./gradlew assembleDebug` — zero errors, zero warnings
- [ ] `./gradlew lintDebug` — zero new lint issues introduced by this PR

### C2 — Visual QA (Pixel 7 emulator API 34 + Pixel 4a emulator API 29)
- [ ] Splash: "Ingest & Crystallize" sequence plays end-to-end, no jank, navigates at ~2400ms
- [ ] CompileScreen: all 4 dialogs replaced by bottom sheets; PDF theme mini-previews render correctly
- [ ] CompilingScreen: atmospheric bg visible, ring aura rotates, step animations stagger in
- [ ] HistoryScreen: card gradient overlays visible, accent bar shimmer plays, search expands inline
- [ ] SettingsScreen: atmospheric bg, section headers in Brand color, PDF theme row present
- [ ] OnboardingScreen: active dot expands, page parallax-fade works, illustrations display
- [ ] Success moment: confetti + PDF icon pop + haptic fires on `CompilingPhase.Success`
- [ ] Nav bar gradient visible on HistoryScreen

### C3 — Reduce-motion
- [ ] Enable "Remove animations" in developer options → all `rememberInfiniteTransition` loops replaced by static states, no crashes

### C4 — Accessibility
- [ ] TalkBack: every interactive element announces a meaningful `contentDescription`
- [ ] All touch targets ≥ 48dp (verify with Layout Inspector)
- [ ] `pressScaleClickable` on every tappable card/button

### C5 — Fonts (after B1)
- [ ] Space Grotesk Medium/SemiBold/Bold render in headings (not fallback sans-serif)
- [ ] Inter Regular/Medium renders in body text
- [ ] No `FontFamily` created inside a `@Composable` (verify with profiler — zero allocations per recomposition for font families)

### C6 — Icons (after B2)
- [ ] All 26 `AppIcons` slots show custom Phosphor icons, not Material fallbacks
- [ ] Icons legible at 24dp and 20dp sizes

### C7 — Sound (after B4)
- [ ] `sfx_compile_start` plays on compile begin (when sound enabled)
- [ ] `sfx_step_done` plays on each step completion
- [ ] `sfx_success` plays on PDF ready
- [ ] `sfx_error` plays on compile failure
- [ ] No sound plays when system sound is muted or sound setting is off
