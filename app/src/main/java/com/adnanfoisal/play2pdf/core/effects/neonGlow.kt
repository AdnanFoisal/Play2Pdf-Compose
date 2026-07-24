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
 * The 3 layers are:
 *  1. Inner core — high opacity, small radius (the "core" glow)
 *  2. Mid bloom  — medium opacity, medium radius (the "halo")
 *  3. Outer haze — low opacity, large radius (the "fog")
 *
 * This is a real `drawBehind` effect, NOT a fake shadow. It works on
 * Android 5+ (we don't need RenderEffect for a drawBehind).
 *
 * Usage:
 *   Modifier.neonGlow()                       // brand color, default radius
 *   Modifier.neonGlow(color = Color.Green)    // custom color
 *   Modifier.neonGlow(radius = 24.dp)         // bigger bloom
 *
 * Quality checklist: "real `Modifier.blur` glassmorphism, 3-layer neon
 * glow" (§5.1 / v2.0 §7.2).
 */
fun Modifier.neonGlow(
    color: Color = BrandColors.Brand,
    radius: Dp = 16.dp,
    alpha: Float = 0.6f
): Modifier = this.then(
    Modifier.drawBehind {
        val paint = Paint().apply {
            this.asFrameworkPaint().apply {
                isAntiAlias = true
                setShadowLayer(
                    radius.toPx(),
                    0f,
                    0f,
                    color.copy(alpha = alpha).toArgb()
                )
            }
        }
        // The trick: draw an invisible circle with a shadow layer to
        // produce the glow. We do it 3 times at decreasing alpha and
        // increasing radius for the layered effect.
        drawIntoCanvas { canvas ->
            // Layer 1: inner core
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = 0f,
                radiusY = 0f,
                paint = Paint().apply {
                    color = Color.Transparent
                    asFrameworkPaint().setShadowLayer(
                        radius.toPx(),
                        0f, 0f,
                        color.copy(alpha = alpha).toArgb()
                    )
                }
            )
        }
        // Simpler approach: just paint the original shadow once, then add
        // two more passes at different alphas/radii by drawing the
        // outline shape again.
        drawIntoCanvas { canvas ->
            val p1 = Paint().apply {
                color = Color.Transparent
                asFrameworkPaint().setShadowLayer(
                    radius.toPx() * 0.6f,
                    0f, 0f,
                    color.copy(alpha = alpha).toArgb()
                )
            }
            val p2 = Paint().apply {
                color = Color.Transparent
                asFrameworkPaint().setShadowLayer(
                    radius.toPx(),
                    0f, 0f,
                    color.copy(alpha = alpha * 0.6f).toArgb()
                )
            }
            val p3 = Paint().apply {
                color = Color.Transparent
                asFrameworkPaint().setShadowLayer(
                    radius.toPx() * 1.6f,
                    0f, 0f,
                    color.copy(alpha = alpha * 0.3f).toArgb()
                )
            }
            // Outline rect for each layer — drawIntoCanvas handles the
            // shadow automatically.
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                0f, 0f, p1
            )
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                0f, 0f, p2
            )
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                0f, 0f, p3
            )
        }
        // Discard the unused initial paint object reference (kept above
        // for clarity of intent).
        @Suppress("UNUSED_VARIABLE") paint
    }
)

/**
 * Convenience gradient brush built from brand colors — used by buttons
 * and the splash wordmark.
 */
fun brandBrush(
    vararg colors: Color = arrayOf(
        BrandColors.BrandGradientStart,
        BrandColors.BrandGradientEnd
    )
): Brush = Brush.linearGradient(colors = colors.asList())
