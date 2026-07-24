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
    // Primary brand — violet family, locked from mockups
    val Brand = Color(0xFFa78bfa)         // violet, primary accent (--purple)
    val BrandStrong = Color(0xFF8b5cf6)   // violet-glow, pressed/active (--purple-strong)
    val BrandDeep = Color(0xFF7c3aed)     // gradient start (--grad stop 0)
    val BrandMid = Color(0xFF6d5cf0)      // gradient middle (--grad stop 45%)
    val BrandGradEnd = Color(0xFF3b82f6)  // gradient end, blue (--grad stop 100%)

    // Brand gradient stops (used by PrimaryButton + neonGlow + Compile button)
    val BrandGradientStart = BrandDeep
    val BrandGradientEnd = BrandGradEnd

    // Surfaces — dark palette locked from mockups (home bg / cards / rows)
    val Surface0 = Color(0xFF0a0a12)      // app background (--bg, home)
    val Surface1 = Color(0xFF14141e)      // cards (--card)
    val Surface2 = Color(0xFF1b1b27)      // card rows (--card-row)
    val Surface3 = Color(0xFF121829)      // history cards (--card-bg history)
    val SurfaceBorder = Color(0x0FFFFFFF) // rgba(255,255,255,.06) hairlines

    // Text — locked from mockups
    val TextPrimary = Color(0xFFF4f4f7)   // --text / --txt
    val TextSecondary = Color(0xFF8a8a99) // --muted / --txt-2
    val TextTertiary = Color(0xFF6f6f80)  // --muted-2 / --txt-3
    val TextQuaternary = Color(0xFF646d84)// --txt-4 (history dates)

    // Status / accent colors — locked from mockups
    val Gold = Color(0xFFf5b942)          // crown button (--gold)
    val YtRed = Color(0xFFff1a1a)         // YouTube icon (--yt)
    val Green = Color(0xFF22c55e)         // done step (--green)
    val GreenDeep = Color(0xFF16a34a)     // done step gradient end
    val Amber = Color(0xFFfbbf24)         // pro-tip card (--amber)
    val Cyan = Color(0xFF22d3ee)          // progress ring comet / sparkline end
    val Fuchsia = Color(0xFFc026d3)       // progress ring gradient start

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
