package com.adnanfoisal.play2pdf.theme

import androidx.compose.material3.Typography
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
 * Type pairing per the design docs (docs/BRAND.md):
 *  - Display / headings: **Space Grotesk**
 *  - Body: **DM Sans**
 *
 * Both ship as variable TTFs in `res/font/` and are registered at every
 * weight we use. NO downloadable fonts, no GoogleFont provider, no Play
 * Services dependency, no first-run network fetch — the previous
 * implementation pulled "Inter" via GMS downloadable fonts, which meant a
 * fallback-to-Roboto flash on first paint and plain system sans on devices
 * without Play Services. Bundled = identical rendering everywhere.
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

    // FontFamily instances are built ONCE at object init — never inside a
    // @Composable getter (that allocates a new FontFamily on every
    // recomposition, a real perf bug on animation-heavy screens).
    //
    // The TTFs are variable fonts: registering the same resource at each
    // weight lets Compose resolve the wght axis per TextStyle weight.

    val SpaceGrotesk: FontFamily = FontFamily(
        Font(R.font.space_grotesk, FontWeight.Normal),
        Font(R.font.space_grotesk, FontWeight.Medium),
        Font(R.font.space_grotesk, FontWeight.SemiBold),
        Font(R.font.space_grotesk, FontWeight.Bold),
    )

    val DmSans: FontFamily = FontFamily(
        Font(R.font.dm_sans, FontWeight.Normal),
        Font(R.font.dm_sans, FontWeight.Medium),
        Font(R.font.dm_sans, FontWeight.SemiBold),
        Font(R.font.dm_sans, FontWeight.Bold),
    )

    private val Sans: FontFamily = DmSans
    private val Mono: FontFamily = FontFamily.Monospace
    private val Display: FontFamily = SpaceGrotesk

    val display: TextStyle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).sp
    )

    val title1: TextStyle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    )

    val title2: TextStyle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    val title3: TextStyle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    val body: TextStyle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val bodySmall: TextStyle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val caption: TextStyle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val label: TextStyle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.4.sp
    )

    val code: TextStyle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val stat: TextStyle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,   // matches Home mockup stat number (was 36)
        lineHeight = 38.sp,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center
    )

    /** Large stat — Compiling ring percentage (54px in mockup). */
    val statLarge: TextStyle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.5).sp,
        textAlign = TextAlign.Center
    )

    /** Primary CTA button label (Compile button — 15.5px/600 in mockup). */
    val button: TextStyle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /** Material 3 [Typography] instance — maps our roles to M3's slot
     *  system so Material components (e.g. TopAppBar, Card) pick up our
     *  typeface automatically. */
    val material: Typography = Typography(
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
