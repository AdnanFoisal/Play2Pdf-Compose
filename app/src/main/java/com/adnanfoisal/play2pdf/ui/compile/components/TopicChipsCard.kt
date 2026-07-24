package com.adnanfoisal.play2pdf.ui.compile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.AnimatedChip
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.domain.model.Topic
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors

/**
 * Syllabus topics card: textarea + "Auto-extract topics" button +
 * chip cloud of existing topics.
 *
 * Per v2.0 §10.3.
 */
@Composable
fun TopicChipsCard(
    topicInput: String,
    onTopicInputChange: (String) -> Unit,
    topics: List<Topic>,
    isExtracting: Boolean,
    onAddTopic: (String) -> Unit,
    onRemoveTopic: (String) -> Unit,
    onExtractTopics: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.compile_topics_label),
                    color = BrandColors.TextSecondary,
                    style = AppType.label,
                    modifier = Modifier.weight(1f)
                )
                // Auto-extract topics button
                Row(
                    modifier = Modifier
                        .clip(AppShape.pill)
                        .background(BrandColors.Surface3)
                        .pressScaleClickable(onClick = onExtractTopics, pressedScale = 0.95f)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.dp,
                            color = BrandColors.Brand
                        )
                        Spacer(Modifier.size(Spacing.xs))
                    } else {
                        Icon(
                            imageVector = AppIcons.Sparkle,
                            contentDescription = null,
                            tint = BrandColors.Brand,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(Spacing.xs))
                    }
                    Text(
                        text = stringResource(R.string.compile_extract_topics),
                        color = BrandColors.TextPrimary,
                        style = AppType.label.copy(fontSize = 11.sp)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            PremiumTextField(
                value = topicInput,
                onValueChange = onTopicInputChange,
                label = stringResource(R.string.compile_topics_label),
                placeholder = stringResource(R.string.compile_topics_placeholder),
                singleLine = false,
                maxLines = 4
            )
            // Quick add button if there's pending text
            AnimatedVisibility(
                visible = topicInput.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = Spacing.sm)
                        .clip(AppShape.pill)
                        .background(BrandColors.Brand)
                        .pressScaleClickable(onClick = { onAddTopic(topicInput) })
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text(
                        text = "Add",
                        color = BrandColors.PureWhite,
                        style = AppType.label
                    )
                }
            }
            if (topics.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    topics.forEach { topic ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AnimatedChip(
                                text = topic.text,
                                onClose = { onRemoveTopic(topic.text) }
                            )
                        }
                    }
                }
            }
        }
    }
}
