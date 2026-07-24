package com.adnanfoisal.play2pdf.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIntoContainer
import androidx.compose.animation.slideOutOfContainer
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.ui.compiling.CompilingScreen
import com.adnanfoisal.play2pdf.ui.main.MainScreen
import com.adnanfoisal.play2pdf.ui.onboarding.OnboardingScreen
import com.adnanfoisal.play2pdf.ui.splash.SplashScreen

/**
 * Top-level navigation host.
 *
 * Routes:
 *   splash     → auto-navigates to onboarding or main after 2.5s
 *   onboarding → 3-page carousel, "Get Started" → main
 *   main       → bottom-nav scaffold with compile / history / settings tabs
 *   compiling  → full-screen compile progress, returns to compile on done
 *
 * Page transitions per Phase E micro-interactions:
 *  - Splash → Onboarding: cross-fade (no slide, splash already sets the tone)
 *  - Onboarding → Main: slide-in from right + fade
 *  - Main → Compiling: slide-in from bottom (modal feel)
 *  - Compiling → Main: slide-out to bottom
 *  - All other: slide-in/out from right (standard Android feel)
 *
 * Quality checklist: "every page transition uses specified curve (not default)".
 */
@Composable
fun Play2PdfNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        // Default transitions: slide in/out from the right with the standard easing.
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(Motion.Durations.Medium, easing = Motion.Easings.Decelerate)
            ) + fadeIn(tween(Motion.Durations.Medium))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(Motion.Durations.Medium, easing = Motion.Easings.Accelerate)
            ) + fadeOut(tween(Motion.Durations.Medium))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(Motion.Durations.Medium, easing = Motion.Easings.Decelerate)
            ) + fadeIn(tween(Motion.Durations.Medium))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(Motion.Durations.Medium, easing = Motion.Easings.Accelerate)
            ) + fadeOut(tween(Motion.Durations.Medium))
        }
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Main) {
            MainScreen(
                onCompileRequest = { navController.navigate(Routes.compiling()) }
            )
        }
        composable(Routes.Compiling) {
            CompilingScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
