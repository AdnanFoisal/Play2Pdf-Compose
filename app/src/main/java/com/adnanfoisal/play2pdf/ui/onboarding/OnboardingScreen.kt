package com.adnanfoisal.play2pdf.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButton
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButtonVariant
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import kotlinx.coroutines.launch

/**
 * 3-page onboarding carousel.
 *
 * Per the v2.0 plan §10.2 + §3.2 placeholder policy:
 *  - Uses Design Agent's illustrations (R.drawable.onboarding_1/2/3) IF
 *    delivered; otherwise uses [AppIcons.Sparkle] at 240dp with brand tint.
 *  - Skip button (top-right), page indicator dots, Get Started button on
 *    page 3.
 *  - Page transitions use HorizontalPager's default spring — good enough.
 *
 * When the user finishes onboarding (Get Started or Skip), [OnboardingViewModel]
 * persists [onboardingComplete] so this screen only shows once after install.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pageCount = 3
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    val finish = {
        viewModel.markOnboardingComplete()
        onComplete()
    }

    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_page_1_title),
            subtitle = stringResource(R.string.onboarding_page_1_subtitle)
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page_2_title),
            subtitle = stringResource(R.string.onboarding_page_2_subtitle)
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page_3_title),
            subtitle = stringResource(R.string.onboarding_page_3_subtitle)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Surface0)
    ) {
        // Top-right Skip button
        Text(
            text = stringResource(R.string.onboarding_skip),
            color = BrandColors.TextSecondary,
            style = AppType.label,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.lg)
                .pressScaleClickable(onClick = finish)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(state = pagerState) { page ->
                OnboardingPageContent(pages[page])
            }

            Spacer(Modifier.height(Spacing.lg))

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) BrandColors.Brand else BrandColors.Surface3)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            PrimaryButton(
                text = if (pagerState.currentPage == pageCount - 1)
                    stringResource(R.string.onboarding_get_started)
                else
                    "Next",
                onClick = {
                    if (pagerState.currentPage == pageCount - 1) {
                        finish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = androidx.compose.animation.core.tween(
                                    Motion.Durations.Medium,
                                    easing = Motion.Easings.Standard
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class OnboardingPage(val title: String, val subtitle: String)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // TODO: replace with R.drawable.onboarding_1/2/3 when Asset I is delivered.
        // Until then: render AppIcons.Sparkle at 240dp inside a brand-tinted rounded square.
        Box(
            modifier = Modifier
                .size(240.dp, 180.dp)
                .clip(AppShape.large)
                .background(BrandColors.Surface2),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = page.title,
            color = BrandColors.TextPrimary,
            style = AppType.title1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = page.subtitle,
            color = BrandColors.TextSecondary,
            style = AppType.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
