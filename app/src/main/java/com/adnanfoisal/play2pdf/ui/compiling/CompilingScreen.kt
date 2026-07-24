package com.adnanfoisal.play2pdf.ui.compiling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButton
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButtonVariant
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.domain.model.CompileStep
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.ui.compiling.components.SuccessConfetti
import java.io.File

/**
 * Compiling screen — shows progress while the backend compiles the PDF.
 *
 * Three phases per v2.0 §10.6:
 *  - In-progress: branded loader + conversational step checklist + cancel button
 *  - Success: confetti + PDF preview + Open/Save buttons
 *  - Error: error in code box + Try Again/Copy buttons
 *
 * NOTE: For v1 we don't pass inputs through SavedStateHandle yet — the
 * caller is expected to invoke [CompilingViewModel.start] right after
 * navigation. A follow-up task will wire this up via navigation arguments
 * so the screen survives process death.
 */
@Composable
fun CompilingScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CompilingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Surface0)
            .statusBarsPadding()
    ) {
        when (state.phase) {
            CompilingPhase.InProgress -> InProgressContent(
                currentStep = state.currentStep,
                completedSteps = state.completedSteps,
                onCancel = onCancel
            )
            CompilingPhase.Success -> SuccessContent(
                pdfFile = state.pdfFile,
                sizeBytes = state.pdfSizeBytes,
                onOpen = {
                    // Open the PDF via an ACTION_VIEW Intent
                    state.pdfFile?.let { file ->
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }
                },
                onSave = {
                    viewModel.saveToDownloads { _ ->
                        onDone()
                    }
                }
            )
            CompilingPhase.Error -> ErrorContent(
                errorMessage = state.errorMessage ?: "Unknown error",
                onTryAgain = { viewModel.retry() },
                onCopyError = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("Play2PDF error", state.errorMessage)
                    )
                }
            )
        }
    }
}

@Composable
private fun InProgressContent(
    currentStep: CompileStep,
    completedSteps: Set<CompileStep>,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Branded loader (placeholder: spinner. TODO: replace with Rive when Asset H is delivered)
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(96.dp),
                strokeWidth = 3.dp,
                color = BrandColors.Brand
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.compiling_title),
            color = BrandColors.TextPrimary,
            style = AppType.title1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xl))

        // Step checklist
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            CompileStep.entries.forEach { step ->
                StepRow(
                    step = step,
                    isCurrent = step == currentStep,
                    isDone = step in completedSteps && step != currentStep
                )
            }
        }
        Spacer(Modifier.height(Spacing.xxl))
        PrimaryButton(
            text = stringResource(R.string.compiling_cancel),
            onClick = onCancel,
            variant = PrimaryButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepRow(step: CompileStep, isCurrent: Boolean, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isDone) BrandColors.Success else if (isCurrent) BrandColors.Brand else BrandColors.Surface3),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = BrandColors.PureWhite,
                    modifier = Modifier.size(14.dp)
                )
            } else if (isCurrent) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.dp,
                    color = BrandColors.PureWhite
                )
            }
        }
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = step.label,
            color = when {
                isDone -> BrandColors.TextSecondary
                isCurrent -> BrandColors.TextPrimary
                else -> BrandColors.TextTertiary
            },
            style = AppType.bodySmall
        )
    }
}

@Composable
private fun SuccessContent(
    pdfFile: File?,
    sizeBytes: Long?,
    onOpen: () -> Unit,
    onSave: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SuccessConfetti(
            modifier = Modifier.fillMaxSize(),
            trigger = pdfFile
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = BrandColors.Success,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.compiling_success_title),
                color = BrandColors.TextPrimary,
                style = AppType.title1,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.sm))
            if (sizeBytes != null) {
                Text(
                    text = "${sizeBytes / 1024} KB",
                    color = BrandColors.TextSecondary,
                    style = AppType.bodySmall
                )
            }
            Spacer(Modifier.height(Spacing.xxl))
            PrimaryButton(
                text = stringResource(R.string.compiling_open),
                icon = AppIcons.OpenExternal,
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(
                text = stringResource(R.string.compiling_save),
                icon = AppIcons.Download,
                onClick = onSave,
                variant = PrimaryButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onTryAgain: () -> Unit,
    onCopyError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = AppIcons.Error,
            contentDescription = null,
            tint = BrandColors.Error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.compiling_error_title),
            color = BrandColors.TextPrimary,
            style = AppType.title1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.md))
        // Error in a code box
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = errorMessage,
                color = BrandColors.Error,
                style = AppType.code,
                modifier = Modifier.padding(Spacing.md)
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        PrimaryButton(
            text = stringResource(R.string.compiling_try_again),
            onClick = onTryAgain,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.sm))
        PrimaryButton(
            text = stringResource(R.string.compiling_copy_error),
            onClick = onCopyError,
            variant = PrimaryButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
