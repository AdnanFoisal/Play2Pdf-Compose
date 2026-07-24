package com.adnanfoisal.play2pdf.ui.compiling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.ui.compiling.components.SuccessConfetti
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Compiling screen — shows progress while the backend compiles the PDF.
 *
 * Rewritten to match `mock assests/compile loading.html` per
 * IMPLEMENTATION_PLAN.md Step 5:
 *  - In-progress: animated circular ring (sweep gradient + comet dot) +
 *    4-step tracker with rail lines + amber pro-tip card with sheen
 *  - Success: confetti + PDF preview + Open/Save buttons (preserved)
 *  - Error: error in code box + Try Again/Copy buttons (preserved)
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
                    viewModel.saveToDownloads { _ -> onDone() }
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

// --- In-progress content (matches compile loading.html) ---

@Composable
private fun InProgressContent(
    currentStep: CompileStep,
    completedSteps: Set<CompileStep>,
    onCancel: () -> Unit
) {
    // Map the 6 CompileSteps onto the mockup's 4 visible steps.
    val trackerSteps = listOf(
        TrackerStep("Fetching playlist videos", "Pulling videos from YouTube", CompileStep.FetchingVideos),
        TrackerStep("Analyzing with Gemini AI", "Reading transcripts", CompileStep.ExtractingTopics),
        TrackerStep("Matching to topics", "Creating study notes & insights", CompileStep.MatchingTopics),
        TrackerStep("Rendering PDF", "Designing your study guide", CompileStep.RenderingPdf)
    )

    // Derive overall progress from the step state (0f..1f).
    val totalSteps = CompileStep.entries.size
    val rawProgress = completedSteps.size.toFloat() / totalSteps
    // Add a partial credit for the in-flight current step so the ring sits
    // between the last completed step and the next one.
    val progress = min(rawProgress + (1f / totalSteps) * 0.5f, 0.99f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 14.dp, bottom = 20.dp)
    ) {
        // Header — back button + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .pressScaleClickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = BrandColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Compiling Your Study Track",
                    color = BrandColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Sit back, we're preparing your PDF",
                    color = BrandColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Progress ring
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ProgressRing(progress = progress)
        }

        Spacer(Modifier.height(30.dp))

        // Step tracker
        Column {
            trackerSteps.forEachIndexed { index, step ->
                val stepState = when {
                    step.step in completedSteps && step.step != currentStep -> StepState.Done
                    step.step == currentStep -> StepState.Active
                    else -> StepState.Pending
                }
                StepRow(
                    step = step,
                    state = stepState,
                    isLast = index == trackerSteps.lastIndex
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Pro-tip card
        ProTipCard()
        Spacer(Modifier.height(16.dp))

        // Cancel button
        PrimaryButton(
            text = stringResource(R.string.compiling_cancel),
            onClick = onCancel,
            variant = PrimaryButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

private data class TrackerStep(
    val title: String,
    val subtitle: String,
    val step: CompileStep
)

private enum class StepState { Done, Active, Pending }

@Composable
private fun ProgressRing(progress: Float) {
    // Animated display percentage (easeOutCubic toward target).
    val targetPct = (progress * 100f).coerceIn(0f, 100f)
    val animatedPct by animateFloatAsState(
        targetValue = targetPct,
        animationSpec = tween(durationMillis = 1700, easing = {
            val t = it
            1f - (1f - t) * (1f - t) * (1f - t) // easeOutCubic
        }),
        label = "ringPct"
    )

    // Aura breathing animation.
    val infinite = rememberInfiniteTransition(label = "aura")
    val auraScale by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Reverse),
        label = "auraScale"
    )

    Box(
        modifier = Modifier.size(224.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aura — blurred conic-ish gradient circle behind the ring.
        Canvas(
            modifier = Modifier
                .size(250.dp * auraScale)
                .clip(CircleShape)
        ) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BrandColors.Fuchsia.copy(alpha = 0.45f),
                        BrandColors.BrandDeep.copy(alpha = 0.40f),
                        BrandColors.BrandGradEnd.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                radius = r
            )
        }

        // Ring — track + progress arc + comet.
        Canvas(modifier = Modifier.size(224.dp)) {
            val strokeW = 12.dp.toPx()
            val r = (size.minDimension - strokeW) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Track
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = r,
                center = center,
                style = Stroke(width = strokeW)
            )

            // Progress arc — sweep gradient (fuchsia→violet→blue→cyan).
            val sweep = Brush.sweepGradient(
                colors = listOf(
                    BrandColors.Fuchsia,
                    BrandColors.BrandDeep,
                    BrandColors.BrandGradEnd,
                    BrandColors.Cyan,
                    BrandColors.Fuchsia
                ),
                center = center
            )
            val sweepAngle = (animatedPct / 100f) * 360f
            val arcSize = androidx.compose.ui.geometry.Size(r * 2, r * 2)
            drawArc(
                brush = sweep,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Comet dot at the arc tip.
            if (sweepAngle > 1f) {
                val angleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                val tipX = center.x + r * cos(angleRad).toFloat()
                val tipY = center.y + r * sin(angleRad).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, BrandColors.Cyan, Color.Transparent),
                        center = Offset(tipX, tipY),
                        radius = 14.dp.toPx()
                    ),
                    radius = 7.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        // Center — percentage + label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = animatedPct.toInt().toString(),
                    color = BrandColors.TextPrimary,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%",
                    color = Color(0xFFd9d6ee),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Almost there!",
                color = BrandColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StepRow(step: TrackerStep, state: StepState, isLast: Boolean) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        // Rail column — dot + connecting line
        Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StepDot(state = state)
                if (!isLast) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(34.dp)
                            .background(
                                if (state == StepState.Done)
                                    BrandColors.Green.copy(alpha = 0.4f)
                                else
                                    Color.White.copy(alpha = 0.07f)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.padding(top = 1.dp)) {
            Text(
                text = step.title,
                color = when (state) {
                    StepState.Done -> BrandColors.TextPrimary
                    StepState.Active -> BrandColors.TextPrimary
                    StepState.Pending -> Color(0xFFb9b8cc)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = step.subtitle,
                color = BrandColors.TextTertiary,
                fontSize = 13.sp
            )
            if (state == StepState.Active) {
                Spacer(Modifier.height(9.dp))
                // Shimmer bar — animated gradient sweep
                val shimmerInfinite = rememberInfiniteTransition(label = "shimmer")
                val shimmerX by shimmerInfinite.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
                    label = "shimmerX"
                )
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BrandColors.BrandStrong.copy(alpha = 0.12f),
                                    BrandColors.BrandStrong.copy(alpha = 0.6f),
                                    BrandColors.Cyan.copy(alpha = 0.6f),
                                    BrandColors.BrandStrong.copy(alpha = 0.12f)
                                ),
                                startX = shimmerX * 150f * 2.2f - 80f,
                                endX = shimmerX * 150f * 2.2f + 80f
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StepDot(state: StepState) {
    when (state) {
        StepState.Done -> Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF34d36b), BrandColors.GreenDeep)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        StepState.Active -> Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandColors.Brand, BrandColors.BrandDeep)
                    )
                )
                .border(2.dp, BrandColors.BrandStrong.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
        StepState.Pending -> Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
        }
    }
}

@Composable
private fun ProTipCard() {
    // Sheen sweep animation.
    val infinite = rememberInfiniteTransition(label = "sheen")
    val sheenX by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "sheenX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandColors.Amber.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, BrandColors.Amber.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
    ) {
        // Sheen overlay
        if (sheenX in -0.6f..1.6f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.06f), Color.Transparent),
                            start = Offset(sheenX * 300f, 0f),
                            end = Offset(sheenX * 300f + 120f, 0f)
                        )
                    )
            )
        }
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = BrandColors.Amber,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Pro Tip",
                    color = BrandColors.Amber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Great study guides are built from the right content. You're doing awesome!",
                    color = Color(0xFFb6b4c8),
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp
                )
            }
        }
    }
}

// --- Success / Error content (preserved from skeleton) ---

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

// --- Helpers ---

// `border` is provided by `androidx.compose.foundation.border` (imported above).
