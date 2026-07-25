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
    Canvas(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        val w = size.width
        val h = size.height
        val padX = w * 0.14f
        val contentW = w - padX * 2
        var y = h * 0.16f
        val lineH = h * 0.028f
        val gap = h * 0.052f

        fun bar(fraction: Float, color: Color, thickness: Float, radius: Float = thickness / 2f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(padX, y),
                size = Size(contentW * fraction, thickness),
                cornerRadius = CornerRadius(radius, radius)
            )
        }

        // Title (heading color, thicker)
        bar(0.72f, pal.heading, lineH * 1.9f)
        y += lineH * 1.9f + gap * 0.5f
        // Heading rule (accent)
        bar(0.34f, pal.accent, lineH * 0.9f)
        y += lineH * 0.9f + gap

        // Body lines
        repeat(4) { i ->
            val frac = when (i) {
                3 -> 0.55f
                else -> 0.92f - (i % 2) * 0.06f
            }
            bar(frac, pal.body.copy(alpha = 0.55f), lineH)
            y += lineH + gap * 0.62f
        }

        y += gap * 0.4f
        // A subheading in heading color
        bar(0.46f, pal.heading.copy(alpha = 0.9f), lineH * 1.2f)
        y += lineH * 1.2f + gap * 0.6f
        // Two more body lines
        repeat(2) {
            bar(if (it == 1) 0.62f else 0.88f, pal.body.copy(alpha = 0.5f), lineH)
            y += lineH + gap * 0.62f
        }

        // Footer rule near bottom
        drawRoundRect(
            color = pal.accent.copy(alpha = 0.8f),
            topLeft = Offset(padX, h - h * 0.12f),
            size = Size(contentW, lineH * 0.8f),
            cornerRadius = CornerRadius(lineH * 0.4f, lineH * 0.4f)
        )
    }
}

