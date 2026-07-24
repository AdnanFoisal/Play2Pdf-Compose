package com.adnanfoisal.play2pdf.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.adnanfoisal.play2pdf.R

/**
 * Typography roles for Play2PDF.
 *
 * Type pairing locked from mockups (per IMPLEMENTATION_PLAN.md §Design Tokens):
 *  - Display / headings: **Space Grotesk** (weight 500/600/700)
 *  - Body: **DM Sans** (weight 400/500/700) — History screen also references
 *    Manrope; DM Sans is the primary, Manrope is the visual sibling and we
 *    don't ship a separate file.
 *
 * Both fonts are loaded as variable TTFs from `res/font/`. Compose picks the
 * correct weight instance via the `Font(resId, weight)` overload — for
 * variable fonts this resolves the `wght` axis, for static fonts it just
 * loads the file. Either way the API is identical.
 *
 * 10 type roles:
 *  Display   — splash wordmark, hero numbers
 *  Title 1   — screen titles (large)
 *  Title 2   — section headers
 *  Title 3   — card titles
 *  Body      — default body
 *  Body Small— metadata, captions inline
 *  Caption   — helper text, footnotes
 *  Label     — uppercase chips, eyebrow text (1.4 letter-spacing)
 *  Code      — error stack traces, API key fields (monospace)
 *  Stat      — big numbers in History / Compiling
 *
 * Usage:
 *   Text("Hello", style = MaterialTheme.typography.titleMedium)  // <- Title 2
 *   Text("hello", style = AppType.label)                          // <- Label
 */
object AppType {

    /** Space Grotesk — display / headings. Loaded from R.font.space_grotesk. */
    private val SpaceGrotesk: FontFamily
        @Composable get() = FontFamily(
            Font(R.font.space_grotesk, FontWeight.Medium),
            Font(R.font.space_grotesk, FontWeight.SemiBold),
            Font(R.font.space_grotesk, FontWeight.Bold)
        )

    /** DM Sans — body text. Loaded from R.font.dm_sans. */
    private val DmSans: FontFamily
        @Composable get() = FontFamily(
            Font(R.font.dm_sans, FontWeight.Normal),
            Font(R.font.dm_sans, FontWeight.Medium),
            Font(R.font.dm_sans, FontWeight.Bold)
        )

    private val Sans: FontFamily
        @Composable get() = DmSans

    private val Mono: FontFamily
        @Composable get() = FontFamily.Monospace

    private val Display: FontFamily
        @Composable get() = SpaceGrotesk

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
            fontFamily = Display,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.3).sp
        )

    val title2: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        )

    val title3: TextStyle
        @Composable get() = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.SemiBold,
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
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.4.sp
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
            fontWeight = FontWeight.Bold,
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

// --- Legacy font-family stubs (kept so any leftover references compile) ---
// The app now loads real fonts via R.font.* above. These are retained as
// no-op aliases for any code that still references the old Geist placeholders.

internal const val GeistLoaded = true

internal val GeistFamily = FontFamily.Default
internal val GeistMonoFamily = FontFamily.Monospace
internal val FrauncesFamily = FontFamily.Serif
