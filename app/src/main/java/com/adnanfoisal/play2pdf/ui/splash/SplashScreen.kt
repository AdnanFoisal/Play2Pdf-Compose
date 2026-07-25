package com.adnanfoisal.play2pdf.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import kotlinx.coroutines.delay

/**
 * Splash screen — Compose canvas animation derived from the MP4 reference
 * in `mock assests/splash screen animation video.mp4`.
 *
 * The video shows a glassmorphic play-button icon that rotates/flips and
 * morphs into a PDF document, with "PDF" text fading in and the wordmark
 * sliding up beneath it.
 *
 * Animation timeline (total ~3.2s before navigation):
 *  - Phase 1 (0–500ms):   play icon scales 0.3→1.0 + alpha 0→1 (spring)
 *  - Phase 2 (400–1300ms): 3D-flip effect — crossfade from play button to document shape
 *  - Phase 3 (1100–1600ms): "PDF" text fades in on the document
 *  - Phase 4 (1400–1900ms): violet radial glow expands behind the logo
 *  - Phase 5 (1800–2400ms): "Play2PDF" wordmark slides up + fades in
 *  - Phase 6 (2400ms+):     hold, then [SplashViewModel] navigates at 3200ms
 */
@Composable
fun SplashScreen(
    onNavigateNext: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Drive navigation from the ViewModel.
    LaunchedEffect(Unit) {
        viewModel.boot(onNavigateNext)
    }

    // Phase state machine.
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        phase = 1
        delay(400)
        phase = 2
        delay(700)
        phase = 3
        delay(300)
        phase = 4
        delay(300)
        phase = 5
        delay(400)
        phase = 6
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MorphingLogoBlock(phase = phase)
            Spacer(Modifier.height(Spacing.lg))

            // Phase 5: wordmark slides up + fades in.
            AnimatedVisibility(
                visible = phase >= 5,
                enter = slideInVertically(
                    animationSpec = tween(500),
                    initialOffsetY = { it / 6 }
                ) + fadeIn(animationSpec = tween(500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Play2PDF",
                        color = BrandColors.TextPrimary,
                        style = AppType.display.copy(fontSize = 32.sp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Playlist \u2192 Study guide",
                        color = BrandColors.TextSecondary,
                        style = AppType.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Morphing logo block: play button → PDF document.
 *
 * Uses Canvas to draw both shapes and animates a 3D-flip crossfade
 * between them, matching the MP4 reference beat-for-beat.
 */
@Composable
private fun MorphingLogoBlock(phase: Int) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Phase 4: radial violet glow behind the logo.
        val glowAlpha by animateFloatAsState(
            targetValue = if (phase >= 4) 0.55f else 0f,
            animationSpec = tween(500),
            label = "glowAlpha"
        )
        val glowScale by animateFloatAsState(
            targetValue = if (phase >= 4) 1f else 0.3f,
            animationSpec = tween(500),
            label = "glowScale"
        )
        if (glowAlpha > 0.01f) {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .scale(glowScale)
                    .alpha(glowAlpha)
            ) {
                val center = Offset(size.minDimension / 2f, size.minDimension / 2f)
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandColors.BrandStrong.copy(alpha = 0.7f),
                            BrandColors.BrandDeep.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )
                )
            }
        }

        // Phase 1: overall scale + alpha.
        val logoScale by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "logoScale"
        )
        val logoAlpha by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0f,
            animationSpec = tween(500),
            label = "logoAlpha"
        )

        // Phase 2: flip animation — scaleX goes 1 → 0 → 1 while crossfading shapes.
        val flipProgress by animateFloatAsState(
            targetValue = when {
                phase < 2 -> 0f
                phase == 2 -> 1f
                else -> 2f
            },
            animationSpec = tween(900),
            label = "flipProgress"
        )

        // Phase 3: PDF text alpha.
        val pdfTextAlpha by animateFloatAsState(
            targetValue = if (phase >= 3) 1f else 0f,
            animationSpec = tween(500),
            label = "pdfTextAlpha"
        )

        Canvas(
            modifier = Modifier
                .size(120.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
        ) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // During the flip (phase 2), we simulate 3D by scaling X.
            // flipProgress 0..1 = play button flipping away
            // flipProgress 1..2 = document flipping in
            val flipScale = when {
                flipProgress <= 1f -> 1f - flipProgress  // 1 → 0
                else -> flipProgress - 1f               // 0 → 1
            }

            if (flipProgress <= 1f) {
                // Draw play button shape (rounded rect + triangle).
                drawPlayButton(
                    center = center,
                    sizePx = minOf(w, h) * 0.85f,
                    scaleX = flipScale.coerceAtLeast(0.01f)
                )
            } else {
                // Draw document shape (rect + folded corner).
                drawDocumentIcon(
                    center = center,
                    sizePx = minOf(w, h) * 0.85f,
                    scaleX = flipScale.coerceAtLeast(0.01f)
                )

                // Phase 3+: "PDF" text on the document.
                if (pdfTextAlpha > 0.01f) {
                    val textLayout = textMeasurer.measure(
                        text = "PDF",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.85f * pdfTextAlpha),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            center.x - textLayout.size.width / 2f,
                            center.y + size.height * 0.18f
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPlayButton(center: Offset, sizePx: Float, scaleX: Float) {
    val halfW = sizePx / 2f * scaleX
    val halfH = sizePx / 2f
    val corner = sizePx * 0.22f

    // Glassmorphic rounded rectangle body.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                BrandColors.Brand.copy(alpha = 0.85f),
                BrandColors.BrandGradEnd.copy(alpha = 0.75f)
            ),
            start = Offset(center.x - halfW, center.y - halfH),
            end = Offset(center.x + halfW, center.y + halfH)
        ),
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(halfW * 2, halfH * 2),
        cornerRadius = CornerRadius(corner, corner)
    )

    // Subtle border glow.
    drawRoundRect(
        color = BrandColors.BrandStrong.copy(alpha = 0.6f),
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(halfW * 2, halfH * 2),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 2f)
    )

    // Play triangle.
    val triW = sizePx * 0.28f * scaleX
    val triH = sizePx * 0.32f
    val triPath = Path().apply {
        moveTo(center.x - triW * 0.35f, center.y - triH * 0.5f)
        lineTo(center.x + triW * 0.65f, center.y)
        lineTo(center.x - triW * 0.35f, center.y + triH * 0.5f)
        close()
    }
    drawPath(
        path = triPath,
        color = Color.White.copy(alpha = 0.92f)
    )
}

