package com.adnanfoisal.play2pdf.tokens

import androidx.compose.ui.unit.dp

/**
 * 8-pt grid spacing tokens.
 *
 * Every padding, margin, and gap in the app goes through [Spacing]. No
 * hardcoded dp values in composables. The 8-pt grid means most values
 * are multiples of 8, with 4dp as the half-step for tight layouts.
 *
 * Usage:
 *   Spacer(modifier = Modifier.height(Spacing.md))
 *   Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
 */
object Spacing {
    val none = 0.dp
    val xs = 4.dp     // half-step (icon gaps, tight chips)
    val sm = 8.dp     // 1 step
    val smMd = 12.dp  // 1.5 steps (rare)
    val md = 16.dp    // 2 steps — default card padding
    val lg = 24.dp    // 3 steps — section spacing
    val xl = 32.dp    // 4 steps — header padding
    val xxl = 48.dp   // 6 steps — screen-edge breathing
    val xxxl = 64.dp  // 8 steps — splash logo container

    // Convenience helpers
    val cardPadding = md
    val screenPadding = lg
    val sectionSpacing = lg
    val itemSpacing = sm
}
