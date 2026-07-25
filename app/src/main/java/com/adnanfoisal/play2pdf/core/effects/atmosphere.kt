package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Per-screen atmospheric glow. One shared deep base ([BrandColors.Bg]) with
 * radial gradients placed differently per screen so each feels alive without
 * fragmenting the palette (Part A creative direction §1.1).
 *
 * Apply AFTER a solid `.background(BrandColors.Bg)` fill.
 */

/** Home / default: violet glow anchored top-center. */
fun Modifier.homeAtmosphere(): Modifier = this
    .background(
        Brush.radialGradient(
            colors = listOf(BrandColors.GlowViolet.copy(alpha = 0.9f), Color.Transparent),
            center = Offset(0.5f * 1000f, 0f),
            radius = 900f
        )
    )

/** Settings: same as Home but a touch softer. */
fun Modifier.settingsAtmosphere(): Modifier = this
    .background(
        Brush.radialGradient(
            colors = listOf(BrandColors.GlowViolet.copy(alpha = 0.7f), Color.Transparent),
            center = Offset(0.5f * 1000f, 0f),
            radius = 850f
        )
    )

/** Compiling: deep vertical fade — darkest at top, brand-tinted mid. */
fun Modifier.compilingAtmosphere(): Modifier = this
    .background(
        Brush.verticalGradient(
            colors = listOf(
                BrandColors.CompilingBg,
                BrandColors.GlowDeep,
                BrandColors.CompilingBg
            )
        )
    )

/** History: indigo glow anchored bottom-right. */
fun Modifier.historyAtmosphere(): Modifier = this
    .background(
        Brush.radialGradient(
            colors = listOf(BrandColors.GlowIndigo.copy(alpha = 0.85f), Color.Transparent),
            center = Offset(1000f, 1400f),
            radius = 1000f
        )
    )
