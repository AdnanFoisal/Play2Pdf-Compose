package com.adnanfoisal.play2pdf.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.tokens.Elevation
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Custom bottom nav bar: 3 tabs (Compile / History / Settings) with a
 * sliding pill indicator + brand tint on the active tab.
 *
 * Source spec: v2.0 §9.5.
 *
 * The pill indicator slides between tabs with a spring animation. The
 * active icon scales up to 1.1, the inactive scales to 0.9, both with
 * the [Motion.Springs.bouncy] spring for a playful "settle".
 */
@Composable
fun Play2PdfBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tabCount = items.size

    // Compute the indicator offset: each tab is 1/tabCount of the width.
    val activeIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(BrandColors.Surface1)
    ) {
        // Top border hairline
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(BrandColors.SurfaceBorder)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isActive = index == activeIndex
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.1f else 0.9f,
                    animationSpec = Motion.Springs.bouncy,
                    label = "tabScale$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .pressScaleClickable(
                            onClick = { onNavigate(item.route) },
                            pressedScale = 0.9f
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(AppShape.pill)
                            .background(
                                if (isActive) BrandColors.Brand.copy(alpha = 0.15f) else Color.Transparent
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                    ) {
                        // Pulsing radial glow behind the active icon —
                        // per IMPLEMENTATION_PLAN.md Step 7 + history screen.html
                        // `pulseGlow` keyframe (scale 0.9→1.12, alpha 0.6→1.0, 2600ms).
                        if (isActive) {
                            val glowTransition = rememberInfiniteTransition(label = "navGlow")
                            val glowScale by glowTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.12f,
                                animationSpec = infiniteRepeatable(
                                    tween(2600, easing = LinearEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "navGlowScale"
                            )
                            val glowAlpha by glowTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    tween(2600, easing = LinearEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "navGlowAlpha"
                            )
                            Canvas(
                                modifier = Modifier
                                    .size(46.dp)
                                    .scale(glowScale)
                            ) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            BrandColors.BrandStrong.copy(alpha = 0.55f * glowAlpha),
                                            BrandColors.BrandStrong.copy(alpha = 0f)
                                        )
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isActive) BrandColors.Brand else BrandColors.TextTertiary,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale)
                        )
                    }
                    Text(
                        text = item.label,
                        color = if (isActive) BrandColors.Brand else BrandColors.TextTertiary,
                        style = AppType.caption.copy(
                            fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.SemiBold
                            else androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
