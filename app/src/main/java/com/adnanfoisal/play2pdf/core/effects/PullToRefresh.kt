package com.adnanfoisal.play2pdf.core.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import androidx.compose.material3.Icon
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.tokens.Motion
import kotlinx.coroutines.launch

/**
 * Pull-to-refresh wrapper: shows a rotating logo mark as the user pulls
 * down, triggers [onRefresh] when the pull exceeds 96dp.
 *
 * Per Phase E micro-interactions:
 *  - Custom indicator (rotates the logo mark as the user pulls).
 *  - Haptic feedback on trigger (handled by the caller via HapticsManager).
 *
 * Usage:
 *   PullToRefresh(
 *       isRefreshing = state.isRefreshing,
 *       onRefresh = { viewModel.refresh() }
 *   ) {
 *       LazyColumn { ... }
 *   }
 *
 * NOTE: This is a minimal hand-rolled implementation. For a more polished
 * experience, swap to androidx.compose.material3.pulltorefresh.PullToRefreshBox
 * once we upgrade to Compose Material3 1.7+ (already in the BOM but
 * currently flagged experimental).
 */
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pullProgress = remember { Animatable(0f) }
    var dragAccum by remember { mutableStateOf(0f) }
    val thresholdPx = with(density) { 96.dp.toPx() }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullProgress.animateTo(0f, tween(Motion.Durations.Medium, easing = Motion.Easings.Standard))
            dragAccum = 0f
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onDragEnd = {
                        scope.launch {
                            if (dragAccum >= thresholdPx && !isRefreshing) onRefresh()
                            pullProgress.animateTo(
                                if (isRefreshing) 1f else 0f,
                                tween(Motion.Durations.Medium, easing = Motion.Easings.Standard)
                            )
                            dragAccum = 0f
                        }
                    }
                ) { _, dragAmount ->
                    if (dragAmount > 0) {
                        dragAccum += dragAmount
                        scope.launch {
                            pullProgress.snapTo((dragAccum / thresholdPx).coerceIn(0f, 1f))
                        }
                    }
                }
            }
    ) {
        // Indicator (logo mark rotating)
        if (pullProgress.value > 0f || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Sparkle,
                    contentDescription = null,
                    tint = BrandColors.Brand,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(pullProgress.value * 360f)
                )
            }
        }
        content()
    }
}
