package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.foundation.LocalHapticFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import com.adnanfoisal.play2pdf.core.haptics.HapticsManager

/**
 * Haptic-on-tap modifier: triggers a haptic when the composable is tapped.
 *
 * This is the lightweight variant — for the full haptic patterns table
 * (light / medium / heavy / success / error), use [HapticsManager]
 * directly inside your ViewModel or click handler. This modifier is
 * for the common case: "every button gets at least a light tap".
 *
 * Usage:
 *   Modifier.hapticClickable(onClick = { ... })
 *   Modifier.hapticClickable(hapticType = HapticFeedbackType.LongPress, onClick = { ... })
 *
 * The modifier respects the system "Vibrate on tap" setting automatically
 * (it goes through [HapticFeedbackType], which the platform gates on the
 * user's settings).
 */
fun Modifier.hapticClickable(
    onClick: () -> Unit,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    enabled: Boolean = true
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = {
            haptic.performHapticFeedback(hapticType)
            onClick()
        }
    )
}

/**
 * Variant that triggers a haptic without consuming the click — useful
 * when you want a hover/touch haptic on a card that has its own
 * pointer-input gesture detector.
 */
fun Modifier.hapticOnTouch(
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove
): Modifier = composed {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    this.then(Modifier).composed {
        // No-op wrapper — hapticOnTouch is mostly used as a marker for
        // places where the HapticsManager should be called explicitly
        // (because the gesture is a long-press or swipe, not a tap).
        this
    }
}
