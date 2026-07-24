package com.adnanfoisal.play2pdf.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 3-step radius scale: small (8dp) / medium (12dp) / large (20dp) +
 * pill (999dp) for chips and full-round elements.
 *
 * The 3-step limit forces visual consistency — if you reach for a
 * 16dp corner, you should either use 12dp (closer to "card") or 20dp
 * (closer to "blob"). The system says no.
 *
 * Usage in composables:
 *   PremiumCard(shape = AppShape.large) { ... }
 *   AnimatedChip(shape = AppShape.pill) { ... }
 */
object AppShape {
    val small: Shape = RoundedCornerShape(8.dp)   // inputs, list items
    val medium: Shape = RoundedCornerShape(12.dp) // small cards
    val large: Shape = RoundedCornerShape(20.dp)  // hero cards, modals
    val pill: Shape = RoundedCornerShape(999.dp)  // chips, FABs, toggles

    /** Material 3 [Shapes] instance for components that consume the
     *  default shape system (e.g. Material3 Buttons that we don't override). */
    val material: Shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
}
