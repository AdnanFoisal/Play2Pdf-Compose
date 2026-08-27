package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * Haptic-on-tap modifier: triggers a haptic when the composable is tapped.
 *
 * This is the lightweight variant — for the full haptic patterns table
 * (light / medium / heavy / success / error), use [com.adnanfoisal.play2pdf.core.haptics.HapticsManager]
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
 *
 * (The old `hapticOnTouch` variant was deleted — it was a literal no-op
 * that returned `this`; nothing called it.)
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
