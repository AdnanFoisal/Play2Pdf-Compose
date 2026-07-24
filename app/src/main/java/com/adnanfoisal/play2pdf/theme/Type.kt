package com.adnanfoisal.play2pdf.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Typography roles for Play2PDF.
 *
 * Type pairing (per Asset A spec): Geist Sans + Geist Mono + Fraunces.
 * Until Design Agent delivers the actual font files (Asset M), we fall
 * back to [FontFamily.Default] (Roboto on AOSP, Inter on Pixel). The
 * fallback is gated behind [GeistLoaded] so swapping in real fonts later
 * is a one-line change.
 *
 * 10 type roles (per Asset A spec):
 *  Display   — splash wordmark, hero numbers
 *  Title 1   — screen titles (large)
 *  Title 2   — section headers
 *  Title 3   — card titles
 *  Body      — default body
 *  Body Small— metadata, captions inline
 *  Caption   — helper text, footnotes
 *  Label     — uppercase chips, eyebrow text (8% letter-spacing)
 *  Code      — error stack traces, API key fields (monospace)
 *  Stat      — big numbers in History / Compiling
 *
 * Usage:
 *   Text("Hello", style = MaterialTheme.typography.titleMedium)  // ← Title 2
 *   Text("hello", style = AppType.label)                          // ← Label
 */
object AppType {

    /** Family fallback — see class kdoc. */
    private val Sans: FontFamily
        @Composable get() = if (GeistLoaded) GeistFamily else FontFamily.Default

    private val Mono: FontFamily
        @Composable get() = if (GeistLoaded) GeistMonoFamily else FontFamily.Monospace

    private val Display: FontFamily
        @Composable get() = if (GeistLoaded) FrauncesFamily else FontFamily.Serif

    val display: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.02).sp
        )

    val title1: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.01).sp
        )

    val title2: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        )

    val title3: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp
        )

    val body: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )

    val bodySmall: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

    val caption: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

    val label: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.0.sp  // ~8% letter-spacing for uppercase labels
        )

    val code: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Mono,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

    val stat: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center
        )

    /** Material 3 [Typography] instance — maps our roles to M3's slot
     *  system so Material components (e.g. TopAppBar, Card) pick up our
     *  typeface automatically. */
    val material: Typography
        @Composable get() = Typography(
            displayLarge = display,
            displayMedium = display.copy(fontSize = 36.sp, lineHeight = 44.sp),
            displaySmall = display.copy(fontSize = 28.sp, lineHeight = 36.sp),
            headlineLarge = title1,
            headlineMedium = title2,
            headlineSmall = title3,
            titleLarge = title1,
            titleMedium = title2,
            titleSmall = title3,
            bodyLarge = body,
            bodyMedium = bodySmall,
            bodySmall = caption,
            labelLarge = label.copy(fontSize = 14.sp),
            labelMedium = label,
            labelSmall = caption.copy(fontWeight = FontWeight.SemiBold)
        )
}

// --- Font family stubs -------------------------------------------------
// These are declared `internal const val GeistLoaded = false` so the swap
// to real fonts is one PR. When Asset M lands, replace the Boolean with
// a real check (e.g. `FontFamily(Font(R.font.geist_regular))` definitions
// gated on the resource existing) and update [GeistFamily] / [GeistMonoFamily]
// / [FrauncesFamily] below.

internal const val GeistLoaded = false

internal val GeistFamily = FontFamily.Default
internal val GeistMonoFamily = FontFamily.Monospace
internal val FrauncesFamily = FontFamily.Serif
