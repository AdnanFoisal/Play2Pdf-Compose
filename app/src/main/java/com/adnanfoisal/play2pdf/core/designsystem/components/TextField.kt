package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Premium text field: filled style, floating label, animated border,
 * error state with supporting text.
 *
 * Source spec: v2.0 §9 (mentioned alongside other components).
 *
 * Usage:
 *   PremiumTextField(
 *       value = url,
 *       onValueChange = { url = it },
 *       label = "YouTube Playlist URL",
 *       placeholder = "https://www.youtube.com/playlist?list=…"
 *   )
 *
 * Error state:
 *   PremiumTextField(value = ..., onValueChange = ..., label = ..., error = "Invalid URL")
 */
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    error: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> BrandColors.Error
            value.isNotEmpty() -> BrandColors.Brand.copy(alpha = 0.6f)
            else -> BrandColors.SurfaceBorder
        },
        animationSpec = tween(Motion.Durations.Micro, easing = Motion.Easings.Standard),
        label = "borderColor"
    )

    val supportingColor = when {
        isError -> BrandColors.Error
        else -> BrandColors.TextTertiary
    }

    val supportingMsg = when {
        isError && error != null -> error
        supportingText != null -> supportingText
        else -> null
    }

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clip(AppShape.small)
                .background(BrandColors.Surface0)
                .border(width = 1.dp, color = borderColor, shape = AppShape.small)
        ) {
            // Hint when empty: prefer the placeholder, fall back to the label.
            // (Previously `if (value.isEmpty()) placeholder else label` sat
            // INSIDE this empty-branch — the else was unreachable, so fields
            // with only a label rendered as blank boxes with no hint.)
            if (value.isEmpty()) {
                Text(
                    text = placeholder.ifEmpty { label },
                    color = BrandColors.TextTertiary,
                    style = AppType.bodySmall,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Spacing.md)
                )
            }
            // Label that floats above when there's content
            if (value.isNotEmpty()) {
                Text(
                    text = label,
                    color = if (isError) BrandColors.Error else BrandColors.TextSecondary,
                    style = AppType.caption,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = Spacing.md, top = 6.dp)
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(if (value.isNotEmpty()) Alignment.BottomStart else Alignment.CenterStart)
                    .fillMaxWidth()
                    .padding(start = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    textStyle = AppType.body.copy(color = BrandColors.TextPrimary),
                    cursorBrush = SolidColor(BrandColors.Brand),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = keyboardType,
                        capitalization = capitalization
                    ),
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            top = if (value.isNotEmpty()) 18.dp else 0.dp,
                            bottom = if (value.isNotEmpty()) 8.dp else 0.dp
                        ),
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            // Already rendered above as floating label
                        }
                        inner()
                    }
                )
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(end = Spacing.sm)) {
                        trailingIcon()
                    }
                }
            }
        }

        if (supportingMsg != null) {
            Text(
                text = supportingMsg,
                color = supportingColor,
                style = AppType.caption,
                modifier = Modifier.padding(start = Spacing.sm, top = 4.dp)
            )
        }
    }
}
