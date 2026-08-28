package com.adnanfoisal.play2pdf.core.effects

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has disabled animations system-wide (Developer
 * options → Animator/Animation duration scale = off, or the accessibility
 * "Remove animations" toggle). Premium apps honour it — ambient loops
 * (aura, shimmer, sheen, pulse) should render static frames instead.
 *
 * Usage:
 *   val reduceMotion = rememberReduceMotion()
 *   val auraScale = if (reduceMotion) 1f else animatedValue
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) == 0f ||
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}
