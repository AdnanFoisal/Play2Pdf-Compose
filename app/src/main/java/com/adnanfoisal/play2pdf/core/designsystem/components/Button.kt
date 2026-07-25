package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adnanfoisal.play2pdf.core.effects.neonGlow
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.tokens.Elevation
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Primary CTA button — gradient background, brand glow, press scale 0.97,
 * loading state with spinner, ghost variant.
 *
 * Source spec: v2.0 §9.1.
 *
 * Usage:
 *   PrimaryButton(text = "Compile Study Guide", onClick = { vm.compile() })
 *   PrimaryButton(text = "Save", loading = true, onClick = { ... })
 *   PrimaryButton(text = "Cancel", variant = PrimaryButtonVariant.Ghost, onClick = { ... })
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: PrimaryButtonVariant = PrimaryButtonVariant.Primary
) {
    val bg: Brush
    val contentColor: Color
    val glow: Boolean

    when (variant) {
        PrimaryButtonVariant.Primary -> {
            bg = Brush.linearGradient(
                listOf(
                    BrandColors.BrandDeep,
                    BrandColors.BrandMid,
                    BrandColors.BrandGradEnd
                )
            )
            contentColor = BrandColors.PureWhite
            glow = enabled
        }
        PrimaryButtonVariant.Ghost -> {
            bg = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            contentColor = BrandColors.TextPrimary
            glow = false
        }
    }

    val shape = AppShape.button

    Box(
        modifier = modifier
            .height(56.dp)
            .then(if (glow) Modifier.neonGlow(radius = 12.dp, alpha = 0.45f) else Modifier)
            .background(bg, shape)
            .then(
                if (enabled && !loading) {
                    Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.97f)
                } else {
                    Modifier.alpha(if (enabled) 1f else 0.5f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        ) {
            AnimatedVisibility(
                visible = loading,
                enter = scaleIn(animationSpec = androidx.compose.animation.core.tween(Motion.Durations.Micro)) +
                        fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            }
            AnimatedVisibility(
                visible = !loading && icon != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = text,
                color = contentColor,
                style = AppType.button,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class PrimaryButtonVariant { Primary, Ghost }
