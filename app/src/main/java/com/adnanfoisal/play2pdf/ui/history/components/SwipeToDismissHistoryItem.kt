package com.adnanfoisal.play2pdf.ui.history.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.domain.model.PdfHistory
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History list item with swipe-to-dismiss.
 *
 * Per Phase E micro-interactions: "swipe-to-dismiss" with a confirm
 * threshold (50% of width). After threshold, the item animates out and
 * [onDelete] is called.
 *
 * Long-press opens a context menu (handled by the parent screen via
 * [onLongPress]).
 */
@Composable
fun SwipeToDismissHistoryItem(
    item: PdfHistory,
    onClick: (PdfHistory) -> Unit,
    onLongPress: (PdfHistory) -> Unit,
    onDelete: (PdfHistory) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(Motion.Durations.Medium, easing = Motion.Easings.Standard),
        label = "swipeOffset"
    )

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxWidth().height(96.dp)) {
        // Delete background — revealed on swipe right.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(AppShape.large)
                .background(BrandColors.Error),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.padding(end = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = null,
                    tint = BrandColors.PureWhite,
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "Delete", color = BrandColors.PureWhite, style = AppType.label)
            }
        }

        // Foreground card — draggestures + animated offset.
        PremiumCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffsetX }
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Threshold: 50% of card width
                            if (kotlin.math.abs(offsetX) > size.width * 0.5f) {
                                onDelete(item)
                            }
                            offsetX = 0f
                        }
                    ) { _, dragAmount ->
                        offsetX += dragAmount
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AppShape.small)
                        .background(BrandColors.Surface3),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Pdf,
                        contentDescription = null,
                        tint = BrandColors.Brand,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subject,
                        color = BrandColors.TextPrimary,
                        style = AppType.title3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = "${item.topics.size} topics · ${item.playlistUrls.size} playlists",
                        color = BrandColors.TextSecondary,
                        style = AppType.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = dateFormat.format(Date(item.createdAtEpochMs)),
                        color = BrandColors.TextTertiary,
                        style = AppType.caption
                    )
                }
                Icon(
                    imageVector = AppIcons.More,
                    contentDescription = "More options",
                    tint = BrandColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
