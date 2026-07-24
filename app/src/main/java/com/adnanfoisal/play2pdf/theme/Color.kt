package com.adnanfoisal.play2pdf.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette + dark surface palette for Play2PDF.
 *
 * The brand color (`#7C5CFF`) is the v2.0 placeholder value. When the
 * Design Agent delivers `design/BRAND_SPEC.md` (Asset A), replace these
 * constants with the locked values — every other file imports from here,
 * so the swap is one commit.
 *
 * All color references in composables MUST go through [MaterialTheme.colorScheme]
 * (which is built from these constants in [Play2PdfTheme]) or through these
 * top-level constants. No `Color.Black`, `Color.White`, or hex literals
 * anywhere else.
 *
 * TODO: replace with Design Agent's locked values when Asset A is delivered.
 */
object BrandColors {
    // Primary brand
    val Brand = Color(0xFF7C5CFF)
    val BrandDark = Color(0xFF5B3FD6)     // pressed state
    val BrandLight = Color(0xFFA78BFA)    // hover / disabled

    // Brand gradient stops (used by PrimaryButton + neonGlow)
    val BrandGradientStart = Brand
    val BrandGradientEnd = Color(0xFF5B3FD6)

    // Surfaces (dark palette — app is dark-only until Design Agent delivers a light palette)
    val Surface0 = Color(0xFF09090B)      // app background
    val Surface1 = Color(0xFF18181B)      // cards
    val Surface2 = Color(0xFF232328)      // raised cards / modals
    val Surface3 = Color(0xFF2E2E36)      // popovers / chips
    val SurfaceBorder = Color(0xFF2A2A30) // 1px hairlines

    // Text
    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextTertiary = Color(0xFF71717A)

    // Status
    val Success = Color(0xFF22C55E)
    val SuccessBg = Color(0xFF0F2A18)
    val Error = Color(0xFFEF4444)
    val ErrorBg = Color(0xFF2A1010)
    val Warning = Color(0xFFF59E0B)
    val WarningBg = Color(0xFF2A1F08)

    // Glass overlay (used by GlassCard)
    val GlassTint = Color(0x33FFFFFF)     // 20% white
    val GlassBorder = Color(0x55FFFFFF)   // 33% white

    // Pure utility (do NOT use directly in composables — go through colorScheme)
    val PureBlack = Color(0xFF000000)
    val PureWhite = Color(0xFFFFFFFF)
}
