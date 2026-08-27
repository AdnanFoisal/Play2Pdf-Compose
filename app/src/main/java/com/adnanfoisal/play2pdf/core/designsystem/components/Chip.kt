package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Animated chip: bouncy spring pop-in on add, brand tint, optional close
 * button and select-on-click.
 *
 * Source spec: v2.0 §9.4 — "spring add, spring remove".
 *
 * Used for syllabus topics in the Compile screen, and for filter chips in
 * the History screen.
 *
 * Usage:
 *   AnimatedChip(text = "Big-O Notation", onClose = { vm.removeTopic(it) })
 *   AnimatedChip(text = "All", selected = true, onClick = { vm.filterAll() })
 */
@Composable
fun AnimatedChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    val bg = if (selected) BrandColors.Brand else BrandColors.Surface3
    val fg = if (selected) BrandColors.PureWhite else BrandColors.TextPrimary
    val borderColor = if (selected) BrandColors.BrandLight else BrandColors.SurfaceBorder

    // Pop-in: 0.6 -> 1.0 with the bouncy spring the spec calls for. (The
    // previous animateFloatAsState(targetValue = 1f) was dead code — the
    // target never changed, so nothing ever animated.)
    val popScale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        popScale.animateTo(1f, animationSpec = Motion.Springs.bouncy)
    }

    Row(
        modifier = modifier
            .scale(popScale.value)
            .clip(AppShape.pill)
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = AppShape.pill)
            .then(
                if (onClick != null) {
                    Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.95f)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            color = fg,
            style = AppType.label.copy(fontSize = 13.sp)
        )
        if (onClose != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(fg.copy(alpha = 0.15f))
                    .pressScaleClickable(onClick = onClose, pressedScale = 0.7f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = fg,
                    style = AppType.label.copy(fontSize = 13.sp)
                )
            }
        }
    }
}
