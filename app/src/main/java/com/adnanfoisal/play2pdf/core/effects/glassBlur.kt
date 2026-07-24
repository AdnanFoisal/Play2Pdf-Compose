package com.adnanfoisal.play2pdf.core.effects

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.theme.AppShape

/**
 * Glass blur modifier: real `Modifier.blur` on Android 12+, no-op on
 * older versions where RenderEffect isn't available.
 *
 * On Android < 12 (API 31), `Modifier.blur` falls back to a no-op
 * silently — but the visual result is still acceptable because the
 * [GlassCard] composable layers a translucent tint on top, so even
 * without real blur the card reads as "frosted glass" against a busy
 * background.
 *
 * Usage:
 *   Modifier.glassBlur()                  // 16dp blur, small corner radius
 *   Modifier.glassBlur(radius = 24.dp)    // stronger blur
 */
fun Modifier.glassBlur(
    radius: Dp = 16.dp,
    shape: Shape = AppShape.small
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius)
    } else {
        // No-op fallback — GlassCard still renders with its tint overlay.
        this
    }
}

/**
 * Forced no-blur variant for places where we explicitly want to skip
 * the RenderEffect (e.g. a card on top of a solid background, where
 * blur adds nothing but costs perf).
 */
fun Modifier.noGlassBlur(): Modifier = this
