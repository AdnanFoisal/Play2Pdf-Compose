# Play2PDF — Implementation Plan (Roles Divided v3.0)

> **This is the master plan that splits all work into two clear
> scopes:** what the **Code Agent** (me) builds vs what the **Design
> Agent** (a separate AI / designer) produces. Every line item in the
> app has a single owner, a single handoff protocol, and a single
> acceptance criterion.

**Document version:** 3.0 (Roles Divided)
**Last updated:** 2026-07-24
**Supersedes:** `PREMIUM_DESIGN_PLAN.md` v2.0 (kept as technical reference appendix)
**Tech stack:** Kotlin 2.0+ / Jetpack Compose 1.7+ / Hilt / Room / Retrofit / Coil / Lottie / Rive
**Backend:** FastAPI + Gemini 3.6 Flash + YouTube Data API v3 (unchanged, reused as-is)

---

## Table of Contents

0. [Roles & Responsibilities Matrix](#0-roles--responsibilities-matrix)
1. [Part 1 — Code Agent Scope (What I Build)](#1-part-1--code-agent-scope-what-i-build)
2. [Part 2 — Design Agent Scope (What the Other Agent Produces)](#2-part-2--design-agent-scope-what-the-other-agent-produces)
3. [Dependencies & Handoff Protocol](#3-dependencies--handoff-protocol)
4. [Implementation Timeline (Parallel Work Streams)](#4-implementation-timeline-parallel-work-streams)
5. [Quality Checklist (Joint)](#5-quality-checklist-joint)
6. [Appendix A — Reference to v2.0 Technical Detail](#6-appendix-a--reference-to-v20-technical-detail)

---

## 0. Roles & Responsibilities Matrix

Two agents work in parallel. Each work item has exactly ONE owner.

### 0.1 Ownership principles

1. **Code structure = Code Agent.** Anything that lives in a `.kt`
   file is mine — composables, ViewModels, repositories, DI modules,
   network clients, database entities, animations logic, haptics
   wiring, sound wiring.
2. **Visual assets = Design Agent.** Anything that lives in `res/`
   as a binary file or vector XML — icons, illustrations, splash
   animation, PDF theme previews, sound effect WAVs, Lottie JSONs,
   font files.
3. **Brand decisions = Design Agent.** Picking the brand color,
   picking the type pairing, designing the logo, picking illustration
   style. Code Agent uses whatever values the Design Agent locks,
   with placeholders from the v2.0 recommendations until then.
4. **Universal design principles = Code Agent.** 8-pt grid, 3-step
   radius scale, 4.5:1 contrast ratios — these are non-negotiable
   best practices, not creative decisions. Code Agent enforces them
   in tokens.
5. **Handoff protocol = both.** Design Agent delivers assets to a
   fixed path in the repo. Code Agent writes code that imports from
   those fixed paths. If the path or filename changes, both agents
   update.

### 0.2 Ownership matrix

| Work item | Owner | Deliverable format | Path |
|-----------|-------|--------------------|------|
| Brand color choice | Design Agent | Hex string in brand spec doc | — |
| Brand color implementation | Code Agent | `Color.kt` constant | `app/src/main/java/.../theme/Color.kt` |
| Type pairing choice | Design Agent | Font family names + weights | — |
| Font files (TTF/OTF) | Design Agent | `.ttf` or `.otf` files | `app/src/main/res/font/` |
| Typography implementation | Code Agent | `Type.kt` TextStyle definitions | `app/src/main/java/.../theme/Type.kt` |
| Logo mark design | Design Agent | SVG source + PNG exports | `design/logo/` + `app/src/main/res/drawable/` |
| Wordmark design | Design Agent | SVG source | `design/logo/` + `app/src/main/res/drawable/` |
| Adaptive icon (foreground) | Design Agent | VectorDrawable XML | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| Adaptive icon (background) | Design Agent | VectorDrawable XML | `app/src/main/res/drawable/ic_launcher_background.xml` |
| Adaptive icon (monochrome) | Design Agent | VectorDrawable XML | `app/src/main/res/drawable/ic_launcher_monochrome.xml` |
| Adaptive icon (mipmap XML) | Code Agent | `<adaptive-icon>` XML | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` |
| Splash icon (native) | Design Agent | VectorDrawable XML | `app/src/main/res/drawable/splash_icon.xml` |
| Splash screen composable | Code Agent | `SplashScreen.kt` | `app/src/main/java/.../ui/splash/` |
| Splash animation (Rive) | Design Agent | `.riv` file | `app/src/main/res/raw/splash_logo.riv` |
| Splash animation wiring | Code Agent | `RiveAnimation` composable call | inside `SplashScreen.kt` |
| Onboarding illustrations (3) | Design Agent | VectorDrawable XML or PNG | `app/src/main/res/drawable/onboarding_*.xml` |
| Onboarding screen composable | Code Agent | `OnboardingScreen.kt` | `app/src/main/java/.../ui/onboarding/` |
| Custom icon set (24 icons) | Design Agent | SVG source files | `design/icons/*.svg` |
| Custom icon set (Kotlin ImageVector) | Code Agent | `Icon*.kt` files | `app/src/main/java/.../core/designsystem/components/icons/` |
| Empty-state illustrations (3) | Design Agent | VectorDrawable XML | `app/src/main/res/drawable/empty_*.xml` |
| Empty-state composables | Code Agent | `EmptyState.kt` | `app/src/main/java/.../components/` |
| PDF theme preview thumbnails (13) | Design Agent | PNG files | `app/src/main/res/drawable-nodpi/pdf_theme_*.png` |
| Theme picker composable | Code Agent | `ThemePreviewCard.kt` | `app/src/main/java/.../ui/settings/components/` |
| Sound effects (6 WAV) | Design Agent | WAV files | `app/src/main/res/raw/sfx_*.wav` |
| `SoundManager.kt` | Code Agent | Kotlin class | `app/src/main/java/.../core/sound/SoundManager.kt` |
| Success confetti Lottie | Design Agent | `.json` file | `app/src/main/res/raw/success_confetti.json` |
| Confetti composable wiring | Code Agent | `SuccessConfetti.kt` | `app/src/main/java/.../components/` |
| Color tokens (`Color.kt`) | Code Agent | Kotlin file | `app/src/main/java/.../theme/Color.kt` |
| Type tokens (`Type.kt`) | Code Agent | Kotlin file | `app/src/main/java/.../theme/Type.kt` |
| Shape tokens (`Shape.kt`) | Code Agent | Kotlin file | `app/src/main/java/.../theme/Shape.kt` |
| Spacing tokens (`Spacing.kt`) | Code Agent | Kotlin file | `app/src/main/java/.../tokens/Spacing.kt` |
| Motion tokens (`Motion.kt`) | Code Agent | Kotlin file | `app/src/main/java/.../tokens/Motion.kt` |
| Theme entry (`Theme.kt`) | Code Agent | Kotlin file | `app/src/main/java/.../theme/Theme.kt` |
| `PrimaryButton` composable | Code Agent | `Button.kt` | `app/src/main/java/.../components/` |
| `PremiumCard` composable | Code Agent | `Card.kt` | `app/src/main/java/.../components/` |
| `GlassCard` composable | Code Agent | `Card.kt` | `app/src/main/java/.../components/` |
| `AnimatedChip` composable | Code Agent | `Chip.kt` | `app/src/main/java/.../components/` |
| Custom bottom nav composable | Code Agent | `NavBar.kt` | `app/src/main/java/.../components/` |
| `ShimmerSkeleton` composable | Code Agent | `Skeleton.kt` | `app/src/main/java/.../components/` |
| `Modifier.neonGlow` extension | Code Agent | `Modifier.neonGlow.kt` | `app/src/main/java/.../effects/` |
| `Modifier.pressScale` extension | Code Agent | `Modifier.pressScale.kt` | `app/src/main/java/.../effects/` |
| Retrofit API client | Code Agent | `Play2PdfApi.kt` + DTOs | `app/src/main/java/.../data/api/` |
| Room database | Code Agent | `Play2PdfDatabase.kt` + DAOs + entities | `app/src/main/java/.../data/db/` |
| DataStore preferences | Code Agent | `SettingsRepository.kt` | `app/src/main/java/.../data/prefs/` |
| Repositories | Code Agent | `*Repository.kt` | `app/src/main/java/.../data/repository/` |
| Hilt DI modules | Code Agent | `*Module.kt` | `app/src/main/java/.../di/` |
| Domain models (dataclasses) | Code Agent | `*.kt` | `app/src/main/java/.../domain/model/` |
| Use cases | Code Agent | `*UseCase.kt` | `app/src/main/java/.../domain/usecase/` |
| ViewModels | Code Agent | `*ViewModel.kt` | `app/src/main/java/.../ui/*/` |
| Screen composables (all 6) | Code Agent | `*Screen.kt` | `app/src/main/java/.../ui/*/` |
| Navigation host | Code Agent | `Play2PdfNavHost.kt` | `app/src/main/java/.../ui/navigation/` |
| `HapticsManager.kt` | Code Agent | Kotlin class | `app/src/main/java/.../core/haptics/` |
| Edge-to-edge + system bars | Code Agent | `Theme.kt` SideEffect | `app/src/main/java/.../theme/Theme.kt` |
| Predictive back gesture | Code Agent | `AndroidManifest.xml` + `BackHandler` | manifest + composables |
| SplashScreen API (Android 12+) | Code Agent | themes.xml + `MainActivity.kt` | res + java |
| Backend (FastAPI) maintenance | Code Agent | `server.py` + `requirements.txt` | `backend/` |
| Backend deployment (HF Space) | Code Agent | Dockerfile + space config | `backend/Dockerfile` |
| `AndroidManifest.xml` | Code Agent | XML | `app/src/main/AndroidManifest.xml` |
| `build.gradle.kts` (root + app) | Code Agent | Kotlin DSL | root + `app/` |
| `libs.versions.toml` (version catalog) | Code Agent | TOML | `gradle/` |
| ProGuard rules | Code Agent | `proguard-rules.pro` | `app/` |
| Final Figma mockups (all screens) | Design Agent | Figma file | shared via Figma link |
| Brand spec document | Design Agent | Markdown | `design/BRAND_SPEC.md` |

### 0.3 Summary counts

- **Code Agent deliverables:** ~60 Kotlin/XML/Gradle files
- **Design Agent deliverables:** ~50 asset files (vectors, PNGs, WAVs, Rive, Lottie, fonts)
- **Joint deliverables:** 1 brand spec doc, 1 final QA pass

---

## 1. Part 1 — Code Agent Scope (What I Build)

Everything that ends in `.kt`, `.xml` (excluding drawable vectors), `.kts`,
`.toml`, `.py`, or `Dockerfile` is mine.

### Phase A — Project Foundation (1 day)

**Goal:** Empty Android Studio project that compiles and runs a "Hello
World" Compose screen on a device.

**Tasks:**
- [ ] Create new Android Studio project with Empty Compose Activity template.
- [ ] Set package name `com.adnanfoisal.play2pdf`, min SDK 26, target SDK 35.
- [ ] Configure `gradle/libs.versions.toml` version catalog with all
      dependencies (Compose, Hilt, Room, Retrofit, Moshi, OkHttp, Coil,
      Navigation Compose, Lottie, Rive, DataStore).
- [ ] Configure root `build.gradle.kts` with plugins.
- [ ] Configure `app/build.gradle.kts` with all dependencies, Kotlin
      compiler args, Compose compiler version, Java 17 target.
- [ ] Set up `settings.gradle.kts` with `versionCatalogs` block.
- [ ] Configure `gradle.properties` (`android.useAndroidX=true`,
      `kotlin.code.style=official`, `org.gradle.jvmargs=-Xmx2048m`).
- [ ] Create directory structure from §4 of v2.0 plan.
- [ ] Set up `AndroidManifest.xml` with INTERNET permission,
      `android:name=".Play2PdfApp"`, edge-to-edge flag, adaptive icon
      reference (will be added by Design Agent).
- [ ] Create `Play2PdfApp.kt` with `@HiltAndroidApp` annotation.
- [ ] Create `MainActivity.kt` with `@AndroidEntryPoint`, `setContent {
      Play2PdfTheme { Play2PdfNavHost() } }`.
- [ ] Set up `themes.xml` with Material 3 base theme (dark, no action bar).
- [ ] Verify build passes and app launches on emulator.

**Acceptance criteria:**
- `./gradlew assembleDebug` succeeds.
- App installs and launches showing a blank dark screen.
- `git log` shows clean commit history.

**Blocks:** All other Code Agent phases. Cannot start Phase B until this is done.

---

### Phase B — Design System Implementation (2-3 days)

**Goal:** All design tokens + all reusable composables exist in code.
App should look correct with placeholder assets.

**Tasks:**
- [ ] **`Color.kt`** — brand palette (using Design Agent's locked colors
      if delivered; using v2.0 placeholder values `#7C5CFF` etc. if not).
- [ ] **`Type.kt`** — typography roles (using Design Agent's fonts if
      delivered; using fallback `FontFamily.Default` if not).
- [ ] **`Shape.kt`** — 3-step radius scale (8 / 12 / 20 / 999 dp).
- [ ] **`Spacing.kt`** — 8-pt grid (0 / 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 dp).
- [ ] **`Motion.kt`** — easing curves (`FastOutSlowInEasing`,
      `LinearOutSlowInEasing`, `FastOutLinearInEasing`) + duration constants
      (150 / 300 / 500 ms) + spec helpers.
- [ ] **`Elevation.kt`** — layered shadow tokens (Card, CardHover, Modal).
- [ ] **`Theme.kt`** — Compose theme entry with `darkColorScheme`, edge-to-edge
      SideEffect, system bar tinting.
- [ ] **`Button.kt`** — `PrimaryButton` (gradient bg, press scale 0.97,
      haptic on tap, loading state with spinner, ghost variant). Full
      Kotlin source in v2.0 §9.1.
- [ ] **`Card.kt`** — `PremiumCard` (layered shadows, hover lift, press
      depth, ripple) + `GlassCard` (real `Modifier.blur`). Full source
      in v2.0 §9.2 + §9.3.
- [ ] **`TextField.kt`** — `PremiumTextField` (filled style, floating
      label, animated border, error state, supporting text).
- [ ] **`Chip.kt`** — `AnimatedChip` (spring add, spring remove, brand
      tint, close button). Full source in v2.0 §9.4.
- [ ] **`Icon.kt`** — `AppIcon` composable that wraps `Icon()` + `tint`.
      Custom `ImageVector` definitions live in `icons/` subpackage —
      generated by Code Agent from Design Agent's SVG source (see
      §3 Handoff Protocol for the SVG → VectorDrawable → ImageVector
      pipeline).
- [ ] **`NavBar.kt`** — `Play2PdfBottomBar` (custom indicator, slide
      animation, brand tint on active). Full source in v2.0 §9.5.
- [ ] **`Skeleton.kt`** — `ShimmerSkeleton` (infinite transition gradient).
      Full source in v2.0 §9.6.
- [ ] **`EmptyState.kt`** — empty-state composable that takes an icon
      (or VectorDrawable reference), title, subtitle, and optional CTA.
- [ ] **`Modifier.neonGlow.kt`** — extension using `drawBehind` with
      3-layer radial gradient. Full source in v2.0 §7.2.
- [ ] **`Modifier.pressScale.kt`** — extension using
      `collectIsPressedAsState` + `animateFloatAsState`. Full source
      in v2.0 §11.1.
- [ ] **`Modifier.glassBlur.kt`** — extension wrapping `Modifier.blur`
      with fallback for Android < 12 via `RenderEffect`.
- [ ] **`Modifier.haptic.kt`** — extension that triggers haptic on
      tap, taking a `HapticFeedbackType` parameter.

**Acceptance criteria:**
- All composables have `@Preview` annotations showing them in isolation.
- All tokens are used (no unused tokens, no hardcoded values in composables).
- All animations use the curves from `Motion.kt`, never the default `tween(300)`.
- App builds and a "design system showcase" screen renders every component.

**Blocks:** Phase D (Screens) needs all of Phase B done.

---

### Phase C — Data Layer (2 days)

**Goal:** App can talk to the FastAPI backend, persist data locally,
and inject repositories via Hilt.

**Tasks:**
- [ ] **Domain models** — `data class Playlist`, `Topic`, `PdfHistory`,
      `PdfTheme` (enum with 13 values), `ConnectionStatus` (enum),
      `CompileStep` (enum), `UserSettings`.
- [ ] **Retrofit API client** — `Play2PdfApi.kt` interface with 4 endpoints:
      `GET /ping`, `POST /extract_topics`, `POST /playlist_meta`,
      `POST /generate_guide`. Moshi converters for request/response DTOs.
- [ ] **OkHttp interceptor** — logging interceptor (debug only), timeout
      configuration (30s ping, 60s default, 300s generate).
- [ ] **DTOs** — `GenerateGuideRequest`, `GenerateGuideResponse`,
      `ExtractTopicsRequest`, `ExtractTopicsResponse`,
      `PlaylistMetaRequest`, `PlaylistMetaResponse`, `PingResponse`.
- [ ] **Room database** — `Play2PdfDatabase.kt` with `HistoryEntity`
      and `SettingsEntity`. DAOs: `HistoryDao` (insert, getAll, delete,
      search), `SettingsDao` (get/set key-value pairs).
- [ ] **DataStore preferences** — `SettingsRepository.kt` wrapping
      DataStore for API keys (yt, gemini), backend URL, user name,
      onboarding complete flag, selected theme, sound/haptics toggles.
- [ ] **Repositories** — `CompileRepository` (calls API, returns PDF
      bytes, saves to cache), `HistoryRepository` (CRUD on Room +
      DataStore cache), `ConnectionRepository` (ping, status flow).
- [ ] **Hilt modules** — `AppModule` (provides Retrofit, OkHttp,
      Moshi, Room, DataStore), `RepositoryModule` (binds repository
      implementations to interfaces), `UseCaseModule` (binds use cases).
- [ ] **Use cases** — `CompileGuideUseCase`, `TestConnectionUseCase`,
      `ExtractTopicsUseCase`, `FetchPlaylistMetaUseCase`,
      `SavePdfToDownloadsUseCase` (uses `MediaStore` API for Android 10+).

**Acceptance criteria:**
- All network calls work against `https://adnanfoisal-play2pdf.hf.space`.
- API keys persist across app restarts (DataStore).
- History persists across app restarts (Room).
- Hilt graph compiles with no missing bindings.
- Unit tests for each repository (mock API, in-memory Room, fake DataStore).

**Blocks:** Phase D (Screens) needs repositories for ViewModels.

---

### Phase D — UI Implementation (5-7 days)

**Goal:** All 6 screens exist, are wired to ViewModels, navigate between
each other, and use placeholder assets where Design Agent's work is
pending.

**Tasks:**
- [ ] **`Play2PdfNavHost.kt`** — Navigation Compose host with 5 routes
      (`splash`, `onboarding`, `compile`, `history`, `settings`,
      `compiling`). Page transition animations per v2.0 §11.2.
- [ ] **`SplashScreen.kt` + `SplashViewModel.kt`** — uses Design Agent's
      Rive animation from `R.raw.splash_logo` IF delivered; uses a
      `ProgressRing` placeholder otherwise. Cross-fades wordmark in.
      Auto-navigates to onboarding or compile after 2.5s based on
      onboarding-complete flag.
- [ ] **`OnboardingScreen.kt`** — 3-page `HorizontalPager`. Uses Design
      Agent's illustrations (`R.drawable.onboarding_1/2/3`) IF delivered;
      uses `AppIcons.Sparkle` placeholder otherwise. Skip button,
      page indicator, Get Started button on page 3.
- [ ] **`CompileScreen.kt` + `CompileViewModel.kt`** — sticky app bar,
      header banner, playlist input card (with neon "+" button),
      topic chips card, featured playlists row, book details card,
      sticky bottom Compile button. All sections per v2.0 §10.3.
- [ ] **`HistoryScreen.kt` + `HistoryViewModel.kt`** — animated search
      field, filter chips (All / Week / Month / Subject), list header
      with sort icon, `LazyColumn` of `SwipeToDismissHistoryItem`s,
      empty state. Per v2.0 §10.4.
- [ ] **`SettingsScreen.kt` + `SettingsViewModel.kt`** — 4 section
      headers, inline auto-save (debounced 500ms), per-field API key
      test buttons, live backend status indicator, PDF theme grid
      with full cover previews. Per v2.0 §10.5.
- [ ] **`CompilingScreen.kt` + `CompilingViewModel.kt`** — branded
      loader (Rive or `ProgressRing` placeholder), conversational
      step checklist, cancel with confirmation, success state with
      confetti + PDF preview + Open/Save buttons, error state with
      error in code box + Try Again/Copy. Per v2.0 §10.6.
- [ ] **`MainScreen.kt`** — `Scaffold` with `Play2PdfBottomBar` and
      `NavHost` for the 3 main tabs (Compile/History/Settings).
- [ ] **Screen-specific composables** — `PlaylistInputCard.kt`,
      `TopicChipsCard.kt`, `FeaturedPlaylistsRow.kt`,
      `HistoryListItem.kt`, `SwipeToDismissHistoryItem.kt`,
      `ThemePreviewCard.kt`, `ConnectionStatusIndicator.kt`,
      `ValidationIndicator.kt`, `SettingsSectionHeader.kt`,
      `SearchField.kt`, `FilterChip` row, `CompileTopBar.kt`,
      `HeaderBanner.kt`, `EmptyHistoryState.kt`, `EmptyPlaylistsHint.kt`.

**Acceptance criteria:**
- All 6 screens build and navigate.
- All ViewModels are Hilt-injected.
- State flows are properly collected via `collectAsStateWithLifecycle`.
- All user interactions trigger the correct ViewModel method.
- Empty states render with placeholders until Design Agent delivers.
- All long text has `maxLines` + `Ellipsis`.

**Blocks:** Phase E (micro-interactions) needs screens to wire into.

---

### Phase E — Micro-interactions & Motion (2 days)

**Goal:** Every interactive element feels alive. Press depth, springs,
page transitions, scroll-linked animations, success celebration.

**Tasks:**
- [ ] Apply `Modifier.pressScale()` to every interactive composable
      (buttons, cards, chips, list items).
- [ ] Wire `AnimatedVisibility` + `spring` on chip add/remove in
      `TopicChipsCard` and `PlaylistManager`.
- [ ] Wire Navigation Compose `slideIntoContainer`/`slideOutOfContainer`
      on every route transition.
- [ ] Wire `ModalBottomSheet` open animation on theme picker + filter
      sheet.
- [ ] Implement scroll-linked parallax on `HeaderBanner` (scale 1.0 →
      0.92 + alpha 1.0 → 0.6 as user scrolls first 200px).
- [ ] Implement `SuccessConfetti` composable using Design Agent's
      `R.raw.success_confetti` Lottie IF delivered; otherwise a custom
      `Canvas`-based particle burst (20 particles, physics fall, brand
      color).
- [ ] Implement empty-state delight — subtle infinite `AnimatedVisibility`
      pulse on the sparkle in empty-state illustrations.
- [ ] Implement long-press context menu on `HistoryListItem` (opens a
      `ModalBottomSheet` with Open / Save / Rename / Share / Delete).
- [ ] Implement pull-to-refresh on `HistoryScreen` (custom indicator
      that rotates the logo mark as the user pulls).
- [ ] Implement error shake on failed form submissions (x offset 0 →
      -8 → 8 → -4 → 4 → 0 over 400ms).

**Acceptance criteria:**
- Every button visibly depresses on tap.
- Every page transition slides + fades.
- Every chip add/remove springs.
- Every modal opens with a spring.
- Success state plays confetti + haptic.
- All animations use specified curves (no `tween(300)` defaults).

**Blocks:** Phase F (sound/haptics wiring) layers on top of these.

---

### Phase F — Sound & Haptics Wiring (1 day)

**Goal:** Every interaction has a haptic. Every primary action has a
sound (if sounds are enabled in Settings).

**Tasks:**
- [ ] Implement `HapticsManager.kt` per v2.0 §12.1 — `light()`,
      `medium()`, `heavy()`, `success()`, `error()` methods, using
      `VibratorManager` on Android 12+ and `Vibrator` on older.
- [ ] Implement `SoundManager.kt` per v2.0 §12.2 — `SoundPool` with
      6 sound effects loaded from `R.raw.sfx_*` (Design Agent's WAV
      files IF delivered; silent no-ops otherwise).
- [ ] Add `SettingsRepository.getSoundEnabled()` /
      `getHapticsEnabled()` flows.
- [ ] Wire haptics into every interaction per v2.0 §12.3 table:
      - Light tap → `light()` on every button press.
      - Medium tap → `medium()` on primary action (Compile, Save).
      - Success → `success()` on PDF compiled.
      - Error → `error()` on compilation failed.
      - Chip add → `light()` → 30ms gap → `medium()`.
      - Chip remove → `medium()` → 30ms gap → `light()`.
      - Page nav → `light()`.
      - Modal open → `medium()`, close → `light()`.
      - Pull-to-refresh trigger → `medium()`.
- [ ] Wire sounds into the same interactions at 30% volume (gated by
      Settings toggle).
- [ ] Add Settings UI toggles: "Sound effects" + "Haptic feedback"
      with live test (tapping the toggle plays the relevant feedback).

**Acceptance criteria:**
- Every button press produces a haptic.
- Every Compile produces `success()` haptic + `sfx_success.wav`.
- Settings toggles work and persist.
- Haptics respect the system "Vibrate on tap" setting.
- Sounds respect the system media volume.

**Blocks:** Phase G (polish & QA).

---

### Phase G — Polish & QA (2 days)

**Goal:** App is production-ready. 60fps, accessible, native Android
features wired.

**Tasks:**
- [ ] **Performance:**
  - [ ] Run Macrobenchmark on cold start + scroll. Target < 2s cold
        start, 60fps scroll.
  - [ ] Profile with Perfetto. Fix any jank hotspots.
  - [ ] Add `derivedStateOf` to expensive state derivations.
  - [ ] Add `key()` to all `LazyColumn` / `LazyRow` items.
  - [ ] Verify `Composable` stability (no unnecessary recompositions).
- [ ] **Accessibility:**
  - [ ] Run TalkBack through every screen. Fix navigation order.
  - [ ] Verify WCAG AA contrast on every text/background pair.
  - [ ] Verify every interactive element is ≥ 48×48dp.
  - [ ] Add `contentDescription` to every meaningful icon (null on
        decorative ones).
  - [ ] Add `Modifier.semantics` where appropriate.
  - [ ] Respect `Settings.Global.ANIMATOR_DURATION_SCALE` for reduced
        motion.
- [ ] **Native Android:**
  - [ ] Edge-to-edge layout (`WindowCompat.setDecorFitsSystemWindows
        (window, false)`).
  - [ ] System bars tinted to match app background.
  - [ ] Predictive back gesture (Android 14+) — `BackHandler` +
        predictive animation.
  - [ ] Themed icons (Android 13+) — `ic_launcher_monochrome.xml`
        delivered by Design Agent.
  - [ ] Per-app language preferences (Android 13+).
  - [ ] SplashScreen API (Android 12+) — themes.xml + `MainActivity`.
  - [ ] Deep link support — `<intent-filter>` for `play2pdf://`
        scheme.
  - [ ] Notification channel for "PDF ready" notifications.
- [ ] **Real device testing:**
  - [ ] Test on small phone (e.g. Pixel 4a / 5.8").
  - [ ] Test on large phone (e.g. Pixel 7 Pro / 6.7").
  - [ ] Test on tablet (e.g. Pixel Tablet / 10.95").
  - [ ] Test on slow network (Android emulator 3G throttle).
  - [ ] Test on low-battery mode.
  - [ ] Test with dark mode + light mode (light mode supported if
        Design Agent delivered a light palette).
- [ ] **Build variants:**
  - [ ] `debug` build with verbose logging.
  - [ ] `release` build with ProGuard / R8 minification.
  - [ ] Sign release build with upload key.
- [ ] **App Store prep:**
  - [ ] Generate signed APK + AAB.
  - [ ] Write Play Store listing (title, short desc, long desc,
        screenshots — screenshots use Design Agent's final mockups).

**Acceptance criteria:**
- App passes Macrobenchmark with no red flags.
- App passes TalkBack end-to-end.
- App runs on all 3 device sizes without layout breakage.
- Release AAB builds and installs on a real device.
- All §5 Quality Checklist items pass.

**Blocks:** Nothing. This is the final phase before release.

---

### Phase H — Backend Maintenance (ongoing)

**Goal:** Keep the FastAPI backend healthy. Add features as needed.

**Tasks:**
- [ ] Copy `backend/` from old repo into `Play2Pdf-Compose/backend/`.
- [ ] Verify backend still uses `gemini-3.6-flash` (latest GA, confirmed
      via web search July 21, 2026).
- [ ] Add `/health` endpoint for uptime monitoring.
- [ ] Add request rate limiting (slowapi or similar) to prevent abuse.
- [ ] Add structured logging (JSON to stdout) for HF Space logs.
- [ ] Configure CORS to allow the Android app's package origin.
- [ ] Add `/api/v1/` prefix to all endpoints for future versioning.
- [ ] Update Dockerfile to use Python 3.12 + multi-stage build for
      smaller image.
- [ ] Add GitHub Action to redeploy on push to `main`.
- [ ] Document backend setup in `backend/README.md`.

**Acceptance criteria:**
- Backend stays live at `https://adnanfoisal-play2pdf.hf.space`.
- `/ping` returns 200 in < 1s when awake.
- Cold-start time < 60s (HF Space free tier).
- No unhandled exceptions in production logs.

**Blocks:** Nothing. Ongoing.

---

## 2. Part 2 — Design Agent Scope (What the Other Agent Produces)

Everything that lives in `res/drawable/`, `res/raw/`, `res/font/`, or
`design/` is the Design Agent's. The Code Agent will not produce any
of these files. The Code Agent will write code that imports them by
resource ID.

### Asset A — Brand Foundation

**Deliverable:** `design/BRAND_SPEC.md` — a 1-page brand spec doc.

**Contents:**
- Brand color hex (recommendation: `#7C5CFF` — but Design Agent's
  choice is final).
- Brand color dark variant (for pressed states).
- Type pairing (recommendation: Geist + Geist Mono + Fraunces — but
  Design Agent's choice is final).
- Type role table (10 roles: Display, Title 1-3, Body, Body small,
  Caption, Micro/Label, Code, Stat number — with size/weight/line-
  height/letter-spacing per role).
- Logo concept statement (1 sentence describing what the logo
  communicates).
- Illustration style (geometric line art? soft gradients? flat? —
  pick one and commit).

**Acceptance criteria:**
- 1 Markdown file, ≤ 2 pages.
- Every value is concrete (no "TBD", no "maybe").
- Code Agent can implement `Color.kt` and `Type.kt` directly from this
  doc without further questions.

**Blocks:** Phase B (Code Agent's design system implementation).

---

### Asset B — Logo & Wordmark

**Deliverable:** Logo mark + wordmark as SVG + PNG exports.

**Logo mark spec:**
- Concept: "playlist → document" — three stacked horizontal bars of
  decreasing width + diagonal fold crease + page corner fold.
- Style: single weight, single color, 2px stroke, no fills.
- Sharp corners on the bars (video thumbnails).
- Rounded corners on the page (PDF output).
- Geometric, not illustrative.

**Readability tests (must pass all 7):**
- ✅ Readable at 16×16.
- ✅ Readable at 32×32.
- ✅ Readable at 48×48.
- ✅ Single-color white on dark.
- ✅ Single-color black on light.
- ✅ Silhouette (alpha only).
- ✅ Recognizable when blurred to 4px.

**Files:**
- `design/logo/logo_mark.svg` — 1024×1024 viewBox, single-color brand.
- `design/logo/logo_wordmark.svg` — mark + "Play2PDF" wordmark side-by-side,
  with the "2" in brand color.
- `design/logo/logo_mark_stacked.svg` — mark above wordmark.
- `design/logo/exports/logo_mark_white_16.png` through `logo_mark_white_1024.png` (7 sizes × 3 colors = 21 PNGs).

**Acceptance criteria:**
- All 7 readability tests pass.
- SVGs open cleanly in Android Studio's Vector Asset importer.
- PNG exports are real PNGs (verify with `file` — must say "PNG image
  data, ... RGBA", NOT "JPEG image data").

**Blocks:** Phase B (welcome screen + splash reference Design Agent's
logo). Asset C (adaptive icon) is derived from this.

---

### Asset C — Android Adaptive Icon

**Deliverable:** 3 VectorDrawable XML files for the Android adaptive
icon system.

**Specs:**
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — VectorDrawable,
  logo mark in brand color, 108×108dp viewport (inner 72dp safe zone,
  outer 18dp per side masked by launcher).
- `app/src/main/res/drawable/ic_launcher_background.xml` — VectorDrawable,
  solid `#09090B` OR radial gradient from `#18181B` to `#09090B`.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — VectorDrawable,
  single-color white silhouette of the logo (for Android 13+ themed
  icons that respect the user's wallpaper tint).

**Acceptance criteria:**
- All 3 files open in Android Studio's Vector Asset previewer without
  errors.
- Foreground's content is centered in the inner 72dp safe zone (test
  by setting the launcher mask to circle, squircle, rounded square,
  full square — the logo should be visible in all).
- Monochrome is pure white with alpha (no colors) — Android applies
  the user's wallpaper tint at runtime.

**Blocks:** Phase A (AndroidManifest references the adaptive icon). Code
Agent will write `mipmap-anydpi-v26/ic_launcher.xml` referencing these
3 drawables.

---

### Asset D — Splash Icon (Native Android Splash)

**Deliverable:** 1 VectorDrawable XML.

**Spec:**
- `app/src/main/res/drawable/splash_icon.xml` — VectorDrawable, logo
  mark in brand color, 120×120dp. Used by Android 12+ SplashScreen
  API before Compose boots.

**Acceptance criteria:**
- Opens cleanly in Android Studio.
- Looks identical to `ic_launcher_foreground.xml` (same logo, just
  different viewport/size for splash use).

**Blocks:** Phase A (SplashScreen API setup).

---

### Asset E — In-App Logo

**Deliverable:** 2 VectorDrawable XMLs.

**Specs:**
- `app/src/main/res/drawable/logo_mark.xml` — same as
  `ic_launcher_foreground.xml` but sized for in-app use (96×96dp).
- `app/src/main/res/drawable/logo_wordmark.xml` — mark + "Play2PDF"
  text, with the "2" in brand color. Sized for use in splash and
  welcome screens.

**Acceptance criteria:**
- Both render correctly at 96dp, 120dp, 160dp.
- The wordmark's "2" is the exact brand color from Asset A.

**Blocks:** Phase B (used in splash + onboarding).

---

### Asset F — Custom Icon Set (24 Icons)

**Deliverable:** 24 SVG files in `design/icons/`.

**List:**
1. `playlist.svg` — 3 stacked bars, decreasing width.
2. `topic.svg` — bookmark with center dot.
3. `book.svg` — open book, 2 lines per page.
4. `compile.svg` — document + corner sparkle.
5. `history.svg` — clock at 10:10.
6. `settings.svg` — 6-tooth gear.
7. `search.svg` — magnifier, 2px stroke.
8. `filter.svg` — 3 lines, decreasing width.
9. `bell.svg` — bell + indicator dot.
10. `pdf.svg` — document + corner fold.
11. `more.svg` — 3 vertical dots.
12. `delete.svg` — trash can.
13. `download.svg` — down arrow + tray.
14. `open_external.svg` — box + up-right arrow.
15. `key.svg` — key.
16. `wifi.svg` — wifi arcs.
17. `cloud.svg` — cloud.
18. `user.svg` — person silhouette.
19. `close.svg` — X, rounded caps.
20. `check.svg` — checkmark.
21. `error.svg` — X in circle.
22. `plus.svg` — + rounded caps.
23. `play.svg` — triangle play.
24. `sparkle.svg` — 4-point star.

**Specs:**
- All 24×24 viewBox.
- All 2px stroke, rounded caps, no fills.
- All single color (currentColor, so they tint at runtime).
- Consistent visual weight across the set (test: arrange all 24 in a
  grid — they should look like a family, not 24 unrelated icons).

**Acceptance criteria:**
- All 24 SVGs open cleanly in Android Studio's Vector Asset importer
  (which converts them to VectorDrawable XML).
- Visual weight is consistent.
- Stroke width is consistent (2px everywhere).
- All icons pass the "blurred to 4px" readability test.

**Blocks:** Phase B (Code Agent converts SVG → VectorDrawable → Kotlin
ImageVector; see §3 Handoff Protocol).

---

### Asset G — Empty-State Illustrations (3)

**Deliverable:** 3 VectorDrawable XMLs.

**Style:** Geometric line art with one accent color (the brand color).
4px stroke. Transparent or Surface 1 background.

**List:**
1. `app/src/main/res/drawable/empty_history.xml` — 240×180dp. Concept:
   stack of 3 PDF documents fanning out, top one has a small brand-
   colored sparkle in the corner.
2. `app/src/main/res/drawable/empty_playlists.xml` — 240×180dp.
   Concept: YouTube play button inside a circle, dashed arrow pointing
   to a stack of pages.
3. `app/src/main/res/drawable/empty_topics.xml` — 240×180dp. Concept:
   chalkboard with 3 empty bullet points, small chalk-dust cloud in
   corner.

**Acceptance criteria:**
- All 3 render at 240×180dp and below (down to 120×90dp for compact
  layouts).
- The brand color accent is visible but not dominant.
- Style matches Asset B's illustration style declaration.

**Blocks:** Phase D (empty states in History + Compile screens).

---

### Asset H — Splash Animation (Rive)

**Deliverable:** 1 Rive file.

**Spec:**
- `app/src/main/res/raw/splash_logo.riv` — Rive animation, 2 seconds,
  60fps.
- **Sequence:**
  1. 0.0-1.2s: Logo mark draws itself in via stroke-dashoffset
     animation (each of the 3 bars + the diagonal + the page corner
     draws in sequence, 200ms each).
  2. 1.2-1.6s: Single pulse of brand glow radiates outward (radius
     0 → 80px → 0, opacity 0 → 0.6 → 0).
  3. 1.6-2.0s: Logo settles (slight scale 1.0 → 1.02 → 1.0).

**Acceptance criteria:**
- File size ≤ 30KB.
- Plays correctly with `app.cash.rive:rive-android` library.
- Animation completes in exactly 2 seconds.
- Logo at end state matches Asset B's logo mark.

**Fallback (if Rive is too complex):**
- `app/src/main/res/raw/splash_logo.json` — Lottie JSON exported from
  After Effects, same sequence, ≤ 100KB.
- Uses `com.airbnb.android:lottie-compose` library instead.

**Blocks:** Phase D (SplashScreen composable).

---

### Asset I — Onboarding Illustrations (3)

**Deliverable:** 3 VectorDrawable XMLs or PNGs.

**Style:** Same as Asset G (geometric line art + brand accent), but
larger (240×180dp display size, exported at 3x = 720×540 source).

**List:**
1. `app/src/main/res/drawable/onboarding_1.xml` — Concept: YouTube
   play button on left, arrow pointing right, stack of PDF pages on
   right. Headline: "Drop a playlist. Get a study guide."
2. `app/src/main/res/drawable/onboarding_2.xml` — Concept: brain-
   shaped cloud with lines connecting to video thumbnails and topic
   labels. Headline: "AI maps every topic to the right video."
3. `app/src/main/res/drawable/onboarding_3.xml` — Concept: single PDF
   page with accent bar, QR code, and checkbox grid visible. Headline:
   "Print-ready. LaTeX-grade. Yours."

**Acceptance criteria:**
- All 3 render at 240×180dp without clipping.
- Style is consistent across the set.
- Style matches Asset G's empty-state illustrations.

**Blocks:** Phase D (OnboardingScreen).

---

### Asset J — PDF Theme Preview Thumbnails (13)

**Deliverable:** 13 PNG files.

**List:**
- `pdf_theme_tufte_scholar.png`
- `pdf_theme_princeton_math.png`
- `pdf_theme_midnight_terminal.png`
- `pdf_theme_cambridge_emerald.png`
- `pdf_theme_bauhaus_geometric.png`
- `pdf_theme_swiss_stark.png`
- `pdf_theme_oxford_burgundy.png`
- `pdf_theme_deep_space.png`
- `pdf_theme_mit_tech.png`
- `pdf_theme_wharton_ledger.png`
- `pdf_theme_sumi_ink.png`
- `pdf_theme_renaissance_gold.png`
- `pdf_theme_warm_sunset_dark.png`

**Spec:**
- All 220×280px, PNG with alpha (or no alpha if background is opaque).
- Each shows the actual cover page of a PDF generated with that theme,
  using placeholder subject "Data Structures & Algorithms" and author
  "Student / Creator".
- File size ≤ 50KB each.

**How to generate (Design Agent can do this OR Code Agent can do it
as a one-off script):**
1. Run each theme through the existing FastAPI backend's
   `/generate_guide` endpoint once.
2. Use `pypdfium2` to render the first page of each PDF to PNG at 2x
   scale.
3. Save to `app/src/main/res/drawable-nodpi/`.

**Acceptance criteria:**
- All 13 PNGs are real PNGs (verify with `file` — must say "PNG image
  data").
- Each thumbnail visually matches the theme it represents (e.g.
  midnight_terminal.png is dark with green text, cambridge_emerald.png
  is dark green with gold accent, etc.).
- All 13 are the same size (220×280px).

**Blocks:** Phase D (ThemePreviewCard in SettingsScreen).

---

### Asset K — Sound Effects (6 WAV Files)

**Deliverable:** 6 WAV files.

**List:**
1. `app/src/main/res/raw/sfx_tap.wav` — subtle low-pass filtered click.
2. `app/src/main/res/raw/sfx_chip_add.wav` — rising chirp.
3. `app/src/main/res/raw/sfx_chip_remove.wav` — falling chirp.
4. `app/src/main/res/raw/sfx_success.wav` — single soft chime.
5. `app/src/main/res/raw/sfx_error.wav` — low buzz at ~80Hz.
6. `app/src/main/res/raw/sfx_nav.wav` — very subtle wood-tap.

**Specs:**
- All ≤ 50ms duration.
- All ≤ 5KB file size.
- All mono (not stereo).
- All 44.1kHz, 16-bit WAV.

**Acceptance criteria:**
- All 6 files play correctly via `SoundPool`.
- All 6 sound "premium" — no clipping, no harsh frequencies, no
  recognizable stock-sound-library artifacts.
- Set is cohesive (all 6 sound like they came from the same product).

**Blocks:** Phase F (SoundManager wiring). If not delivered, Code
Agent leaves `SoundManager` fully implemented but with silent no-ops
for each sound — app still works, just silent.

---

### Asset L — Success Confetti Lottie

**Deliverable:** 1 Lottie JSON file.

**Spec:**
- `app/src/main/res/raw/success_confetti.json` — Lottie animation, 1
  second, 60fps.
- 20 small brand-colored particles (mix of brand color, brand dark,
  and white) burst from center, physics-based fall (gravity + slight
  horizontal drift), fade out at end.

**Acceptance criteria:**
- File size ≤ 50KB.
- Plays correctly via `com.airbnb.android:lottie-compose`.
- Particles are visible against the dark Surface0 background.

**Blocks:** Phase E (SuccessConfetti composable). If not delivered,
Code Agent uses a custom Canvas-based particle burst as fallback.

---

### Asset M — Font Files

**Deliverable:** 8 TTF/OTF font files.

**List (per Asset A's type pairing decision — assuming Geist + Geist
Mono + Fraunces):**
- `app/src/main/res/font/geist_regular.ttf`
- `app/src/main/res/font/geist_medium.ttf`
- `app/src/main/res/font/geist_semibold.ttf`
- `app/src/main/res/font/geist_bold.ttf`
- `app/src/main/res/font/geist_mono_regular.ttf`
- `app/src/main/res/font/geist_mono_medium.ttf`
- `app/src/main/res/font/geist_mono_bold.ttf`
- `app/src/main/res/font/fraunces_regular.ttf`
- `app/src/main/res/font/fraunces_semibold.ttf`

**Specs:**
- All TTF or OTF (no WOFF — Android doesn't support WOFF).
- All licensed for app embedding (Geist is SIL OFL, Fraunces is SIL
  OFL — both free for commercial use).

**Acceptance criteria:**
- All files load correctly via `FontFamily(Font(R.font.*))`.
- Weights render correctly (Regular / Medium / SemiBold / Bold are
  visually distinguishable).
- File names match the resource IDs Code Agent expects in `Type.kt`.

**Blocks:** Phase B (Type.kt implementation). If not delivered, Code
Agent uses `FontFamily.Default` (Roboto) as fallback.

---

### Asset N — Final Figma Mockups

**Deliverable:** 1 Figma file with all 6 screens designed at production
fidelity.

**Screens:**
1. Splash (showing Rive animation's final frame).
2. Onboarding (3 pages, each with the final illustration).
3. Compile (with all sections, real content, real icons).
4. History (with 3 sample items + empty state).
5. Settings (with all 4 sections, real data).
6. Compiling (showing all 3 states: in-progress, success, error).

**Acceptance criteria:**
- All screens use the locked brand color and type pairing from Asset A.
- All icons are from Asset F's custom set.
- All illustrations are from Assets G + I.
- All PDF theme previews are from Asset J.
- Mockups are pixel-perfect — Code Agent should be able to match them
  1:1 in Compose.

**Blocks:** Phase G (final QA — Code Agent compares implemented
screens against the Figma mockups).

---

## 3. Dependencies & Handoff Protocol

### 3.1 Dependency graph

```
Asset A (Brand Spec)
  ├── blocks → Phase B (Design System Implementation)
  ├── blocks → Asset B (Logo Design — needs brand color)
  ├── blocks → Asset C (Adaptive Icon — needs logo)
  ├── blocks → Asset D (Splash Icon — needs logo)
  ├── blocks → Asset E (In-App Logo — needs logo)
  ├── blocks → Asset F (Custom Icons — needs illustration style)
  ├── blocks → Asset G (Empty States — needs illustration style)
  ├── blocks → Asset I (Onboarding Illustrations — needs illustration style)
  └── blocks → Asset M (Fonts — needs type pairing decision)

Asset B (Logo)
  ├── blocks → Asset C (Adaptive Icon foreground)
  ├── blocks → Asset D (Splash Icon)
  ├── blocks → Asset E (In-App Logo)
  └── blocks → Asset H (Splash Rive — animates the logo)

Phase A (Project Foundation)
  ├── blocks → Phase B
  ├── blocks → Phase C
  └── references → Asset C, Asset D (manifest references)

Phase B (Design System Implementation)
  ├── needs → Asset A (brand color + type pairing)
  ├── needs → Asset F (custom icons → ImageVector)
  ├── needs → Asset M (font files)
  └── blocks → Phase D (Screens need design system)

Phase C (Data Layer)
  └── blocks → Phase D (Screens need repositories)

Phase D (UI Implementation)
  ├── needs → Phase B + Phase C
  ├── needs → Asset E (in-app logo for splash + onboarding)
  ├── needs → Asset G (empty states for History + Compile)
  ├── needs → Asset H (Rive splash animation)
  ├── needs → Asset I (onboarding illustrations)
  └── needs → Asset J (PDF theme previews for Settings)

Phase E (Micro-interactions)
  ├── needs → Phase D
  └── needs → Asset L (Success Confetti Lottie)

Phase F (Sound & Haptics Wiring)
  ├── needs → Phase E
  └── needs → Asset K (Sound effects)

Phase G (Polish & QA)
  ├── needs → Phase F
  └── needs → Asset N (Final Figma mockups for comparison)
```

### 3.2 Placeholder policy

While waiting for Design Agent assets, Code Agent uses these
placeholders so development is never blocked:

| Asset missing | Code Agent placeholder |
|---------------|------------------------|
| Asset A (brand spec) | Hardcode `#7C5CFF` brand color, Geist type pairing. Mark with `// TODO: replace with Design Agent's locked values`. |
| Asset B (logo) | Use `AppIcons.Sparkle` (Asset F's sparkle icon) at 96dp inside a brand-tinted rounded square. |
| Asset C (adaptive icon) | Use a single-color VectorDrawable with "P2P" text. App will install but with placeholder launcher icon. |
| Asset D (splash icon) | Same as Asset B placeholder. |
| Asset E (in-app logo) | Same as Asset B placeholder. |
| Asset F (custom icons) | Use Material Icons (`Icons.R.PlayArrow`, etc.) with `// TODO: replace with AppIcons.*` comments. |
| Asset G (empty states) | Use `AppIcons.Inbox` (Material fallback) at 96dp inside a Surface2 rounded square. |
| Asset H (splash Rive) | Use `CircularProgressIndicator` with brand color. Less premium but unblocks development. |
| Asset I (onboarding illustrations) | Use `AppIcons.Sparkle` (large) at 240dp with brand tint. |
| Asset J (PDF theme previews) | Use `AppIcons.Pdf` at 56dp inside a brand-tinted rounded rectangle. |
| Asset K (sound effects) | `SoundManager.play()` is a no-op (returns immediately). App is silent but functional. |
| Asset L (success confetti) | Custom `Canvas`-based particle burst in Code Agent (60 lines of Kotlin). Less polished than Lottie but works. |
| Asset M (font files) | Use `FontFamily.Default` (Roboto). App works but loses Geist's character. |
| Asset N (Figma mockups) | Code Agent ships without pixel-perfect comparison. QA is "looks good enough" instead of "matches mockup 1:1". |

### 3.3 Handoff protocol

When Design Agent delivers an asset:

1. **Design Agent** pushes the file(s) to the agreed path in the repo
   (per the path column in §0.2 Ownership Matrix).
2. **Design Agent** opens a PR titled `design: deliver asset X` with
   a checklist of acceptance criteria from §2.
3. **Code Agent** reviews the PR:
   - Verifies file format (real PNG, real VectorDrawable XML, etc.).
   - Verifies dimensions and file size constraints.
   - Verifies the asset renders correctly (open in Android Studio
     preview).
4. **Code Agent** merges the PR.
5. **Code Agent** removes the corresponding placeholder code (per
   §3.2) and replaces it with a reference to the delivered asset.
6. **Code Agent** commits with message `refactor: replace placeholder
   for asset X with delivered asset`.

### 3.4 Asset delivery order (recommended)

To unblock Code Agent as fast as possible, Design Agent should deliver
in this order:

1. **Asset A** (Brand Spec) — 1 day. Unblocks Phase B.
2. **Asset B** (Logo) — 2 days. Unblocks Assets C, D, E, H.
3. **Asset M** (Fonts) — 1 day (download from Geist GitHub). Unblocks
   Phase B fully.
4. **Asset C** (Adaptive Icon) — 1 day. Unblocks Phase A's manifest
   reference.
5. **Asset F** (Custom Icons) — 3 days. Unblocks Phase B's `Icon.kt`.
6. **Asset H** (Splash Rive) — 2 days. Unblocks Phase D's splash.
7. **Asset I** (Onboarding Illustrations) — 2 days. Unblocks Phase D's
   onboarding.
8. **Asset G** (Empty States) — 2 days. Unblocks Phase D's empty states.
9. **Asset J** (PDF Theme Previews) — 1 day. Unblocks Phase D's Settings.
10. **Asset D + E** (Splash + In-App Icons) — 1 day. Polish.
11. **Asset K** (Sound Effects) — 2 days. Unblocks Phase F.
12. **Asset L** (Success Confetti) — 1 day. Unblocks Phase E.
13. **Asset N** (Final Figma Mockups) — 3 days. Unblocks Phase G QA.

**Total Design Agent time:** ~15-20 days of focused work, parallel to
Code Agent's 18-25 days.

---

## 4. Implementation Timeline (Parallel Work Streams)

Two parallel tracks. Design Agent starts first (Asset A blocks the
most), Code Agent starts Phase A immediately (no design dependencies).

```
Day  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25
    ├─────────────────────────────────────────────────────────────────────────┤
DESIGN  ████████████████████████████████████████
    Asset A (Brand Spec)      ██████
    Asset B (Logo)                  ████████
    Asset M (Fonts)                 ████
    Asset C (Adaptive Icon)             ██████
    Asset F (Custom Icons)                  ████████████
    Asset H (Splash Rive)                        ████████
    Asset I (Onboarding)                              ████████
    Asset G (Empty States)                                    ████████
    Asset J (PDF Previews)                                        ██████
    Asset D + E (Splash + In-App)                                     ██████
    Asset K (Sounds)                                                      ████████
    Asset L (Confetti)                                                        ████
    Asset N (Figma Mockups)                                              ████████████
    ├─────────────────────────────────────────────────────────────────────────┤
CODE    ████████████████████████████████████████████████████████████████
    Phase A (Foundation)  ████
    Phase B (Design Sys)       ████████████
    Phase C (Data Layer)           ████████
    Phase D (UI)                           ██████████████████
    Phase E (Micro-Interactions)                          ████████
    Phase F (Sound/Haptics)                                    ██████
    Phase G (Polish & QA)                                          ████████
    Phase H (Backend)                                             ░░░░░░░░░░░░ (ongoing)
    ├─────────────────────────────────────────────────────────────────────────┤
```

### Critical path

The longest dependency chain is:

```
Asset A (1d) → Asset B (2d) → Asset F (3d) → Phase B (3d) → Phase D (7d) → Phase E (2d) → Phase F (1d) → Phase G (2d) = 21 days
```

So the project ships in **~21 days of focused work**, assuming both
agents work in parallel and Design Agent delivers in the recommended
order.

If Design Agent is unavailable, Code Agent can ship a "developer
preview" build in ~15 days using all placeholders from §3.2. It will
look amateur but be functionally complete.

---

## 5. Quality Checklist (Joint)

Before shipping, BOTH agents verify their respective items.

### 5.1 Code Agent checklist

**Visual:**
- [ ] No default Material Icons — all icons are custom `ImageVector`
      from Asset F.
- [ ] No hardcoded colors — all from `MaterialTheme.colorScheme` or
      `Color.kt` constants.
- [ ] No hardcoded sizes — all from `Spacing.*` or `Radius.*`.
- [ ] No `Color.Black` / `Color.White` literals.
- [ ] All uppercase labels have 8% letter-spacing.
- [ ] All long text has `maxLines` + `overflow = TextOverflow.Ellipsis`.

**Motion:**
- [ ] Every interactive element has press scale (0.97, 100ms).
- [ ] Every page transition uses specified curve (not default).
- [ ] Every modal open/close has spring.
- [ ] Every list item add/remove has spring.
- [ ] No `Thread.sleep` in animations — always `delay()`.
- [ ] No layout thrashing during animations.

**Performance:**
- [ ] 60fps scroll on a mid-range device (Macrobench).
- [ ] Sub-200ms tap response.
- [ ] Splash exits under 3 seconds.
- [ ] App cold-start under 2 seconds.
- [ ] No "white flash" between splash and first screen.
- [ ] `derivedStateOf` on expensive state derivations.
- [ ] `key()` on all `LazyColumn` / `LazyRow` items.

**Accessibility:**
- [ ] WCAG AA contrast on every text/background pair.
- [ ] Every interactive element ≥ 48×48dp.
- [ ] Every meaningful icon has `contentDescription`.
- [ ] TalkBack navigates every screen logically.
- [ ] Reduced motion respected.

**Native Android:**
- [ ] Edge-to-edge layout.
- [ ] System bars tinted to match app background.
- [ ] Predictive back gesture (Android 14+).
- [ ] Themed icons (Android 13+).
- [ ] Per-app language preferences (Android 13+).
- [ ] Adaptive icon for all launcher shapes.
- [ ] SplashScreen API (Android 12+).
- [ ] Deep link support.
- [ ] Notification channel for "PDF ready".

**Backend:**
- [ ] `/ping` returns 200 in < 1s when awake.
- [ ] Cold-start time < 60s.
- [ ] No unhandled exceptions in production logs.
- [ ] CORS allows the Android app's package origin.
- [ ] Uses `gemini-3.6-flash` (latest GA, verified).

### 5.2 Design Agent checklist

**Brand:**
- [ ] Asset A delivered and locked.
- [ ] No "TBD" or "maybe" in brand spec.

**Logo:**
- [ ] Asset B passes all 7 readability tests.
- [ ] Logo looks identical across splash, onboarding, app icon, About.

**Icons:**
- [ ] All 24 icons in Asset F open cleanly in Android Studio Vector
      Asset importer.
- [ ] Visual weight is consistent across the set.
- [ ] Stroke width is 2px everywhere.
- [ ] All icons pass the "blurred to 4px" readability test.

**Illustrations:**
- [ ] All 3 empty states (Asset G) match the illustration style declared
      in Asset A.
- [ ] All 3 onboarding illustrations (Asset I) match the same style.
- [ ] Style is consistent across the set.

**Adaptive Icon:**
- [ ] Foreground content centered in inner 72dp safe zone.
- [ ] Logo visible in all 4 launcher mask shapes (circle, squircle,
      rounded square, full square).
- [ ] Monochrome is pure white with alpha.

**Animations:**
- [ ] Asset H (splash Rive) plays correctly and matches Asset B's logo.
- [ ] Asset L (confetti Lottie) plays correctly and particles are
      visible against Surface0.

**Sounds:**
- [ ] All 6 sounds in Asset K are ≤ 50ms and ≤ 5KB.
- [ ] All 6 are mono, 44.1kHz, 16-bit WAV.
- [ ] Set is cohesive (sounds like one product).

**Fonts:**
- [ ] All 9 font files in Asset M load correctly via `FontFamily(Font
      (R.font.*))`.
- [ ] Weights are visually distinguishable.
- [ ] License allows app embedding.

**Final:**
- [ ] Asset N (Figma mockups) covers all 6 screens at production
      fidelity.
- [ ] Mockups use the locked brand color and type pairing from Asset A.
- [ ] Mockups use icons from Asset F, illustrations from Assets G + I,
      PDF previews from Asset J.

---

## 6. Appendix A — Reference to v2.0 Technical Detail

The following sections from `PREMIUM_DESIGN_PLAN.md` v2.0 remain the
authoritative technical reference. Code Agent implements per these
specs; Design Agent produces assets per these specs.

| v2.0 Section | What it covers | Used by |
|--------------|----------------|--------|
| §1 | Why rewrite in Kotlin + Jetpack Compose | Context only |
| §2 | Brutal truth audit of old Flet app | Context only |
| §3 | What "premium" means (7 principles) | Context only |
| §4 | Project architecture (full directory tree + tech stack table) | Code Agent Phase A |
| §5.1-5.7 | Brand identity (color, logo spec, typography, spacing, radius) | Design Agent Assets A + B + M |
| §6.1-6.8 | Asset inventory (adaptive icon, splash icon, in-app logo, custom icons, empty states, splash animation, sound effects, PDF previews) | Design Agent Assets C-L |
| §7.1 | Real backdrop blur (`Modifier.blur` + `GlassCard` source) | Code Agent Phase B |
| §7.2 | Real neon glow (`Modifier.neonGlow` source) | Code Agent Phase B |
| §7.3 | Micro-typography in Compose | Code Agent Phase B |
| §7.4 | Motion principles (Compose animation APIs table) | Code Agent Phase E |
| §8.1-8.6 | Design system in Compose (full Color.kt, Theme.kt, Type.kt, Spacing.kt, Shape.kt, Motion.kt source) | Code Agent Phase B |
| §9.1-9.6 | Component polish (PrimaryButton, PremiumCard, GlassCard, AnimatedChip, BottomBar, ShimmerSkeleton — full Kotlin source) | Code Agent Phase B |
| §10.1-10.6 | Screen-by-screen redesign (splash, onboarding, compile, history, settings, compiling) | Code Agent Phase D |
| §11.1-11.4 | Micro-interactions (pressScale, page transitions, scroll parallax, confetti — full Kotlin source) | Code Agent Phase E |
| §12.1-12.3 | Sound & haptics (HapticsManager, SoundManager source, haptic patterns table) | Code Agent Phase F |
| §14.1-14.6 | Asset generation toolkit (Figma, Rive, LottieFiles, Phosphor, etc.) | Design Agent (all assets) |

When in doubt, the v2.0 plan has the technical detail. This v3.0 plan
has the ownership and handoff protocol.

---

## Final note

This plan exists to make sure nothing falls through the cracks. Every
line item has one owner, one acceptance criterion, and one handoff
protocol.

**Code Agent's job:** ship a 60fps, accessible, native-feeling Kotlin +
Jetpack Compose app that talks to the existing FastAPI backend. Use
placeholders for any missing Design Agent asset so development is
never blocked.

**Design Agent's job:** produce a real brand, real logo, real custom
icons, real illustrations, real animations, real sounds. Without
these, the app is functionally complete but visually amateur.

Both agents work in parallel. Both agents ship to the same repo. Both
agents respect the ownership matrix in §0.2.

The bar is not "good enough for an Android app". The bar is "would
Linear / Arc / Things 3 / Craft ship this on iOS?" If the answer is
no, keep working.

When in doubt: **fewer things, done better.**

— End of document —
