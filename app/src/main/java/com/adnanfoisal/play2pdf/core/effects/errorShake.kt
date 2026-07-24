package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import com.adnanfoisal.play2pdf.tokens.Motion
import kotlinx.coroutines.launch

/**
 * Error shake modifier: shakes the composable horizontally for 400ms
 * when [trigger] changes.
 *
 * Pattern per Phase E: 0 → -8 → 8 → -4 → 4 → 0 over 400ms.
 *
 * Usage:
 *   Column(modifier = Modifier.errorShake(trigger = formErrors)) { ... }
 */
fun Modifier.errorShake(trigger: Any?): Modifier = composed {
    val shakeOffset = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(trigger) {
        if (trigger != null) {
            // 0 → -8 → 8 → -4 → 4 → 0
            launch {
                shakeOffset.animateTo(-8f, tween(80, easing = Motion.Easings.Standard))
                shakeOffset.animateTo(8f, tween(80, easing = Motion.Easings.Standard))
                shakeOffset.animateTo(-4f, tween(80, easing = Motion.Easings.Standard))
                shakeOffset.animateTo(4f, tween(80, easing = Motion.Easings.Standard))
                shakeOffset.animateTo(0f, tween(80, easing = Motion.Easings.Standard))
            }
        }
    }

    this.offset { IntOffset(shakeOffset.value.toInt(), 0) }
}
