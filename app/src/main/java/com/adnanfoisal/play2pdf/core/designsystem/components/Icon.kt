package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Generic icon wrapper: takes an [ImageVector] (either a custom
 * [com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons] entry or
 * a Material fallback) and renders it at the right size with the right
 * tint from the current theme.
 *
 * Until Design Agent delivers Asset F (custom icon set), pass Material
 * icons here with a `// TODO: replace with AppIcons.*` comment.
 *
 * Usage:
 *   AppIcon(icon = Icons.R.PlayArrow, contentDescription = "Compile")
 *   AppIcon(icon = Icons.R.Settings, size = 32.dp, tint = BrandColors.Brand)
 */
@Composable
fun AppIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
