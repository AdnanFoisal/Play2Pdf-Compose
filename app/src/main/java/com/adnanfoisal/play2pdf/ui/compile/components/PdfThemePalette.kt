package com.adnanfoisal.play2pdf.ui.compile.components

import androidx.compose.ui.graphics.Color
import com.adnanfoisal.play2pdf.domain.model.PdfTheme

/**
 * Faithful color palette for each of the 13 backend PDF themes.
 *
 * These drive the in-app mini "page" previews so a user can see what a
 * theme looks like before compiling — no images required, every preview is
 * drawn in Compose from these values. Colors are hand-tuned to echo each
 * theme's real identity on the backend (server.py THEMES dict).
 *
 * @param page    the page/paper background
 * @param heading heading text + rule color (the theme's signature accent)
 * @param body    body text color
 * @param accent  secondary accent (footer rule, bullet, tag)
 * @param serif   true if the theme reads as a serif/editorial face (affects
 *                the mini-preview's rendered line style)
 */
data class PdfThemePalette(
    val page: Color,
    val heading: Color,
    val body: Color,
    val accent: Color,
    val serif: Boolean,
    val fontName: String
)

fun PdfTheme.palette(): PdfThemePalette = when (this) {
    PdfTheme.NordicFrost -> PdfThemePalette(
        page = Color(0xFFF2F6FA), heading = Color(0xFF2C3E50),
        body = Color(0xFF2C3E50), accent = Color(0xFF86A8C4), serif = true, fontName = "Times"
    )
    PdfTheme.VelvetDawn -> PdfThemePalette(
        page = Color(0xFFFDF6F1), heading = Color(0xFF5C4646),
        body = Color(0xFF5C4646), accent = Color(0xFFD69E91), serif = true, fontName = "Times"
    )
    PdfTheme.MintBlueprint -> PdfThemePalette(
        page = Color(0xFFEDFCF8), heading = Color(0xFF0F766E),
        body = Color(0xFF0F766E), accent = Color(0xFF34D399), serif = false, fontName = "Courier"
    )
    PdfTheme.GoldenEra -> PdfThemePalette(
        page = Color(0xFFF4F0E6), heading = Color(0xFF423426),
        body = Color(0xFF423426), accent = Color(0xFFB8914E), serif = true, fontName = "Times"
    )
    PdfTheme.MidnightPurple -> PdfThemePalette(
        page = Color(0xFF140F26), heading = Color(0xFFFFFFFF),
        body = Color(0xFFFFFFFF), accent = Color(0xFFFF2A80), serif = false, fontName = "Helvetica"
    )
    PdfTheme.Cyberpunk2077 -> PdfThemePalette(
        page = Color(0xFF121212), heading = Color(0xFFFAFA33),
        body = Color(0xFFFAFA33), accent = Color(0xFF00FFF0), serif = false, fontName = "Courier"
    )
    PdfTheme.ObsidianCrimson -> PdfThemePalette(
        page = Color(0xFF0A0A0A), heading = Color(0xFFE0E0E0),
        body = Color(0xFFE0E0E0), accent = Color(0xFFDC143C), serif = false, fontName = "Helvetica"
    )
    PdfTheme.OceanicAbyss -> PdfThemePalette(
        page = Color(0xFF040F1F), heading = Color(0xFFF0F8FF),
        body = Color(0xFFF0F8FF), accent = Color(0xFF00CCFF), serif = false, fontName = "Helvetica"
    )
}
