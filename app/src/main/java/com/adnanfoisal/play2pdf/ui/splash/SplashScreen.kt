package com.adnanfoisal.play2pdf.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Splash screen.
 *
 * Per the v2.0 plan §10.1:
 *  - Uses the Design Agent's Rive animation from R.raw.splash_logo IF
 *    delivered; otherwise uses a CircularProgressIndicator placeholder
 *    (§3.2 fallback table).
 *  - Cross-fades the wordmark in.
 *  - Auto-navigates to onboarding or compile after 2.5s based on the
 *    onboarding-complete flag (handled by [SplashViewModel]).
 *
 * Asset status: Rive animation NOT YET DELIVERED — we use the
 * CircularProgressIndicator + logo_mark drawable fallback. When Asset H
 * lands, replace the [LogoBlock] content with a `RiveAnimation` call.
 */
@Composable
fun SplashScreen(
    onNavigateNext: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.boot(onNavigateNext)
    }

    var showWordmark by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Cross-fade the wordmark in after 600ms (after the spinner has started).
        kotlinx.coroutines.delay(600)
        showWordmark = true
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
            LogoBlock()
            Spacer(Modifier.height(Spacing.lg))
            AnimatedVisibility(
                visible = showWordmark,
                enter = scaleIn(tween(Motion.Durations.Long, easing = Motion.Easings.Standard)) +
                        fadeIn(tween(Motion.Durations.Long))
            ) {
                Text(
                    text = "Play2PDF",
                    color = BrandColors.TextPrimary,
                    style = AppType.display.copy(fontSize = 32.sp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            AnimatedVisibility(
                visible = showWordmark,
                enter = fadeIn(tween(Motion.Durations.Long))
            ) {
                Text(
                    text = "Playlist → Study guide",
                    color = BrandColors.TextSecondary,
                    style = AppType.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Splash logo block. Per the asset fallback table, uses [R.drawable.logo_mark]
 * until the Design Agent delivers the Rive animation (Asset H).
 *
 * TODO: replace with `RiveAnimation(resId = R.raw.splash_logo)` when
 *       Asset H is delivered.
 */
@Composable
private fun LogoBlock() {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        CircularProgressIndicator(
            modifier = Modifier.size(120.dp),
            strokeWidth = 2.dp,
            color = BrandColors.Brand
        )
    }
}
