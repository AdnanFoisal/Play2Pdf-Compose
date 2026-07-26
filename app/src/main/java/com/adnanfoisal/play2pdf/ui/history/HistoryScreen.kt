package com.adnanfoisal.play2pdf.ui.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adnanfoisal.play2pdf.core.designsystem.components.EmptyState
import com.adnanfoisal.play2pdf.core.designsystem.components.GhostIconButton
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.historyAtmosphere
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.domain.model.PdfHistory
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.theme.HistoryCardAccents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History screen — the user's compiled-PDF library.
 *
 * - Tap card → opens PDF in system viewer
 * - 3-dot menu → dropdown with Delete / Rename
 * - No swipe-to-delete
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State for rename dialog
    var renameTarget by remember { mutableStateOf<PdfHistory?>(null) }
    var renameText by remember { mutableStateOf("") }

    // State for delete confirmation
    var deleteTarget by remember { mutableStateOf<PdfHistory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.HistoryBg)
            .historyAtmosphere()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(14.dp))

            // Header — title + subtitle + icon buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Study Guides",
                        color = BrandColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Your compiled PDF library",
                        color = BrandColors.TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostIconButton(
                        icon = Icons.Filled.Search,
                        contentDescription = "Search",
                        onClick = { /* TODO: expand search field */ },
                        tint = BrandColors.TextSecondary
                    )
                    GhostIconButton(
                        icon = Icons.Filled.Tune,
                        contentDescription = "Filter",
                        onClick = { /* TODO: open filter sheet */ },
                        tint = BrandColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (state.items.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = AppIcons.Inbox,
                    title = "No study guides yet",
                    subtitle = "Compile your first PDF from the Compile tab",
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                        val accentPair = HistoryCardAccents[index % HistoryCardAccents.size]
                        StaggeredHistoryCard(
                            index = index,
                            item = item,
                            accentTop = accentPair.first,
                            accentBot = accentPair.second,
                            onCardClick = { openPdf(context, item) },
                            onDelete = { deleteTarget = item },
                            onRename = {
                                renameTarget = item
                                renameText = item.subject
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Study Guide?", color = BrandColors.TextPrimary) },
            text = {
                Text(
                    "\"${deleteTarget!!.subject}\" will be permanently removed.",
                    color = BrandColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(deleteTarget!!)
                    deleteTarget = null
                }) {
                    Text("Delete", color = BrandColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = BrandColors.TextSecondary)
                }
            },
            containerColor = BrandColors.Surface1,
            titleContentColor = BrandColors.TextPrimary,
            textContentColor = BrandColors.TextSecondary
        )
    }

    // Rename dialog
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Study Guide", color = BrandColors.TextPrimary) },
            text = {
                PremiumTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = "Subject"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        viewModel.rename(renameTarget!!, renameText.trim())
                    }
                    renameTarget = null
                }) {
                    Text("Save", color = BrandColors.Brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel", color = BrandColors.TextSecondary)
                }
            },
            containerColor = BrandColors.Surface1,
            titleContentColor = BrandColors.TextPrimary,
            textContentColor = BrandColors.TextSecondary
        )
    }
}

private fun openPdf(context: Context, item: PdfHistory) {
    val uri = item.pdfUri
    if (uri.isNullOrBlank()) {
        Toast.makeText(context, "PDF file not available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri.toUri(), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer app installed", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun StaggeredHistoryCard(
    index: Int,
    item: PdfHistory,
    accentTop: Color,
    accentBot: Color,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    val delayMs = (index * 80 + 100).coerceAtMost(800)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.Durations.Long, easing = Motion.Easings.EmphasizedDecelerate)) +
            slideInVertically(
                animationSpec = tween(Motion.Durations.Long, easing = Motion.Easings.EmphasizedDecelerate),
                initialOffsetY = { it / 8 }
            )
    ) {
        HistoryCard(
            item = item,
            accentTop = accentTop,
            accentBot = accentBot,
            onCardClick = onCardClick,
            onDelete = onDelete,
            onRename = onRename
        )
    }
}

