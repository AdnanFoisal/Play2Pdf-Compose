package com.adnanfoisal.play2pdf.ui.compile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButton
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.ui.compile.components.PdfThemePreviewRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adnanfoisal.play2pdf.core.effects.homeAtmosphere
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.model.Playlist
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Compile screen — the main "create a study guide" surface.
 *
 * Rewritten to match `mock assests/Home screen.html` per
 * IMPLEMENTATION_PLAN.md Step 4. All [CompileUiEvent] wiring is preserved;
 * the URL input, topic input, subject/author fields, and theme picker move
 * into dialogs triggered by "+ Add Playlist" / "+ Add Topic" / "Advanced" /
 * "Change" buttons so no existing input capability is lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompileScreen(
    onCompileRequest: () -> Unit,
    viewModel: CompileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(CompileUiEvent.DismissError)
        }
    }

    // Dialog state — which sheet (if any) is open.
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAdvancedDialog by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Bg)
            .homeAtmosphere()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Spacing.lgMinus)
        ) {
            Spacer(Modifier.height(Spacing.mdMinus))

            // Header — greeting + gold crown button
            GreetingHeader(userName = state.userName)
            Spacer(Modifier.height(18.dp))

            // Stats card — total compilations + sparkline
            StatsCard(
                stats = state.stats,
                onClick = { showStatsSheet = true }
            )
            Spacer(Modifier.height(18.dp))

            // Section label
            SectionLabel(text = "Your Workspace")
            Spacer(Modifier.height(10.dp))

            // Playlists card
            PlaylistsCard(
                playlists = state.playlists,
                isFetchingMeta = state.isFetchingMeta,
                onAddPlaylist = { showAddPlaylistDialog = true },
                onRemovePlaylist = { viewModel.onEvent(CompileUiEvent.RemovePlaylist(it)) }
            )
            Spacer(Modifier.height(14.dp))

            // Topics card
            TopicsCard(
                topics = state.topics,
                onAddTopic = { showAddTopicDialog = true },
                onRemoveTopic = { viewModel.onEvent(CompileUiEvent.RemoveTopic(it)) },
                onExtractTopics = { viewModel.onEvent(CompileUiEvent.ExtractTopics) },
                isExtracting = state.isExtractingTopics
            )
            Spacer(Modifier.height(14.dp))

            // PDF theme section — live previews + selector row
            SectionLabel(text = "PDF Theme")
            Spacer(Modifier.height(10.dp))
            PdfThemePreviewRow(
                themes = PdfTheme.entries.toList(),
                selected = state.selectedTheme,
                onSelect = { viewModel.onEvent(CompileUiEvent.ThemeChanged(it)) },
                compact = true
            )
            Spacer(Modifier.height(12.dp))
            PdfThemeRow(
                theme = state.selectedTheme,
                onChange = { showThemeDialog = true }
            )
            Spacer(Modifier.height(Spacing.mdMinus))

            // Advanced toggle — subject/author fields
            AdvancedRow(onOpen = { showAdvancedDialog = true }, subject = state.subject)
            Spacer(Modifier.height(18.dp))

            // Compile button
            CompileButton(
                enabled = state.canCompile,
                onClick = {
                    viewModel.prepareForCompilation()
                    onCompileRequest()
                }
            )

            Spacer(Modifier.height(18.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // --- Dialogs ---

    if (showAddPlaylistDialog) {
        PlaylistUrlDialog(
            initialValue = state.playlistUrlInput,
            onValueChange = { viewModel.onEvent(CompileUiEvent.PlaylistUrlChanged(it)) },
            onConfirm = {
                viewModel.onEvent(CompileUiEvent.AddPlaylist(state.playlistUrlInput))
                showAddPlaylistDialog = false
            },
            onDismiss = { showAddPlaylistDialog = false }
        )
    }

    if (showAddTopicDialog) {
        TopicInputDialog(
            initialValue = state.topicInput,
            onValueChange = { viewModel.onEvent(CompileUiEvent.TopicInputChanged(it)) },
            onConfirm = {
                viewModel.onEvent(CompileUiEvent.AddTopic(state.topicInput))
                showAddTopicDialog = false
            },
            onDismiss = { showAddTopicDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            themes = PdfTheme.entries.toList(),
            selected = state.selectedTheme,
            onSelect = {
                viewModel.onEvent(CompileUiEvent.ThemeChanged(it))
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAdvancedDialog) {
        AdvancedDialog(
            subject = state.subject,
            author = state.author,
            onSubjectChange = { viewModel.onEvent(CompileUiEvent.SubjectChanged(it)) },
            onAuthorChange = { viewModel.onEvent(CompileUiEvent.AuthorChanged(it)) },
            onDismiss = { showAdvancedDialog = false }
        )
    }

    if (showStatsSheet) {
        StatsFullScreenDialog(
            stats = state.stats,
            onDismiss = { showStatsSheet = false }
        )
    }
}

// --- Header ---

@Composable
private fun GreetingHeader(userName: String) {
    val name = userName.trim()
    val hour = java.time.LocalTime.now().hour
    val timeGreeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Hello" // Late night or default
    }
    val greeting = if (name.isNotEmpty()) "$timeGreeting, $name \uD83D\uDC4B" else "$timeGreeting \uD83D\uDC4B"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                color = BrandColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Let's build your study guide",
                color = BrandColors.TextSecondary,
                fontSize = 13.5.sp
            )
        }
    }
}

// --- Stats card ---

@Composable
private fun StatsCard(stats: HomeStats, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Surface1)
            .clickable { onClick() }
            .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Compilations",
                    color = BrandColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stats.totalCount.toString(),
                    color = BrandColors.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "This month",
                    color = BrandColors.TextTertiary,
                    fontSize = 12.5.sp
                )
            }
            Sparkline(values = stats.sparkline, modifier = Modifier.size(150.dp, 64.dp))
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return
    Canvas(modifier = modifier) {
        val n = values.size
        if (n < 2) return@Canvas
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).coerceAtLeast(0.001f)
        val stepX = size.width / (n - 1)
        val padding = size.height * 0.12f
        val usableHeight = size.height - padding * 2

        // If all values are the same (including all zeros), draw flat at bottom
        val allSame = maxV == minV
        val points = values.mapIndexed { i, v ->
            Offset(
                x = i * stepX,
                y = if (allSame) size.height - padding else padding + (1f - (v - minV) / range) * usableHeight
            )
        }

        val linePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    BrandColors.BrandDeep.copy(alpha = 0.35f),
                    BrandColors.BrandDeep.copy(alpha = 0f)
                )
            )
        )

        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(
                colors = listOf(BrandColors.BrandStrong, BrandColors.Cyan)
            ),
            style = Stroke(width = 2.4f)
        )
    }
}

