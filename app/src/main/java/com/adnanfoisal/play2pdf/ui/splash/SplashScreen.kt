package com.adnanfoisal.play2pdf.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import kotlinx.coroutines.delay

/**
 * Splash screen — 4-phase Compose canvas animation derived from the MP4
 * reference in `mock assests/splash screen animation video.mp4`.
 *
 * Per IMPLEMENTATION_PLAN.md Step 3:
 *  - Phase 1 (0–600ms):  logo mark scales 0.4→1.0 + alpha 0→1 (spring)
 *  - Phase 2 (400–900ms): violet radial glow expands behind logo
 *  - Phase 3 (700–1100ms): "Play2PDF" wordmark slides up 20dp→0 + alpha 0→1
 *  - Phase 4 (1200ms+):   hold, then [SplashViewModel] navigates at 2500ms
 *
 * Pure Compose animation — no Rive dependency. The MP4 reference is kept
 * as a TODO comment per the plan.
 *
 * TODO: when Asset H (R.raw.splash_logo.riv) is delivered, swap [LogoMark]
 *       for a `RiveAnimation` composable. Until then this Canvas animation
 *       matches the MP4 beat-for-beat.
 */
@Composable
fun SplashScreen(
    onNavigateNext: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Drive navigation from the ViewModel (holds for Motion.Durations.Splash).
    LaunchedEffect(Unit) {
        viewModel.boot(onNavigateNext)
    }

    // Phase state machine — drives the 4 animation phases.
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        // Phase 1 starts immediately (phase=1).
        phase = 1
        delay(400)   // Phase 2 overlap start
        phase = 2
        delay(300)   // Phase 3 start (700ms in)
        phase = 3
        delay(500)   // Phase 4 hold (1200ms in)
        phase = 4
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Surface0),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoBlock(phase = phase)
            Spacer(Modifier.height(Spacing.lg))

            // Phase 3: wordmark slides up + fades in.
            AnimatedVisibility(
                visible = phase >= 3,
                enter = slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { it / 6 }  // ~20dp up
                ) + fadeIn(animationSpec = tween(400))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Play2PDF",
                        color = BrandColors.TextPrimary,
                        style = AppType.display.copy(fontSize = 32.sp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Playlist \u2192 Study guide",
                        color = BrandColors.TextSecondary,
                        style = AppType.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Logo mark + radial glow. The glow expands behind the logo during Phase 2;
 * the logo scales+fades in during Phase 1.
 */
@Composable
private fun LogoBlock(phase: Int) {
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Phase 2: radial violet glow behind the logo (Canvas drawCircle).
        val glowAlpha by animateFloatAsState(
            targetValue = if (phase >= 2) 0.55f else 0f,
            animationSpec = tween(500),
            label = "glowAlpha"
        )
        val glowScale by animateFloatAsState(
            targetValue = if (phase >= 2) 1f else 0.3f,
            animationSpec = tween(500),
            label = "glowScale"
        )
        if (glowAlpha > 0.01f) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .scale(glowScale)
                    .alpha(glowAlpha)
            ) {
                val center = Offset(size.minDimension / 2f, size.minDimension / 2f)
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandColors.BrandStrong,
                            BrandColors.BrandDeep.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )
                )
            }
        }

        // Phase 1: logo mark scales 0.4→1.0 + alpha 0→1 with a medium-bouncy spring.
        val logoScale by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0.4f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "logoScale"
        )
        val logoAlpha by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0f,
            animationSpec = tween(500),
            label = "logoAlpha"
        )
        Image(
            painter = painterResource(id = R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
        )
    }
}