private fun DrawScope.drawDocumentIcon(center: Offset, sizePx: Float, scaleX: Float) {
    val halfW = sizePx / 2f * scaleX
    val halfH = sizePx / 2f
    val corner = sizePx * 0.12f
    val foldSize = sizePx * 0.22f

    // Document body path with rounded corners and folded corner.
    val bodyPath = Path().apply {
        // Top-left corner.
        moveTo(center.x - halfW + corner, center.y - halfH)
        // Top edge to fold start.
        lineTo(center.x + halfW - foldSize, center.y - halfH)
        // Fold diagonal.
        lineTo(center.x + halfW, center.y - halfH + foldSize)
        // Right edge.
        lineTo(center.x + halfW, center.y + halfH - corner)
        // Bottom-right corner.
        arcTo(
            rect = Rect(
                center.x + halfW - corner * 2, center.y + halfH - corner * 2,
                center.x + halfW, center.y + halfH
            ),
            startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        // Bottom edge.
        lineTo(center.x - halfW + corner, center.y + halfH)
        // Bottom-left corner.
        arcTo(
            rect = Rect(
                center.x - halfW, center.y + halfH - corner * 2,
                center.x - halfW + corner * 2, center.y + halfH
            ),
            startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        // Left edge.
        lineTo(center.x - halfW, center.y - halfH + corner)
        // Top-left corner.
        arcTo(
            rect = Rect(
                center.x - halfW, center.y - halfH,
                center.x - halfW + corner * 2, center.y - halfH + corner * 2
            ),
            startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        close()
    }

    // Glassmorphic gradient fill.
    drawPath(
        path = bodyPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2a2540).copy(alpha = 0.9f),
                Color(0xFF1a1730).copy(alpha = 0.95f)
            ),
            start = Offset(center.x - halfW, center.y - halfH),
            end = Offset(center.x + halfW, center.y + halfH)
        )
    )

    // Violet edge glow.
    drawPath(
        path = bodyPath,
        color = BrandColors.BrandStrong.copy(alpha = 0.5f),
        style = Stroke(width = 2f)
    )

    // Fold flap.
    val foldPath = Path().apply {
        moveTo(center.x + halfW - foldSize, center.y - halfH)
        lineTo(center.x + halfW, center.y - halfH + foldSize)
        lineTo(center.x + halfW, center.y - halfH)
        close()
    }
    drawPath(
        path = foldPath,
        color = BrandColors.Brand.copy(alpha = 0.5f)
    )

    // Play button overlay on the document (smaller, centered).
    val playSize = sizePx * 0.3f
    val playHalfW = playSize / 2f * scaleX
    val playHalfH = playSize / 2f
    val playTriW = playSize * 0.28f * scaleX
    val playTriH = playSize * 0.32f

    // Small rounded rect behind play triangle.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                BrandColors.Brand.copy(alpha = 0.7f),
                BrandColors.BrandGradEnd.copy(alpha = 0.6f)
            ),
            start = Offset(center.x - playHalfW, center.y - playHalfH),
            end = Offset(center.x + playHalfW, center.y + playHalfH)
        ),
        topLeft = Offset(center.x - playHalfW, center.y - playHalfH - sizePx * 0.06f),
        size = Size(playHalfW * 2, playHalfH * 2),
        cornerRadius = CornerRadius(playSize * 0.18f, playSize * 0.18f)
    )

    val triPath = Path().apply {
        moveTo(center.x - playTriW * 0.35f, center.y - playTriH * 0.5f - sizePx * 0.06f)
        lineTo(center.x + playTriW * 0.65f, center.y - sizePx * 0.06f)
        lineTo(center.x - playTriW * 0.35f, center.y + playTriH * 0.5f - sizePx * 0.06f)
        close()
    }
    drawPath(
        path = triPath,
        color = Color.White.copy(alpha = 0.9f)
    )
}
