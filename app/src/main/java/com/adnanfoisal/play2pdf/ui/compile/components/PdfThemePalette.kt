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
    val serif: Boolean
)

fun PdfTheme.palette(): PdfThemePalette = when (this) {
    PdfTheme.TufteScholar -> PdfThemePalette(
        page = Color(0xFFFBF8F1), heading = Color(0xFF1A1A17),
        body = Color(0xFF4A4740), accent = Color(0xFFB03A2E), serif = true
    )
    PdfTheme.PrincetonMath -> PdfThemePalette(
        page = Color(0xFFFFFFFF), heading = Color(0xFF14213D),
        body = Color(0xFF3D4658), accent = Color(0xFFE8A200), serif = true
    )
    PdfTheme.MidnightTerminal -> PdfThemePalette(
        page = Color(0xFF0D1117), heading = Color(0xFF39D353),
        body = Color(0xFF8B949E), accent = Color(0xFF58A6FF), serif = false
    )
    PdfTheme.CambridgeEmerald -> PdfThemePalette(
        page = Color(0xFFF7FAF7), heading = Color(0xFF0B3D2E),
        body = Color(0xFF41524A), accent = Color(0xFF2E8B57), serif = true
    )
    PdfTheme.BauhausGeometric -> PdfThemePalette(
        page = Color(0xFFFDFDFB), heading = Color(0xFF111111),
        body = Color(0xFF3A3A3A), accent = Color(0xFFE63946), serif = false
    )
    PdfTheme.SwissStark -> PdfThemePalette(
        page = Color(0xFFFFFFFF), heading = Color(0xFF000000),
        body = Color(0xFF2B2B2B), accent = Color(0xFFD00000), serif = false
    )
    PdfTheme.OxfordBurgundy -> PdfThemePalette(
        page = Color(0xFFFDF9F6), heading = Color(0xFF5C1A1B),
        body = Color(0xFF4A3B38), accent = Color(0xFF8C2F39), serif = true
    )
    PdfTheme.DeepSpace -> PdfThemePalette(
        page = Color(0xFF0B0E1A), heading = Color(0xFF9D8CFF),
        body = Color(0xFF8891B0), accent = Color(0xFF22D3EE), serif = false
    )
    PdfTheme.MitTech -> PdfThemePalette(
        page = Color(0xFFFFFFFF), heading = Color(0xFFA31F34),
        body = Color(0xFF33373D), accent = Color(0xFF8A8B8C), serif = false
    )
    PdfTheme.WhartonLedger -> PdfThemePalette(
        page = Color(0xFFFCFBF7), heading = Color(0xFF1B3A2B),
        body = Color(0xFF44483F), accent = Color(0xFFB08D2E), serif = true
    )
    PdfTheme.SumiInk -> PdfThemePalette(
        page = Color(0xFFF6F4EF), heading = Color(0xFF1C1C1C),
        body = Color(0xFF474743), accent = Color(0xFF8A5A44), serif = true
    )
    PdfTheme.RenaissanceGold -> PdfThemePalette(
        page = Color(0xFFFBF3E0), heading = Color(0xFF5A4413),
        body = Color(0xFF5C513A), accent = Color(0xFFC9A227), serif = true
    )
    PdfTheme.WarmSunsetDark -> PdfThemePalette(
        page = Color(0xFF1A1214), heading = Color(0xFFFF9E64),
        body = Color(0xFFB8A39A), accent = Color(0xFFF2545B), serif = false
    )
}
