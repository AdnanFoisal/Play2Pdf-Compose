package com.adnanfoisal.play2pdf.ui.compiling.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.theme.BrandColors
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Success confetti — custom Canvas-based particle burst.
 *
 * Per Asset L: the Design Agent will deliver a Lottie JSON for a more
 * polished effect. Until then, this Canvas fallback (60 lines of
 * Kotlin per §3.2) renders 20 brand-colored particles that burst from
 * the center, fall under gravity, and fade out.
 *
 * TODO: replace with `LottieAnimation(LottieCompositionSpec.RawRes(R.raw.success_confetti))`
 * when Asset L is delivered.
 */
@Composable
fun SuccessConfetti(
    modifier: Modifier = Modifier,
    trigger: Any? = Unit  // change this to re-fire the burst
) {
    val particles = remember(trigger) { List(56) { Particle() } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 2200))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 3f
            val t = progress.value
            if (t <= 0f || t >= 1f) return@Canvas
            particles.forEach { p ->
                // Fly outward with ease-out, then fall under gravity + drift.
                val ease = 1f - (1f - t) * (1f - t)
                val distance = p.initialSpeed * ease * 640f
                val x = cx + cos(p.angle) * distance + p.drift * t * 90f
                val y = cy + sin(p.angle) * distance + 0.5f * 1500f * t * t
                // Fade only in the last 35% so particles stay vivid mid-flight.
                val alpha = (if (t < 0.65f) 1f else (1f - t) / 0.35f).coerceIn(0f, 1f)
                val rot = p.spin * t * 12f
                val len = p.size
                // Ribbon: a thin rotated rounded rect (spinning streamer).
                rotate(degrees = rot, pivot = Offset(x, y)) {
                    drawRoundRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - len * 0.35f, y - len),
                        size = Size(len * 0.7f, len * 2f),
                        cornerRadius = CornerRadius(len * 0.35f, len * 0.35f)
                    )
                }
            }
        }
    }
}

private data class Particle(
    val angle: Float = (Random.nextFloat() * 0.9f + 0.05f) * Math.PI.toFloat() * -1f, // upward fan
    val initialSpeed: Float = 0.55f + Random.nextFloat() * 0.6f,
    val drift: Float = Random.nextFloat() * 2f - 1f,
    val spin: Float = Random.nextFloat() * 2f - 1f,
    val size: Float = 4f + Random.nextFloat() * 5f,
    val color: Color = listOf(
        BrandColors.Brand,
        BrandColors.BrandLight,
        BrandColors.Cyan,
        BrandColors.Fuchsia,
        BrandColors.Amber,
        BrandColors.PureWhite
    ).random()
)
