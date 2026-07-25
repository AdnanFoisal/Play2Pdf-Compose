package com.adnanfoisal.play2pdf.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Motion tokens — every animation in the app uses these constants.
 *
 * Three categories:
 *  1. **Durations** — 150ms (micro, e.g. press scale), 300ms (standard,
 *     e.g. page transition), 500ms (long, e.g. modal open).
 *  2. **Easings** — Material's standard curves. Never use `tween(300)`
 *     with the default `LinearEasing` — always pass one of these.
 *  3. **Spring specs** — for chip add/remove, modal open/close, and any
 *     physics-based motion where a tween would feel robotic.
 *
 * Why a central file? Because "every animation uses these curves" is
 * one of the Quality Checklist items (§5.1). If the curve lives in one
 * place, the linter can enforce it.
 */
object Motion {
    object Durations {
        const val Micro = 150    // ms — press scale, ripple
        const val Short = 200    // ms — chip add/remove
        const val Medium = 300   // ms — page transition
        const val Long = 500     // ms — modal open
        const val Splash = 2400  // ms — splash total (trimmed from 3200, no dead hold)

        // Ambient loop durations (slow, calm — never compete with interaction).
        const val PulseGlow = 2600   // nav active pulse breathe
        const val Sheen = 6000       // pro-tip sheen sweep
        const val Shimmer = 1700     // active-step shimmer bar
        const val ShimmerBar = 3400  // history accent-bar shimmer
        const val Flicker = 3500     // pro-tip bulb flicker
        const val CometPulse = 1900  // progress-ring comet
        const val AuraBreathe = 3400 // compiling aura scale breathe
        const val AuraSpin = 9000    // compiling aura rotation
    }

    object Easings {
        val Standard: Easing = FastOutSlowInEasing          // most UI motion
        val Decelerate: Easing = LinearOutSlowInEasing      // entering screen
        val Accelerate: Easing = FastOutLinearInEasing      // exiting screen
        val Emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        // Emphasized-decelerate (M3): snappy start, soft settle. Ring pop-in, entrances.
        val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    }

    object Springs {
        /** Bounce-y spring for chip add/remove — feels playful. */
        val bouncy = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        /** Snappy spring for modal open/close — no overshoot, just speed. */
        val snappy = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )

        /** Gentle spring for parallax / scroll-linked motion. */
        val gentle = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    }
}