@Composable
private fun HistoryCard(
    item: PdfHistory,
    accentTop: Color,
    accentBot: Color,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateLabel = remember(item.createdAtEpochMs) {
        val diff = System.currentTimeMillis() - item.createdAtEpochMs
        val dayMs = 86_400_000L
        when {
            diff < dayMs -> "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.createdAtEpochMs))
            diff < 2 * dayMs -> "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.createdAtEpochMs))
            else -> dateFormat.format(Date(item.createdAtEpochMs))
        }
    }

    // State for dropdown menu
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Surface3)
            .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .pressScaleClickable(onClick = onCardClick)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(4.dp)
                .background(Brush.verticalGradient(colors = listOf(accentTop, accentBot)))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Card main — title + theme + stats
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject,
                    color = BrandColors.TextPrimary,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.theme.displayName + " Theme",
                    color = BrandColors.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                // Stats row — topics + videos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Topic,
                        contentDescription = null,
                        tint = BrandColors.TextQuaternary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.topicCount ?: item.topics.size} topics",
                        color = BrandColors.TextTertiary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("+", color = BrandColors.TextQuaternary, fontSize = 12.sp)
                    Icon(
                        imageVector = AppIcons.Playlist,
                        contentDescription = null,
                        tint = BrandColors.TextQuaternary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.videoCount ?: item.playlistUrls.size} videos",
                        color = BrandColors.TextTertiary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Card side — 3-dot menu + PDF icon + date
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(74.dp)
            ) {
                // 3-dot menu with dropdown
                Box {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = BrandColors.TextTertiary,
                        modifier = Modifier
                            .size(22.dp)
                            .pressScaleClickable(onClick = { menuExpanded = true })
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = BrandColors.Surface2
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = BrandColors.TextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = BrandColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = BrandColors.Error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = BrandColors.Error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
                PdfSvgIcon(accentTop = accentTop, accentBot = accentBot)
                Text(
                    text = dateLabel,
                    color = BrandColors.TextQuaternary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * PDF document SVG icon — drawn via Canvas.
 */
@Composable
private fun PdfSvgIcon(accentTop: Color, accentBot: Color) {
    Canvas(modifier = Modifier.size(46.dp, 58.dp)) {
        val sx = size.width / 46f
        val sy = size.height / 58f
        val body = Path().apply {
            moveTo(6f * sx, 1f * sy)
            lineTo(31f * sx, 1f * sy)
            lineTo(45f * sx, 15f * sy)
            lineTo(45f * sx, 52f * sy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 40f * sx, top = 47f * sy,
                    right = 50f * sx, bottom = 57f * sy
                ),
                startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(6f * sx, 57f * sy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 1f * sx, top = 47f * sy,
                    right = 11f * sx, bottom = 57f * sy
                ),
                startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(1f * sx, 6f * sy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 1f * sx, top = 1f * sy,
                    right = 11f * sx, bottom = 11f * sy
                ),
                startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            close()
        }
        drawPath(
            path = body,
            brush = Brush.verticalGradient(colors = listOf(accentTop, accentBot))
        )
        drawPath(
            path = body,
            color = Color.White.copy(alpha = 0.14f),
            style = Stroke(width = 1f)
        )
        val fold = Path().apply {
            moveTo(31f * sx, 1f * sy)
            lineTo(31f * sx, 11f * sy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 31f * sx, top = 7f * sy,
                    right = 39f * sx, bottom = 15f * sy
                ),
                startAngleDegrees = -90f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(45f * sx, 15f * sy)
            close()
        }
        drawPath(path = fold, color = Color.White.copy(alpha = 0.22f))
        val labelY = 32f * sy
        val labelW = 18f * sx
        val labelH = 1.6f * sy
        repeat(3) { i ->
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset((14f + i * 6f) * sx, labelY + i * 4f * sy),
                size = androidx.compose.ui.geometry.Size(labelW - i * 3f * sx, labelH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(labelH, labelH)
            )
        }
    }
}
