package com.adnanfoisal.play2pdf.ui.compile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing

/**
 * A horizontal, scrollable row of live PDF theme previews. Each preview is a
 * real Compose-drawn miniature page reflecting the theme's true palette, so
 * the user can see what they'll get before compiling. Used on the Home tab and
 * inside the theme picker sheet.
 *
 * @param compact when true renders smaller cards (Home inline row); when false
 *                renders larger cards (picker sheet).
 */
@Composable
fun PdfThemePreviewRow(
    themes: List<PdfTheme>,
    selected: PdfTheme,
    onSelect: (PdfTheme) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    val pageW = if (compact) 92.dp else 116.dp
    val pageH = if (compact) 122.dp else 154.dp
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smMd),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xs)
    ) {
        items(themes, key = { it.name }) { theme ->
            PdfThemeMiniPreview(
                theme = theme,
                selected = theme == selected,
                onSelect = { onSelect(theme) },
                pageWidth = pageW,
                pageHeight = pageH
            )
        }
    }
}

/**
 * One mini PDF "page" preview card. Selected state gets a violet ring + lift.
 */
@Composable
fun PdfThemeMiniPreview(
    theme: PdfTheme,
    selected: Boolean,
    onSelect: () -> Unit,
    pageWidth: androidx.compose.ui.unit.Dp,
    pageHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val pal = theme.palette()
    val ringColor by animateColorAsState(
        targetValue = if (selected) BrandColors.Brand else BrandColors.SurfaceBorder,
        animationSpec = tween(Motion.Durations.Short),
        label = "themeRing"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) 10.dp else 2.dp,
        animationSpec = tween(Motion.Durations.Short),
        label = "themeLift"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(pageWidth)
            .pressScaleClickable(onClick = onSelect, pressedScale = 0.95f)
    ) {
        Box(
            modifier = Modifier
                .size(pageWidth, pageHeight)
                .shadow(
                    elevation = lift,
                    shape = AppShape.medium,
                    clip = false,
                    spotColor = if (selected) BrandColors.BrandStrong else Color.Black,
                    ambientColor = Color.Black
                )
                .clip(AppShape.medium)
                .background(pal.page)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = ringColor,
                    shape = AppShape.medium
                )
        ) {
            MiniPageContent(pal)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = theme.displayName,
            color = if (selected) BrandColors.Brand else BrandColors.TextSecondary,
            style = AppType.caption.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}

/**
 * Draws a faithful miniature of a document page: title, heading rule, body
 * lines, an accent tag, and a footer rule — all in the theme's palette.
 */
@Composable
private fun MiniPageContent(pal: PdfThemePalette) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp, horizontal = 10.dp)
    ) {
        val width = size.width
        val height = size.height
        val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        // Header Title (Accent colored or Heading colored)
        drawRoundRect(
            color = pal.heading,
            topLeft = Offset(0f, 0f),
            size = Size(width * 0.7f, 8.dp.toPx()),
            cornerRadius = corner
        )

        // Accent divider
        drawRoundRect(
            color = pal.accent,
            topLeft = Offset(0f, 14.dp.toPx()),
            size = Size(width * 0.3f, 2.dp.toPx()),
            cornerRadius = corner
        )

        // Paragraph 1 (Body colored)
        var currentY = 24.dp.toPx()
        val lineHeight = 4.dp.toPx()
        val lineSpacing = 3.dp.toPx()
        val p1Lines = listOf(1.0f, 0.9f, 0.95f, 0.6f)
        for (wRatio in p1Lines) {
            drawRoundRect(
                color = pal.body.copy(alpha = 0.6f),
                topLeft = Offset(0f, currentY),
                size = Size(width * wRatio, lineHeight),
                cornerRadius = corner
            )
            currentY += lineHeight + lineSpacing
        }

        // Subheading
        currentY += 6.dp.toPx()
        drawRoundRect(
            color = pal.heading.copy(alpha = 0.8f),
            topLeft = Offset(0f, currentY),
            size = Size(width * 0.5f, 6.dp.toPx()),
            cornerRadius = corner
        )
        currentY += 6.dp.toPx() + 6.dp.toPx()

        // Bullet points
        val bulletRadius = 1.5.dp.toPx()
        for (i in 0..2) {
            drawCircle(
                color = pal.accent,
                radius = bulletRadius,
                center = Offset(bulletRadius, currentY + (lineHeight / 2))
            )
            drawRoundRect(
                color = pal.body.copy(alpha = 0.6f),
                topLeft = Offset(8.dp.toPx(), currentY),
                size = Size(width * 0.75f, lineHeight),
                cornerRadius = corner
            )
            currentY += lineHeight + lineSpacing + 2.dp.toPx()
        }
        
        // Footer divider
        drawRoundRect(
            color = pal.accent.copy(alpha = 0.5f),
            topLeft = Offset(0f, height - 2.dp.toPx()),
            size = Size(width, 1.dp.toPx()),
            cornerRadius = corner
        )
    }
}

