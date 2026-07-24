package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.effects.glassBlur
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.tokens.Elevation
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Premium card: layered shadow + 1px border + subtle surface color shift.
 *
 * Source spec: v2.0 §9.2.
 *
 * Used by every card in the app — compile input card, history list items,
 * settings sections, etc. For glass-morphic floating cards (over a busy
 * background), use [GlassCard] instead.
 *
 * @param onClick If non-null, the card becomes pressable (press scale 0.98).
 * @param raised  If true, uses the "hover" elevation (6dp) instead of the
 *                default card elevation (2dp).
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    raised: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val elevation = if (raised) Elevation.cardHover else Elevation.card
    val shape = AppShape.large

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(BrandColors.Surface1)
            .border(width = 1.dp, color = BrandColors.SurfaceBorder, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.98f)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

/**
 * Glass card: real backdrop blur on Android 12+, translucent tint overlay
 * + hairline border on all versions.
 *
 * Source spec: v2.0 §9.3.
 *
 * Use for floating cards that sit over a busy background (e.g. the splash
 * wordmark card, or a sticky bottom bar). For a regular card sitting on
 * the app surface, use [PremiumCard] — it has better contrast.
 *
 * The fallback on Android < 12 is just the tint + border, which still
 * reads as "frosted" because the background is busy.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = AppShape.large

    Box(
        modifier = modifier
            .glassBlur(radius = blurRadius, shape = shape)
            .clip(shape)
            .background(BrandColors.GlassTint)
            .border(width = 1.dp, color = BrandColors.GlassBorder, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.98f)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}
