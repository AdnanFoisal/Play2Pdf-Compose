package com.adnanfoisal.play2pdf.ui.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.domain.model.ConnectionStatus
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Live backend status indicator (Online / Offline / Checking).
 *
 * Pulses while checking; otherwise a solid colored dot.
 */
@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus) {
    val dotColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.Online -> BrandColors.Success
            ConnectionStatus.Offline -> BrandColors.Error
            ConnectionStatus.Checking -> BrandColors.Warning
        },
        label = "statusDot"
    )
    val label = when (status) {
        ConnectionStatus.Online -> "Online"
        ConnectionStatus.Offline -> "Offline"
        ConnectionStatus.Checking -> "Checking…"
    }
    Row(
        modifier = Modifier
            .clip(AppShape.pill)
            .background(BrandColors.Surface2)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (status == ConnectionStatus.Checking) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.dp,
                color = dotColor
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Text(text = label, color = BrandColors.TextPrimary, style = AppType.caption)
    }
}
