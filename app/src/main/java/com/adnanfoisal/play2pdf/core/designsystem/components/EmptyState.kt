package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Empty-state composable: icon + title + subtitle + optional CTA.
 *
 * Source spec: v2.0 §9 (mentioned alongside other components).
 *
 * Used by HistoryScreen (empty history) and CompileScreen (no playlists
 * yet, no topics yet). The icon gets a subtle infinite pulse for "empty
 * state delight" per Phase E micro-interactions.
 *
 * Usage:
 *   EmptyState(
 *       icon = Icons.R.Inbox, // TODO: replace with AppIcons.Inbox
 *       title = "No PDFs yet",
 *       subtitle = "Your compiled study guides will appear here."
 *   )
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    cta: @Composable (() -> Unit)? = null
) {
    // Subtle pulse on the icon — infinite alpha 0.6 → 1.0 → 0.6 over 1.8s.
    val transition = rememberInfiniteTransition(label = "emptyPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = Motion.Easings.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .alpha(pulse)
                .clip(AppShape.large)
                .background(BrandColors.Surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandColors.Brand,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = title,
            color = BrandColors.TextPrimary,
            style = AppType.title2,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = BrandColors.TextSecondary,
            style = AppType.bodySmall,
            textAlign = TextAlign.Center
        )
        if (cta != null) {
            cta()
        }
    }
}