// --- Section label ---

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = BrandColors.Brand,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
}

@Composable
fun StatsFullScreenDialog(
    stats: HomeStats,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandColors.Surface0)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compilation Stats",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandColors.Surface2)
                            .pressScaleClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = BrandColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Total count card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandColors.Surface1)
                        .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total PDFs", color = BrandColors.TextSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stats.totalCount.toString(),
                                color = BrandColors.Brand,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("This Month", color = BrandColors.TextTertiary, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            val thisMonthCount = stats.dailyCounts.sumOf { it.count }
                            Text(
                                text = thisMonthCount.toString(),
                                color = BrandColors.TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Chart title
                Text(
                    text = "Last 15 Days",
                    color = BrandColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))

                // Full line chart
                DailyLineChart(
                    dailyCounts = stats.dailyCounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandColors.Surface1)
                        .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyLineChart(dailyCounts: List<DailyCount>, modifier: Modifier = Modifier) {
    if (dailyCounts.isEmpty()) return
    val maxCount = dailyCounts.maxOf { it.count }.coerceAtLeast(1)
    // Y-axis labels: 0 to maxCount, with reasonable steps
    val yStep = when {
        maxCount <= 5 -> 1
        maxCount <= 15 -> 3
        maxCount <= 30 -> 5
        else -> (maxCount / 5).coerceAtLeast(1)
    }
    val yLabels = (0..maxCount step yStep).toList().let {
        if (it.last() < maxCount) it + maxCount else it
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = dailyCounts.size
                if (n < 2) return@Canvas
                val leftPad = 36f
                val bottomPad = 4f
                val chartWidth = size.width - leftPad
                val chartHeight = size.height - bottomPad
                val stepX = chartWidth / (n - 1)
                val yMax = yLabels.last().toFloat().coerceAtLeast(1f)

                // Draw horizontal grid lines and Y labels
                for (yVal in yLabels) {
                    val yPos = chartHeight - (yVal.toFloat() / yMax) * chartHeight
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(leftPad, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 1f
                    )
                    // Y-axis label
                    drawContext.canvas.nativeCanvas.drawText(
                        yVal.toString(),
                        leftPad - 12f,
                        yPos + 4f,
                        android.graphics.Paint().apply {
                            color = 0xFF9CA3AF.toInt()
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                    )
                }

                // Data points
                val points = dailyCounts.mapIndexed { i, dc ->
                    Offset(
                        x = leftPad + i * stepX,
                        y = chartHeight - (dc.count.toFloat() / yMax) * chartHeight
                    )
                }

                // Smooth line
                val linePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }

                // Fill gradient
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, chartHeight)
                    lineTo(points.first().x, chartHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BrandColors.BrandDeep.copy(alpha = 0.3f),
                            BrandColors.BrandDeep.copy(alpha = 0f)
                        )
                    )
                )

                // Line stroke
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(BrandColors.BrandStrong, BrandColors.Cyan)
                    ),
                    style = Stroke(width = 2.8f)
                )

                // Dot markers on data points
                for (pt in points) {
                    drawCircle(
                        color = BrandColors.Brand,
                        radius = 4f,
                        center = pt
                    )
                    drawCircle(
                        color = BrandColors.Surface1,
                        radius = 2f,
                        center = pt
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // X-axis date labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Show every 3rd label to avoid crowding
            dailyCounts.forEachIndexed { i, dc ->
                if (i % 3 == 0 || i == dailyCounts.size - 1) {
                    Text(
                        text = dc.dateLabel.substringAfter(" "),  // Just the day number
                        color = BrandColors.TextQuaternary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
            }
        }
    }
}

// --- Playlists card ---

@Composable
private fun PlaylistsCard(
    playlists: List<Playlist>,
    isFetchingMeta: Boolean,
    onAddPlaylist: () -> Unit,
    onRemovePlaylist: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Surface1)
            .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            // Card header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Playlists",
                        color = BrandColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(9.dp))
                    Badge(text = playlists.size.toString())
                }
                TextButton(onClick = onAddPlaylist) {
                    Text("+ Add Playlist", color = BrandColors.Brand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists yet — tap \u201C+ Add Playlist\u201D",
                    color = BrandColors.TextTertiary,
                    fontSize = 12.5.sp
                )
            } else {
                playlists.forEach { p ->
                    PlaylistRow(
                        playlist = p,
                        isFetchingMeta = isFetchingMeta && p.title == null,
                        onRemove = { onRemovePlaylist(p.url) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, isFetchingMeta: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(BrandColors.Surface2)
            .border(1.dp, BrandColors.SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // YouTube red icon — 38x30dp, r=9dp, white play triangle
        Box(
            modifier = Modifier
                .size(38.dp, 30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(BrandColors.YtRed),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(13.dp)) {
                val w = size.minDimension / 12f
                val tri = Path().apply {
                    moveTo(2f * w, 1.5f * w)
                    lineTo(2f * w, 10.5f * w)
                    lineTo(10f * w, 6f * w)
                    close()
                }
                drawPath(tri, Color.White)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title ?: "Playlist",
                color = BrandColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isFetchingMeta) "Loading\u2026" else "${playlist.videoCount ?: "?"} videos",
                color = BrandColors.TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Remove playlist",
            tint = BrandColors.TextSecondary,
            modifier = Modifier
                .size(20.dp)
                .pressScaleClickable(onClick = onRemove)
        )
    }
}

// --- Topics card ---

@Composable
private fun TopicsCard(
    topics: List<com.adnanfoisal.play2pdf.domain.model.Topic>,
    onAddTopic: () -> Unit,
    onRemoveTopic: (String) -> Unit,
    onExtractTopics: () -> Unit,
    isExtracting: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Surface1)
            .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Topics / Syllabus",
                        color = BrandColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(9.dp))
                    Badge(text = topics.size.toString())
                }
                TextButton(onClick = onAddTopic) {
                    Text("+ Add Topic", color = BrandColors.Brand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                topics.forEach { topic ->
                    TopicPill(text = topic.text, onRemove = { onRemoveTopic(topic.text) })
                }
                // "+" add pill
                Box(
                    modifier = Modifier
                        .size(42.dp, 40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(13.dp))
                        .pressScaleClickable(onClick = onAddTopic),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = BrandColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (topics.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onExtractTopics, enabled = !isExtracting) {
                    Text(
                        text = if (isExtracting) "Extracting\u2026" else "Auto-extract from playlists",
                        color = BrandColors.Brand,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicPill(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(BrandColors.BrandStrong.copy(alpha = 0.05f))
            .border(1.dp, BrandColors.BrandStrong.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 9.dp)
            .pressScaleClickable(onClick = onRemove),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = BrandColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove topic",
            tint = BrandColors.TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

// --- PDF theme row ---

@Composable
private fun PdfThemeRow(theme: PdfTheme, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Surface1)
            .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PDF Theme",
            color = BrandColors.TextPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
                .background(BrandColors.Surface2)
                .border(1.dp, BrandColors.SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = theme.displayName,
                color = BrandColors.TextSecondary,
                fontSize = 13.5.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .background(BrandColors.BrandStrong.copy(alpha = 0.16f))
                .pressScaleClickable(onClick = onChange)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text("Change", color = BrandColors.Brand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --- Advanced row ---

@Composable
private fun AdvancedRow(subject: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onOpen)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (subject.isBlank()) "Advanced: subject & author" else "Subject: $subject",
            color = BrandColors.TextSecondary,
            fontSize = 12.5.sp
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = BrandColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// --- Compile button ---

@Composable
private fun CompileButton(enabled: Boolean, onClick: () -> Unit) {
    val gradAlpha = if (enabled) 1f else 0.4f
    val gradBrush = Brush.horizontalGradient(
        colors = listOf(
            BrandColors.BrandDeep.copy(alpha = gradAlpha),
            BrandColors.BrandMid.copy(alpha = gradAlpha),
            BrandColors.BrandGradEnd.copy(alpha = gradAlpha)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(gradBrush)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = BrandColors.BrandMid,
                spotColor = BrandColors.BrandMid
            )
            .pressScaleClickable(onClick = { if (enabled) onClick() }, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "Compile Study Track",
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// --- Badge ---

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrandColors.BrandStrong.copy(alpha = 0.18f))
            .padding(horizontal = 9.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = BrandColors.Brand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// --- Bottom Sheets ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistUrlDialog(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text("Add Playlist", color = BrandColors.TextPrimary, style = AppType.title3)
            Spacer(Modifier.height(Spacing.md))
            PremiumTextField(
                value = initialValue,
                onValueChange = onValueChange,
                label = "YouTube playlist URL",
                placeholder = "https://youtube.com/playlist?list=\u2026"
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(text = "Add Playlist", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicInputDialog(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text("Add Topics (Batch Import)", color = BrandColors.TextPrimary, style = AppType.title3)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Paste multiple topics separated by commas or newlines. They will be added all at once.",
                color = BrandColors.TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(Spacing.md))
            androidx.compose.material3.OutlinedTextField(
                value = initialValue,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("e.g. Photosynthesis, Cell division\nGenetics", color = BrandColors.TextSecondary) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandColors.Brand,
                    unfocusedBorderColor = BrandColors.SurfaceBorder,
                    focusedTextColor = BrandColors.TextPrimary,
                    unfocusedTextColor = BrandColors.TextPrimary
                )
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(text = "Add Topics", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerDialog(
    themes: List<PdfTheme>,
    selected: PdfTheme,
    onSelect: (PdfTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text("PDF Theme", color = BrandColors.TextPrimary, style = AppType.title3)
            Spacer(Modifier.height(Spacing.md))
            PdfThemePreviewRow(
                themes = themes,
                selected = selected,
                onSelect = onSelect,
                compact = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedDialog(
    subject: String,
    author: String,
    onSubjectChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Book Details", color = BrandColors.TextPrimary, style = AppType.title3)
            PremiumTextField(value = subject, onValueChange = onSubjectChange, label = "Subject")
            PremiumTextField(value = author, onValueChange = onAuthorChange, label = "Author")
            PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = Spacing.sm)
            .size(40.dp, 4.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .background(BrandColors.SurfaceBorderStrong)
    )
}
