# Play2PDF — Master Implementation Plan
# Synthesized from: Claude audit + owner audit (myown plan.txt)
# Date: 2026-07-25
# Active repo: C:\Users\adnan\Downloads\Play2PDF redefined\ (root only)

---

## RESOLVED DECISIONS (locked before implementation begins)

| # | Decision | Resolution |
|---|----------|------------|
| 1 | Active repo | Root `Play2PDF redefined/` only. Inner copy deleted. |
| 2 | Font strategy | Weight-specific TTFs from Google Fonts (Part B delivers files; Part A wires them) |
| 3 | Manrope vs DM Sans | **Unify to DM Sans** across all screens. Manrope is a Design Agent choice that was never shipped. |
| 4 | Text color unification | Unify to Home values: `#f4f4f7` / `#8a8a99` / `#6f6f80`. Per-screen micro-variations are noise. |
| 5 | Custom icons | Material Icons stay for now. Custom SVG set is Part B. |
| 6 | Splash animation | Improve the existing Compose canvas animation (fix 3D flip, tune spring, trim hold). The Lottie JSON is evaluated as an alternative — Part A implements both and picks the better one after seeing them run. |
| 7 | API 26–30 blur fallback | Skip blur on older devices. Aura is still visible without blur. |
| 8 | Dark-only | Yes. Light mode stays as unreachable placeholder. |
| 9 | Settings/Onboarding | Design freehand in established language (no mockup exists). |
| 10 | Splash duration | Trim from 3200ms to ~2600ms. Remove 800ms dead hold. |

---

## PART A — Code Work (Claude executes)

### A0 — Repo hygiene (do first, one pass)

**Files changed:**
- `theme/Type.kt` — fix `@Composable get()` on FontFamily (CRITICAL performance bug). Move `SpaceGrotesk` and `DmSans` FontFamily construction to top-level `val` using `remember`-free static initialization. Each `Font(resId, weight)` call is cheap at top-level; the bug is creating new instances on every recomposition.
- `theme/Type.kt` — remove legacy stubs (`GeistFamily`, `GeistMonoFamily`, `FrauncesFamily`) at bottom of file.
- `theme/Color.kt` — add missing per-screen background tokens:
  - `HistoryBg = Color(0xFF05070d)` (history screen base)
  - `CompilingBg = Color(0xFF060509)` (compiling screen darkest point)
  - `SurfaceBorderStrong = Color(0x1FFFFFFF)` (rgba 255,255,255,0.12 — hover borders)
- `tokens/Spacing.kt` — add the mockup-exact values that fall outside the 8pt grid as named tokens: `Spacing.smPlus = 9.dp`, `Spacing.mdMinus = 14.dp`, `Spacing.mdPlus = 18.dp`, `Spacing.lgMinus = 20.dp`, `Spacing.lgPlus = 22.dp`.
- `theme/Shape.kt` — extend with mockup-exact radii: `AppShape.card = 18.dp`, `AppShape.row = 13.dp`, `AppShape.button = 15.dp`, `AppShape.pill22 = 22.dp`, `AppShape.ytIcon = 9.dp`.

**Needs from Part B:** Nothing.
**Passes:** 1.

---

### A1 — Font wiring (depends on Part B delivering TTF files)

**Files changed:**
- `res/font/` — rename/add 6 weight-specific files (Part B delivers them):
  - `space_grotesk_medium.ttf`, `space_grotesk_semibold.ttf`, `space_grotesk_bold.ttf`
  - `dm_sans_regular.ttf`, `dm_sans_medium.ttf`, `dm_sans_bold.ttf`
- `theme/Type.kt` — update `Font(R.font.*)` references to use the correct per-weight resource IDs.

**Needs from Part B:** The 6 TTF files placed in `app/src/main/res/font/`.
**Passes:** 1 (trivial once files exist).

---

### A2 — CompileScreen polish (1–2 passes)

**What changes:**
1. Replace all raw `dp`/`sp` literals with `Spacing.*` and `AppShape.*` tokens.
2. Replace inline `fontSize`/`fontWeight` with `AppType.*` styles.
3. **Dialog overhaul** — `PlaylistUrlDialog`, `TopicInputDialog`, `ThemePickerDialog`, `AdvancedDialog`:
   - Replace `OutlinedTextField` with `PremiumTextField`.
   - Style `AlertDialog` container: `containerColor = BrandColors.Surface1`, `tonalElevation = 0.dp`, `shape = AppShape.large`, border via `Modifier.border(1.dp, BrandColors.SurfaceBorder, AppShape.large)`.
