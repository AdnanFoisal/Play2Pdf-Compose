package com.adnanfoisal.play2pdf.ui.compile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.AnimatedChip
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButton
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.ui.compile.components.PlaylistInputCard
import com.adnanfoisal.play2pdf.ui.compile.components.TopicChipsCard

/**
 * Compile screen — the main "create a study guide" surface.
 *
 * Per v2.0 §10.3.
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

    Box(modifier = Modifier.fillMaxSize().background(BrandColors.Surface0)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg)
        ) {
            Spacer(Modifier.height(Spacing.lg))
            HeaderBanner()
            Spacer(Modifier.height(Spacing.lg))

            PlaylistInputCard(
                urlInput = state.playlistUrlInput,
                onUrlInputChange = { viewModel.onEvent(CompileUiEvent.PlaylistUrlChanged(it)) },
                playlists = state.playlists,
                isFetchingMeta = state.isFetchingMeta,
                onAddPlaylist = { viewModel.onEvent(CompileUiEvent.AddPlaylist(it)) },
                onRemovePlaylist = { viewModel.onEvent(CompileUiEvent.RemovePlaylist(it)) }
            )
            Spacer(Modifier.height(Spacing.md))

            TopicChipsCard(
                topicInput = state.topicInput,
                onTopicInputChange = { viewModel.onEvent(CompileUiEvent.TopicInputChanged(it)) },
                topics = state.topics,
                isExtracting = state.isExtractingTopics,
                onAddTopic = { viewModel.onEvent(CompileUiEvent.AddTopic(it)) },
                onRemoveTopic = { viewModel.onEvent(CompileUiEvent.RemoveTopic(it)) },
                onExtractTopics = { viewModel.onEvent(CompileUiEvent.ExtractTopics) }
            )
            Spacer(Modifier.height(Spacing.md))

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Book Details",
                        color = BrandColors.TextSecondary,
                        style = AppType.label
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    PremiumTextField(
                        value = state.subject,
                        onValueChange = { viewModel.onEvent(CompileUiEvent.SubjectChanged(it)) },
                        label = stringResource(R.string.compile_subject_label),
                        placeholder = stringResource(R.string.compile_subject_placeholder)
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    PremiumTextField(
                        value = state.author,
                        onValueChange = { viewModel.onEvent(CompileUiEvent.AuthorChanged(it)) },
                        label = stringResource(R.string.compile_author_label),
                        placeholder = stringResource(R.string.compile_author_placeholder)
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(R.string.compile_theme_label),
                        color = BrandColors.TextSecondary,
                        style = AppType.label
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    ThemePickerRow(
                        selected = state.selectedTheme,
                        onSelect = { viewModel.onEvent(CompileUiEvent.ThemeChanged(it)) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            PrimaryButton(
                text = stringResource(R.string.compile_button),
                icon = AppIcons.Play,
                onClick = onCompileRequest,
                enabled = state.canCompile,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.lg))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HeaderBanner() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.compile_header_title),
            color = BrandColors.TextPrimary,
            style = AppType.title1
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.compile_header_subtitle),
            color = BrandColors.TextSecondary,
            style = AppType.bodySmall
        )
    }
}

@Composable
private fun ThemePickerRow(
    selected: PdfTheme,
    onSelect: (PdfTheme) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items(PdfTheme.entries.toList()) { theme ->
            AnimatedChip(
                text = theme.displayName,
                selected = theme == selected,
                onClick = { onSelect(theme) }
            )
        }
    }
}
