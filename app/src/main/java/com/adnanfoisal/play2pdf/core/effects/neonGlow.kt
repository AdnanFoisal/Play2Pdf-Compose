package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Neon glow modifier: 3-layer radial gradient painted behind the composable.
 *
 * Implementation: draws a transparent round-rect with a [android.graphics.Paint.setShadowLayer]
 * three times at decreasing alpha and increasing radius for the layered glow
 * effect. Works on Android 5+ (no RenderEffect needed).
 *
 * Usage:
 *   Modifier.neonGlow()
 *   Modifier.neonGlow(color = Color.Green)
 *   Modifier.neonGlow(radius = 24.dp)
 */
fun Modifier.neonGlow(
    color: Color = BrandColors.Brand,
    radius: Dp = 16.dp,
    alpha: Float = 0.6f
): Modifier = this.then(
    Modifier.drawBehind {
        val layers = listOf(
            Triple(radius.toPx() * 0.6f, alpha, 0f),
            Triple(radius.toPx(), alpha * 0.6f, 0f),
            Triple(radius.toPx() * 1.6f, alpha * 0.3f, 0f)
        )
        drawIntoCanvas { canvas ->
            layers.forEach { (r, a, _) ->
                val paint = Paint().apply {
                    this.color = Color.Transparent
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        setShadowLayer(r, 0f, 0f, color.copy(alpha = a).toArgb())
                    }
                }
                canvas.drawRoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = 0f,
                    radiusY = 0f,
                    paint = paint
                )
            }
        }
    }
)

/**
 * Convenience gradient brush built from brand colors - used by buttons
 * and the splash wordmark.
 */
fun brandBrush(
    colors: List<Color> = listOf(
        BrandColors.BrandGradientStart,
        BrandColors.BrandGradientEnd
    )
): Brush = Brush.linearGradient(colors = colors)
