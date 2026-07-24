package com.adnanfoisal.play2pdf.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.AnimatedChip
import com.adnanfoisal.play2pdf.core.designsystem.components.EmptyState
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.ui.history.components.SwipeToDismissHistoryItem

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(BrandColors.Surface0)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg)
        ) {
            Spacer(Modifier.height(Spacing.lg))
            // Title row with sort icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    color = BrandColors.TextPrimary,
                    style = AppType.title1
                )
                Icon(
                    imageVector = AppIcons.Filter,
                    contentDescription = stringResource(R.string.cd_more_options),
                    tint = BrandColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(Spacing.md))

            // Search field
            PremiumTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = stringResource(R.string.history_search_placeholder),
                placeholder = stringResource(R.string.history_search_placeholder),
                trailingIcon = {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = null,
                        tint = BrandColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            Spacer(Modifier.height(Spacing.md))

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                HistoryFilter.entries.forEach { f ->
                    AnimatedChip(
                        text = f.label,
                        selected = state.filter == f,
                        onClick = { viewModel.setFilter(f) }
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))

            if (state.items.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = AppIcons.Inbox, // TODO: replace with Asset G empty_history.xml
                    title = stringResource(R.string.history_empty_title),
                    subtitle = stringResource(R.string.history_empty_subtitle),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.id }) { item ->
                        SwipeToDismissHistoryItem(
                            item = item,
                            onClick = { /* TODO: open PDF */ },
                            onLongPress = { /* TODO: show context menu */ },
                            onDelete = { viewModel.delete(it) }
                        )
                    }
                    item {
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }
}