4. Fix top padding: `Spacer(Modifier.height(14.dp))` (was 12dp — 2dp off from mockup).

**Files:** `ui/compile/CompileScreen.kt`
**Needs from Part B:** Nothing.
**Passes:** 1–2.

---

### A3 — CompilingScreen atmospheric background + missing animations (2–3 passes)

This is the screen with the most gaps. Work in this order:

**Pass 1 — Background atmosphere:**
- Replace flat `BrandColors.Surface0` background with a multi-layer `Box`:
  - Layer 0: `Color(0xFF060509)` fill (darkest base)
  - Layer 1: `Brush.radialGradient(listOf(Color(0xFF1a1730), Color.Transparent), center = Offset(width*0.5f, 0f), radius = height*0.7f)` — violet aura at top
  - Layer 2: `Brush.radialGradient(listOf(Color(0xFF0c1a2c).copy(alpha=0.5f), Color.Transparent), center = Offset(width, height), radius = height*0.6f)` — cyan hint at bottom-right
- Fix aura: replace `radialGradient` with `Brush.sweepGradient` + add `infiniteRepeatable` `rotationZ` animation (0→360, 9000ms, `LinearEasing`). Add `Modifier.blur(26.dp)` on API 31+, skip on lower (no fallback needed per decision #7).
- Add ring drop-shadow: after drawing the arc, draw two more arcs at slightly larger radius with low alpha (fuchsia 65%, cyan 30%) to simulate `drop-shadow`.
- Add percentage text glow: wrap the percentage `Text` in a `Box` with `Modifier.neonGlow(BrandColors.BrandStrong, radius = 24.dp, alpha = 0.35f)`.

**Pass 2 — Step animations:**
- Add staggered entrance to `StepRow`: same `AnimatedVisibility` + `fadeIn + slideInVertically` pattern used in `HistoryScreen`, delays 350/500/650/800ms.
- Add active step pulse ring: `rememberInfiniteTransition` animating `scale 0.85→1.5` + `alpha 0.8→0` over 1800ms on a `Canvas` circle drawn behind the active dot.
- Add header entrance: `AnimatedVisibility(visible = true, enter = fadeIn(tween(600)) + slideInVertically { -it/4 })` wrapping the header `Row`.
- Add ring popIn: `animateFloatAsState` from 0.85→1.0 on first composition, 800ms, `Emphasized` easing, applied via `Modifier.scale(ringScale)`.

**Pass 3 — Pro-tip micro-details:**
- Sheen skew: apply `graphicsLayer { rotationZ = -3f; transformOrigin = TransformOrigin(0f, 0.5f) }` to the sheen overlay `Box` to approximate `skewX(-18deg)`.
- Bulb flicker: `rememberInfiniteTransition` animating lightbulb icon alpha between 0.75→1.0→0.85→1.0 with irregular keyframe durations over 3500ms.

**Files:** `ui/compiling/CompilingScreen.kt`
**Needs from Part B:** Nothing.
**Passes:** 3.

---

### A4 — HistoryScreen atmospheric background + missing animations (2 passes)

**Pass 1 — Background + card polish:**
- Replace flat `BrandColors.Surface0` with `Color(0xFF05070d)` base + two radial gradient overlays (same multi-layer `Box` pattern as A3).
- Add card top gradient overlay: inside `HistoryCard`, add a `Box` with `Brush.verticalGradient(listOf(Color.White.copy(alpha=0.025f), Color.Transparent), endY = cardHeight * 0.6f)` overlaid on the card background.
- Add accent bar shimmer: `rememberInfiniteTransition` animating a `translationX` sweep on a white gradient overlay inside the 4dp accent bar, 3400ms linear.
- Fix nav bar background: in `Play2PdfBottomBar` (`NavBar.kt`), change background from flat `Surface1` to `Brush.verticalGradient(listOf(Color(0x00080B14), Color(0x99080B14)))`.

**Pass 2 — Card interactions:**
- Add colored glow shadow on press: use `Modifier.shadow(elevation = 16.dp, spotColor = accentTop.copy(alpha=0.45f), ambientColor = Color.Black.copy(alpha=0.6f))` when card is pressed (track with `interactionSource.collectIsPressedAsState()`).
- Add PDF icon press animation: `animateFloatAsState` for `rotation (-3f when pressed, 0f otherwise)` + `scale (1.04f when pressed)` applied via `graphicsLayer` on the `PdfSvgIcon`.
- Wire search/filter buttons: expand a `SearchBar` composable inline (animated height 0→56dp) on search tap; filter opens a `ModalBottomSheet` with sort/filter options backed by `HistoryViewModel`.

**Files:** `ui/history/HistoryScreen.kt`, `core/designsystem/components/NavBar.kt`
**Needs from Part B:** Nothing.
**Passes:** 2.

---

### A5 — SettingsScreen + OnboardingScreen polish (1 pass each)

**SettingsScreen:**
- Add atmospheric background (same multi-layer radial gradient as other screens, using `Surface0` base).
- Add subtitle under header: `"Configure your API keys and preferences"` in `AppType.bodySmall` / `TextSecondary`.
- `SectionHeader` color: change from `TextTertiary` to `Brand` to match `CompileScreen`'s `SectionLabel` pattern.
- No other structural changes needed — the screen already uses the design system correctly.

**OnboardingScreen:**
- Add atmospheric background.
- Add active dot width animation: animate active indicator dot from 8dp to 24dp width (pill shape) using `animateDpAsState`.
- Add page transition: `HorizontalPager` already handles swipe; add `graphicsLayer { alpha = lerp(0.5f, 1f, pageOffset); translationX = lerp(40f, 0f, pageOffset) }` on each page for a parallax-fade feel.
- Illustration slot: change from hardcoded `R.drawable.logo_mark` to a `painterResource(page.illustrationRes)` where each `OnboardingPage` data class carries its own drawable ID. Part B delivers the 3 illustration drawables.

**Files:** `ui/settings/SettingsScreen.kt`, `ui/onboarding/OnboardingScreen.kt`
**Needs from Part B:** 3 onboarding illustration drawables (for OnboardingScreen only; SettingsScreen needs nothing).
**Passes:** 1 each.

---

### A6 — SplashScreen animation refinement (2 passes)

**Pass 1 — Fix the 3D flip:**
- Replace the `scaleX` flip simulation with a true perspective rotation:
  ```kotlin
  Canvas(modifier = Modifier.graphicsLayer {
      rotationY = flipAngle   // 0 → 90 (play disappears) → 180 (doc appears)
      cameraDistance = 8f * density
  })
  ```
- Replace sequential `delay()` phase machine with a single `Animatable<Float>` driven by `animateTo` calls with explicit `durationMillis` — eliminates drift under load.
- Tune Phase 1 spring: change `DampingRatioMediumBouncy` to `DampingRatioNoBouncy` (smooth ease-out, no overshoot).
- Trim total duration: remove the 800ms Phase 6 hold; navigate at 2400ms instead of 3200ms.

**Pass 2 — Evaluate Lottie JSON:**
- Copy `mock assests/opening animation.json` → `app/src/main/res/raw/opening_animation.json`.
- Add a `LottieAnimation` variant of the splash alongside the Compose canvas version, gated by a `USE_LOTTIE_SPLASH = true/false` constant.
- Run both on device. Pick whichever looks better and delete the other. Document the decision in a comment.

**Files:** `ui/splash/SplashScreen.kt`, `app/src/main/res/raw/opening_animation.json` (new)
**Needs from Part B:** Nothing (Lottie 6.5.2 already in `libs.versions.toml` and `build.gradle.kts`).
**Passes:** 2.

---

### A7 — Polish pass (1 pass, after all above)

- Audit every screen for remaining raw `dp`/`sp` literals — replace with tokens.
- Verify `contentDescription` on every interactive element (accessibility).
- Verify all touch targets ≥ 48dp.
- Verify `pressScaleClickable` on every tappable element.
- Remove `// TODO` comments that are now resolved.
- Run `./gradlew assembleDebug` — zero warnings, zero errors.

**Files:** All screens + components.
**Passes:** 1.

---

### Part A — Execution Order

```
A0 (repo hygiene + token fixes)
  ↓
A2 (CompileScreen dialogs — no asset dependency)
  ↓
A3 Pass 1 (CompilingScreen background)
A4 Pass 1 (HistoryScreen background)   ← run in parallel
  ↓
A3 Pass 2 (step animations)
A4 Pass 2 (card interactions)          ← run in parallel
  ↓
A3 Pass 3 (pro-tip micro-details)
A5 (Settings + Onboarding)             ← run in parallel
  ↓
A6 Pass 1 (splash fix)
  ↓
[Wait for Part B: font TTFs]
  ↓
A1 (font wiring)
  ↓
A6 Pass 2 (Lottie evaluation)
  ↓
A7 (polish pass)
```

---

## PART B — Asset Work (separate agent executes)

> **Context for the Part B agent:**
> This is a Jetpack Compose Android app at `C:\Users\adnan\Downloads\Play2PDF redefined\` (Windows path) or `/c/Users/adnan/Downloads/Play2PDF redefined/` (Git Bash path). The repo is on the `main` branch. All asset files go into the Android resource directory at `app/src/main/res/`. After placing files, commit and push to the `main` branch remote (`origin`). The app package is `com.adnanfoisal.play2pdf`. The build system is Gradle with `libs.versions.toml`.

### B1 — Font files (HIGHEST PRIORITY — blocks Part A Phase A1)

**Task:** Download the following 6 font files from Google Fonts and place them in `app/src/main/res/font/`:

| Filename to create | Google Fonts source | Weight |
|--------------------|---------------------|--------|
| `space_grotesk_medium.ttf` | Space Grotesk, weight 500 | Medium |
| `space_grotesk_semibold.ttf` | Space Grotesk, weight 600 | SemiBold |
| `space_grotesk_bold.ttf` | Space Grotesk, weight 700 | Bold |
| `dm_sans_regular.ttf` | DM Sans, weight 400 | Regular |
| `dm_sans_medium.ttf` | DM Sans, weight 500 | Medium |
| `dm_sans_bold.ttf` | DM Sans, weight 700 | Bold |

Download URL pattern: `https://fonts.gstatic.com/s/[fontname]/v[version]/[hash].ttf` — use the Google Fonts API or download directly from fonts.google.com. The existing `res/font/space_grotesk.ttf` and `res/font/dm_sans.ttf` are single-weight variable fonts — keep them as fallbacks but add the static weight files above.

**Android resource naming rules:** filenames must be lowercase, no hyphens (use underscores), no spaces. The names above already comply.

**After placing files:** Run `git add app/src/main/res/font/` and commit with message `feat(fonts): add weight-specific Space Grotesk and DM Sans TTFs`.

---

### B2 — Custom icon set (can run in parallel with B1)

**Task:** Create or source a custom 2px-stroke icon set matching the mockup aesthetic (dark background, violet accent, clean geometric shapes). The app needs 26 icons. Each must be delivered as an Android `VectorDrawable` XML in `app/src/main/res/drawable/` OR as a Compose `ImageVector` Kotlin file in `app/src/main/java/com/adnanfoisal/play2pdf/core/designsystem/icons/`.

**Icon list** (from `AppIcons.kt` — these are the slot names that need custom assets):

| Slot name | Description | Notes |
|-----------|-------------|-------|
| `Playlist` | YouTube playlist / list of videos | YouTube red accent acceptable |
| `Topic` | Tag or label | |
| `Book` | Open book or study guide | |
| `Compile` | Lightning bolt or spark | Already uses `Icons.Filled.Bolt` — keep if acceptable |
| `History` | Clock or history | |
| `Settings` | Gear or sliders | |
| `Search` | Magnifying glass | |
| `Filter` | Funnel or sliders | |
| `Bell` | Notification bell | |
| `Pdf` | PDF document with fold corner | Already drawn via Canvas in HistoryScreen — vector version needed |
| `More` | Three dots vertical | |
| `Delete` | Trash can | |
| `Download` | Arrow down into tray | |
| `OpenExternal` | Arrow out of box | |
| `Key` | API key / lock | |
| `Wifi` | WiFi signal | |
| `Cloud` | Cloud upload | |
| `User` | Person silhouette | |
| `Close` | X mark | |
| `Check` | Checkmark | |
| `Error` | Circle with X or exclamation | |
| `Plus` | Plus sign | |
| `Play` | Triangle play button | |
| `Sparkle` | Star/sparkle | |
| `Inbox` | Inbox tray | |
| `ArrowForward` | Right arrow | |
| `ArrowBack` | Left arrow | |

**Recommended source:** Phosphor Icons (MIT license, 2px stroke, available as SVG). Download from https://phosphoricons.com/ — select the "Regular" weight (2px stroke). Convert SVGs to Android VectorDrawable XML using Android Studio's SVG import or `svg2vectordrawable` CLI tool.

**Delivery format:** Update `core/designsystem/icons/AppIcons.kt` to replace each `Icons.Filled.*` placeholder with the correct `ImageVector.vectorResource(R.drawable.ic_*)` reference, OR inline the vector path data directly as `ImageVector.Builder` calls.

**After placing files:** Commit with message `feat(icons): add custom 2px-stroke icon set`.

---

### B3 — Onboarding illustrations (can run in parallel with B1, B2)

**Task:** Create or source 3 illustrations for the onboarding carousel. Each illustration should:
- Be 240×180dp (960×720px at 4x density for `drawable-xxxhdpi`)
- Match the app's dark aesthetic: dark background (`#0a0a12` or transparent), violet/cyan accent colors
- Communicate the 3 onboarding concepts:
  1. **Page 1:** "Add your YouTube playlists" — illustration of a playlist/video list
  2. **Page 2:** "AI extracts the key topics" — illustration of AI/brain/sparkle analyzing content
  3. **Page 3:** "Get a beautiful PDF study guide" — illustration of a PDF document

**Delivery format:** PNG files in `app/src/main/res/drawable/`:
- `onboarding_1.png`
- `onboarding_2.png`
- `onboarding_3.png`

OR as VectorDrawable XMLs (preferred for resolution independence):
- `onboarding_1.xml`
- `onboarding_2.xml`
- `onboarding_3.xml`

**After placing files:** Commit with message `feat(assets): add onboarding illustrations`.

---

### B4 — App icon refinement (optional, low priority)

The current adaptive icon uses `ic_launcher_foreground.xml` (a VectorDrawable). The `mock assests/app icon.jpeg` shows the intended design: dark background, book/PDF motif, violet accent. If the current vector doesn't match, update `ic_launcher_foreground.xml` and `ic_launcher_background.xml` to match the JPEG reference.

**Files:** `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/drawable/ic_launcher_background.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

---

### Part B — Execution Order

```
B1 (fonts) ← HIGHEST PRIORITY, blocks Part A Phase A1
B2 (icons) ← parallel with B1
B3 (onboarding illustrations) ← parallel with B1, B2
B4 (app icon) ← lowest priority, do last
```

After each B task: `git add`, `git commit`, `git push origin main`.

---

## VERIFICATION CHECKLIST

After all Part A phases complete:
- [ ] `./gradlew assembleDebug` — zero errors, zero warnings
- [ ] Install on device/emulator (API 26 minimum, test on API 31+ for blur)
- [ ] SplashScreen: smooth scale-in → 3D flip → morph → glow → wordmark → navigate (~2.4s total)
- [ ] CompileScreen: bold headings visually heavier than body; dialogs match dark theme
- [ ] CompilingScreen: atmospheric background visible; aura rotates; step pulse animates
- [ ] HistoryScreen: atmospheric background; accent bar shimmer; card press glow
- [ ] SettingsScreen: consistent with other screens; section headers violet
- [ ] OnboardingScreen: active dot animates width; page parallax-fade transition
- [ ] All screens: no raw hex literals in Kotlin (grep for `Color(0x` outside Color.kt)
- [ ] All screens: no raw `dp` literals outside token files (spot-check)
- [ ] Font weights: Space Grotesk Bold headings visually distinct from DM Sans Regular body

After all Part B tasks complete:
- [ ] Font files present in `res/font/` — 6 weight-specific TTFs
- [ ] All 26 icon slots in `AppIcons.kt` point to custom assets (no `Icons.Filled.*` placeholders)
- [ ] 3 onboarding illustrations present in `res/drawable/`
- [ ] All commits pushed to `origin main`
