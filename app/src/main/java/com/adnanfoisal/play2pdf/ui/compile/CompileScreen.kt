package com.adnanfoisal.play2pdf.ui.compile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Box(modifier = Modifier.fillMaxSize().background(BrandColors.Surface0)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Header — greeting + gold crown button
            GreetingHeader(userName = state.userName)
            Spacer(Modifier.height(18.dp))

            // Stats card — total compilations + sparkline
            StatsCard(stats = state.stats)
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

            // PDF theme row
            PdfThemeRow(
                theme = state.selectedTheme,
                onChange = { showThemeDialog = true }
            )
            Spacer(Modifier.height(14.dp))

            // Advanced toggle — subject/author fields
            AdvancedRow(onOpen = { showAdvancedDialog = true }, subject = state.subject)
            Spacer(Modifier.height(18.dp))

            // Compile button
            CompileButton(
                enabled = state.canCompile,
                onClick = onCompileRequest
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
}

// --- Header ---

@Composable
private fun GreetingHeader(userName: String) {
    val name = userName.trim()
    val greeting = if (name.isNotEmpty()) "Hello, $name \uD83D\uDC4B" else "Hello \uD83D\uDC4B"
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
        // Gold crown button — 42dp circle with gold border
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF15151f))
                .border(1.dp, BrandColors.Gold.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                // Crown path from Home screen.html: M3 7l3.5 3L12 4l5.5 6L21 7l-1.5 11h-15L3 7z
                val w = size.minDimension / 24f
                val crown = Path().apply {
                    moveTo(3f * w, 7f * w)
                    lineTo(6.5f * w, 10f * w)
                    lineTo(12f * w, 4f * w)
                    lineTo(17.5f * w, 10f * w)
                    lineTo(21f * w, 7f * w)
                    lineTo(19.5f * w, 18f * w)
                    lineTo(4.5f * w, 18f * w)
                    close()
                }
                drawPath(crown, BrandColors.Gold)
                // Base bar
                val base = Path().apply {
                    moveTo(4.8f * w, 20f * w)
                    lineTo(19.2f * w, 20f * w)
                    lineTo(19.2f * w, 22f * w)
                    lineTo(4.8f * w, 22f * w)
                    close()
                }
                drawPath(base, BrandColors.Gold)
            }
        }
    }
}

// --- Stats card ---

@Composable
private fun StatsCard(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Surface1)
            .border(1.dp, BrandColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(
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

        val points = values.mapIndexed { i, v ->
            Offset(
                x = i * stepX,
                y = padding + (1f - (v - minV) / range) * usableHeight
            )
        }

        // Smooth-ish line path through the points
        val linePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        // Fill gradient underneath
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

        // Line stroke with violet→cyan gradient
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

// --- Dialogs ---

@Composable
private fun PlaylistUrlDialog(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Playlist", color = BrandColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = initialValue,
                onValueChange = onValueChange,
                label = { Text("YouTube playlist URL") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TopicInputDialog(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Topic", color = BrandColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = initialValue,
                onValueChange = onValueChange,
                label = { Text("Topic (comma-separated)") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThemePickerDialog(
    themes: List<PdfTheme>,
    selected: PdfTheme,
    onSelect: (PdfTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF Theme", color = BrandColors.TextPrimary) },
        text = {
            Column {
                themes.forEach { theme ->
                    val isSelected = theme == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScaleClickable(onClick = { onSelect(theme) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BrandColors.Brand else Color.Transparent)
                                .border(1.dp, BrandColors.Brand, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = theme.displayName,
                            color = BrandColors.TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AdvancedDialog(
    subject: String,
    author: String,
    onSubjectChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced \u2014 Book Details", color = BrandColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = onSubjectChange,
                    label = { Text("Subject") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = onAuthorChange,
                    label = { Text("Author") },
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
