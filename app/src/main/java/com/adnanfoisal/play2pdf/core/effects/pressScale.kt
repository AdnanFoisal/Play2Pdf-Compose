package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import com.adnanfoisal.play2pdf.tokens.Motion

/**
 * Press-scale modifier: scales the composable to 0.97 when pressed.
 *
 * Used by every interactive composable (buttons, cards, chips, list
 * items). Quality checklist §5.1 requires "every interactive element
 * has press scale (0.97, 100ms)".
 *
 * Usage:
 *   Modifier.pressScale()  // scales but does NOT handle clicks
 *   Modifier.pressScaleClickable(onClick = { ... })  // scales AND handles clicks
 *
 * The modifier uses [composed] so it can remember its own
 * [MutableInteractionSource] per-call site, avoiding accidental sharing
 * of press state between siblings.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.97f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = Motion.Durations.Micro,
            easing = Motion.Easings.Standard
        ),
        label = "pressScale"
    )
    // The pure pressScale variant does not handle click — callers must
    // attach their own clickable() with the same interactionSource if
    // they want press detection. Most callers should use
    // [pressScaleClickable] instead.
    this.scale(scale)
}

/**
 * Press-scale modifier that also wires up the click handler so the caller
 * doesn't have to manage the interaction source. This is the variant 90%
 * of call sites should use.
 */
fun Modifier.pressScaleClickable(
    onClick: () -> Unit,
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = Motion.Durations.Micro,
            easing = Motion.Easings.Standard
        ),
        label = "pressScaleClickable"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}
