package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Shimmer skeleton: infinite gradient sweep used as a loading placeholder.
 *
 * Source spec: v2.0 §9.6.
 *
 * Usage:
 *   ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(120.dp))
 *   ShimmerSkeleton(shape = AppShape.pill, modifier = Modifier.size(80.dp, 24.dp))
 *
 * Quality checklist: "60fps scroll" — the shimmer is a single
 * [rememberInfiniteTransition] that animates a single float, so it's
 * cheap and won't jank.
 */
@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = AppShape.small,
    height: Dp? = null,
    width: Dp? = null
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = Motion.Easings.Standard),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            BrandColors.Surface2,
            BrandColors.Surface3,
            BrandColors.Surface2
        ),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )

    val sized = modifier
        .then(if (width != null) Modifier.width(width) else Modifier)
        .then(if (height != null) Modifier.height(height) else Modifier)

    Box(
        modifier = sized
            .clip(shape)
            .background(brush)
    )
}
