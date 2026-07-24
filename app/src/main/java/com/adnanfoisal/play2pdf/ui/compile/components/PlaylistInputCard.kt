package com.adnanfoisal.play2pdf.ui.compile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.AnimatedChip
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.neonGlow
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Playlist input card: URL field + neon "+" button + chip cloud of added
 * playlists. Per v2.0 §10.3.
 */
@Composable
fun PlaylistInputCard(
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    playlists: List<com.adnanfoisal.play2pdf.domain.model.Playlist>,
    isFetchingMeta: Boolean,
    onAddPlaylist: (String) -> Unit,
    onRemovePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = stringResource(R.string.compile_playlist_input_label),
                color = BrandColors.TextSecondary,
                style = AppType.label
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    PremiumTextField(
                        value = urlInput,
                        onValueChange = onUrlInputChange,
                        label = stringResource(R.string.compile_playlist_input_label),
                        placeholder = stringResource(R.string.compile_playlist_input_placeholder),
                        keyboardType = KeyboardType.Uri
                    )
                }
                Spacer(Modifier.size(Spacing.sm))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .neonGlow(radius = 8.dp, alpha = 0.5f)
                        .clip(AppShape.medium)
                        .background(BrandColors.Brand)
                        .pressScaleClickable(
                            onClick = { onAddPlaylist(urlInput) },
                            pressedScale = 0.92f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = stringResource(R.string.compile_add_playlist),
                        tint = BrandColors.PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (playlists.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    playlists.forEach { p ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFetchingMeta && p.title == null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.dp,
                                        color = BrandColors.TextSecondary
                                    )
                                    Spacer(Modifier.size(Spacing.xs))
                                }
                                AnimatedChip(
                                    text = p.title ?: "Playlist ${p.url.takeLast(8)}",
                                    onClose = { onRemovePlaylist(p.url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
