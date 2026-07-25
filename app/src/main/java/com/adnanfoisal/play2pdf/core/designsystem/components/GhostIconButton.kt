package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Shared 42dp circular ghost icon button.
 *
 * The mockups use this exact pattern in the History header, the Compiling
 * back button, and various row actions. Previously it was duplicated inline
 * in each screen — this extracts the single source of truth.
 *
 * Spec: 42dp circle, rgba(255,255,255,0.05) bg, 1px rgba(255,255,255,0.08)
 * border, press scale 0.92.
 */
@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    tint: Color = BrandColors.TextPrimary
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .pressScaleClickable(
                onClick = onClick,
                pressedScale = 0.92f,
                onClickLabel = contentDescription
            )
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
