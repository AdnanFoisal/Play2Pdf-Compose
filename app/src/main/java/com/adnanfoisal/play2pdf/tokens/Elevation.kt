package com.adnanfoisal.play2pdf.tokens

import androidx.compose.ui.unit.dp

/**
 * Layered elevation tokens.
 *
 * Material 3's elevation system is built for light themes where shadow
 * is the primary depth cue. On a dark theme, shadows disappear into the
 * background — so we layer TWO cues: shadow + subtle border + slightly
 * lighter surface color.
 *
 * These values are used both as `Modifier.shadow(elevation = ...)` AND
 * as the `tonalElevation` parameter on Surface, which is what gives
 * Material3 cards their automatic color shift.
 */
object Elevation {
    val none = 0.dp      // flat (no card)
    val card = 2.dp      // PremiumCard default
    val cardHover = 6.dp // PremiumCard hover state (cursor / focus)
    val modal = 12.dp    // ModalBottomSheet, dialogs
    val popover = 8.dp   // dropdowns, menus
}
