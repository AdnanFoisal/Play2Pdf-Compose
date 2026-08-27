package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Per-screen atmospheric glow. One shared deep base ([BrandColors.Bg]) with
 * radial gradients placed differently per screen so each feels alive without
 * fragmenting the palette.
 *
 * All geometry is RELATIVE to the composable's size — the previous version
 * used absolute pixel offsets (center 500f/1000f, radius 900f), which placed
 * the glow correctly on exactly one screen size and nowhere else.
 *
 * Apply AFTER a solid `.background(BrandColors.Bg)` fill.
 */

/** Home / default: green glow anchored top-center. */
fun Modifier.homeAtmosphere(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(BrandColors.GlowGreen.copy(alpha = 0.9f), Color.Transparent),
            center = Offset(size.width * 0.5f, 0f),
            radius = size.width * 0.9f
        )
    )
}

/** Settings: same as Home but a touch softer. */
fun Modifier.settingsAtmosphere(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(BrandColors.GlowGreen.copy(alpha = 0.7f), Color.Transparent),
            center = Offset(size.width * 0.5f, 0f),
            radius = size.width * 0.85f
        )
    )
}

/** Compiling: deep vertical fade — darkest at top, brand-tinted mid. */
fun Modifier.compilingAtmosphere(): Modifier = drawBehind {
    drawRect(
        Brush.verticalGradient(
            colors = listOf(
                BrandColors.CompilingBg,
                BrandColors.GlowDeep,
                BrandColors.CompilingBg
            )
        )
    )
}

/** History: teal glow anchored bottom-right. */
fun Modifier.historyAtmosphere(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(BrandColors.GlowTeal.copy(alpha = 0.85f), Color.Transparent),
            center = Offset(size.width, size.height),
            radius = size.width * 0.9f
        )
    )
}
