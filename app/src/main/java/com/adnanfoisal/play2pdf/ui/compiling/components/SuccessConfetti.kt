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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    val particles = remember { mutableStateOf(List(20) { Particle() }) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1500))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 3f
            val t = progress.value
            particles.value.forEach { p ->
                // Each particle flies outward, falls under gravity, fades.
                val distance = p.initialSpeed * t * 600f
                val x = cx + cos(p.angle) * distance
                val y = cy + sin(p.angle) * distance + 0.5f * 1200f * t * t  // gravity
                val radius = (1f - t) * 6f + 2f
                val alpha = (1f - t).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private data class Particle(
    val angle: Float = Random.nextFloat() * 2f * Math.PI.toFloat(),
    val initialSpeed: Float = 0.6f + Random.nextFloat() * 0.4f,
    val color: Color = listOf(
        BrandColors.Brand,
        BrandColors.BrandLight,
        BrandColors.PureWhite
    ).random()
)
