package com.adnanfoisal.play2pdf.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette + dark surface palette for Play2PDF.
 *
 * Values are locked from the HTML mockups in `mock assests/` (per
 * IMPLEMENTATION_PLAN.md §Design Tokens). The brand moved from the v2.0
 * placeholder `#7C5CFF` to the mockup-locked violet `#a78bfa` /
 * `#8b5cf6` family, with a blue gradient end `#3b82f6` used by the
 * Compile button and progress ring.
 *
 * All color references in composables MUST go through [MaterialTheme.colorScheme]
 * (which is built from these constants in [Play2PdfTheme]) or through these
 * top-level constants. No `Color.Black`, `Color.White`, or hex literals
 * anywhere else.
 */
object BrandColors {
    // Primary brand — Spotify Green family
    val Brand = Color(0xFF1DB954)         // Spotify Green, primary accent
    val BrandStrong = Color(0xFF1ED760)   // Spotify hover/bright green
    val BrandDeep = Color(0xFF12873B)     // darker green gradient start
    val BrandMid = Color(0xFF18A048)      // gradient middle
    val BrandGradEnd = Color(0xFF00D1FF)  // vibrant cyan for gradient ends (playful contrast)

    // Brand gradient stops
    val BrandGradientStart = BrandDeep
    val BrandGradientEnd = BrandGradEnd

    // Surfaces — slightly cooler/deeper dark mode to make green pop
    val Surface0 = Color(0xFF080C0A)      // app background
    val Surface1 = Color(0xFF121B15)      // cards
    val Surface2 = Color(0xFF18241C)      // card rows
    val Surface3 = Color(0xFF0E1611)      // history cards
    val SurfaceBorder = Color(0x0FFFFFFF) // rgba(255,255,255,.06) hairlines
    val SurfaceBorderStrong = Color(0x1FFFFFFF) // rgba(255,255,255,.12) hover/active

    // Unified deep base + per-screen atmospheric glow stops
    val Bg = Color(0xFF060907)            // unified app base
    val GlowViolet = Color(0xFF0D2415)    // top radial atmosphere (Home, Settings) - now GlowGreen
    val GlowIndigo = Color(0xFF0A1F1E)    // bottom-right radial (History) - now GlowTeal
    val GlowDeep = Color(0xFF07120A)      // Compiling mid-stop
    val CompilingBg = Color(0xFF040705)   // Compiling darkest base
    val HistoryBg = Color(0xFF050807)     // History base

    // Text
    val TextPrimary = Color(0xFFFFFFFF)   // --text / --txt
    val TextSecondary = Color(0xFFA1A1AA) // --muted / --txt-2
    val TextTertiary = Color(0xFF71717A)  // --muted-2 / --txt-3
    val TextQuaternary = Color(0xFF52525B)// --txt-4

    // Status / accent colors
    val Gold = Color(0xFFF5B942)
    val YtRed = Color(0xFFFF0000)
    val Green = Color(0xFF1DB954)         // done step uses brand green now
    val GreenDeep = Color(0xFF12873B)
    val Amber = Color(0xFFFBBF24)
    val Cyan = Color(0xFF00D1FF)          // progress ring comet
    val Fuchsia = Color(0xFF1ED760)       // progress ring gradient start (now bright green)

    // Status (kept for compatibility with existing code)
    val Success = Green
    val SuccessBg = Color(0xFF0F2A18)
    val Error = Color(0xFFEF4444)
    val ErrorBg = Color(0xFF2A1010)
    val Warning = Amber
    val WarningBg = Color(0xFF2A1F08)
    val BrandDark = BrandStrong           // alias for pressed state (legacy refs)
    val BrandLight = Brand                // alias for hover state (legacy refs)

    // Glass overlay (used by GlassCard)
    val GlassTint = Color(0x33FFFFFF)     // 20% white
    val GlassBorder = Color(0x55FFFFFF)   // 33% white

    // Pure utility (do NOT use directly in composables — go through colorScheme)
    val PureBlack = Color(0xFF000000)
    val PureWhite = Color(0xFFFFFFFF)
}

/**
 * Per-card accent color pairs used by HistoryScreen (5 pairs, cycled).
 * Locked from `mock assests/history screen.html` card CSS vars.
 */
val HistoryCardAccents: List<Pair<Color, Color>> = listOf(
    Color(0xFFfbbf24) to Color(0xFFea580c),  // amber/orange
    Color(0xFFd946ef) to Color(0xFF7c3aed),  // fuchsia/violet
    Color(0xFF34d399) to Color(0xFF0d9488),  // teal/emerald
    Color(0xFF60a5fa) to Color(0xFF2563eb),  // blue/indigo
    Color(0xFF4ade80) to Color(0xFF15803d)   // green/emerald
)
