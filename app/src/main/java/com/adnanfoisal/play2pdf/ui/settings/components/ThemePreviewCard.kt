package com.adnanfoisal.play2pdf.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.designsystem.components.AppIcon
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Theme picker card: horizontal row of 13 theme thumbnails.
 *
 * Per Asset J, each theme should render a real PNG preview. Until Asset J
 * is delivered, we render a brand-tinted icon as the preview (per §3.2
 * fallback: "Use AppIcons.Pdf at 56dp inside a brand-tinted rounded rectangle").
 *
 * TODO: replace the placeholder Box below with an Image(painterResource(id = R.drawable.pdf_theme_<theme>.png)).
 */
@Composable
fun ThemePreviewCard(
    themes: List<PdfTheme>,
    selected: PdfTheme,
    onSelect: (PdfTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Pick a theme",
                color = BrandColors.TextSecondary,
                style = AppType.label
            )
            Spacer(Modifier.height(Spacing.sm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(themes) { theme ->
                    ThemeThumbnail(
                        theme = theme,
                        isSelected = theme == selected,
                        onClick = { onSelect(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeThumbnail(
    theme: PdfTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.95f)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp, 96.dp)
                .clip(AppShape.small)
                .background(if (isSelected) BrandColors.Brand else BrandColors.Surface2)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            // TODO: replace with Image(painterResource(R.drawable.pdf_theme_<theme>.png))
            // when Asset J is delivered.
            AppIcon(
                icon = AppIcons.Pdf,
                contentDescription = theme.displayName,
                size = 32.dp,
                tint = if (isSelected) BrandColors.PureWhite else BrandColors.Brand
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = theme.displayName,
            color = if (isSelected) BrandColors.Brand else BrandColors.TextSecondary,
            style = AppType.caption,
            maxLines = 1
        )
    }
}
