package com.adnanfoisal.play2pdf.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Redesigned Spread-Out Splash Screen Animation
 * 
 * 1. Play icon slides in from left.
 * 2. Doc icon slides in from right.
 * 3. They merge with a ripple/flash.
 * 4. Text fades in and letter-spacing expands outwards.
 */
@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Animation states
    val playOffsetX = remember { Animatable(-300f) }
    val playAlpha = remember { Animatable(0f) }
    
    val docOffsetX = remember { Animatable(300f) }
    val docAlpha = remember { Animatable(0f) }
    
    val rippleScale = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    
    val textAlpha = remember { Animatable(0f) }
    val textLetterSpacing = remember { Animatable(-4f) }

    LaunchedEffect(Unit) {
        viewModel.boot(onNavigate)

        // 1. Slide in Play icon
        coroutineScope {
            launch {
                playOffsetX.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                playAlpha.animateTo(1f, tween(400))
            }
        }
        
        delay(100)

        // 2. Slide in Doc icon
        coroutineScope {
            launch {
                docOffsetX.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                docAlpha.animateTo(1f, tween(400))
            }
        }
        
        delay(200)
        
        // 3. Merge & Ripple flash
        coroutineScope {
            launch {
                rippleAlpha.snapTo(0.6f)
                rippleAlpha.animateTo(0f, tween(800, easing = LinearOutSlowInEasing))
            }
            launch {
                rippleScale.snapTo(0.5f)
                rippleScale.animateTo(2f, tween(800, easing = FastOutSlowInEasing))
            }
            // 4. Text fade and letter-spacing expansion
            launch {
                textAlpha.animateTo(1f, tween(800))
            }
            launch {
                textLetterSpacing.animateTo(4f, tween(1000, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Surface0),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Ripple/Flash effect
                if (rippleAlpha.value > 0f) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(rippleScale.value)
                            .alpha(rippleAlpha.value)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BrandColors.Brand.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension / 2f
                        )
                    }
                }
                
                // Play and Doc Icons
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Center the combined icon
                    val cx = w / 2
                    val cy = h / 2
                    val iconW = 80f
                    val iconH = 90f
                    
                    val gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
                        start = Offset(cx - iconW, cy - iconH),
                        end = Offset(cx + iconW, cy + iconH)
                    )

                    // Draw Play Icon (Left half)
                    val pAlpha = playAlpha.value
                    val px = playOffsetX.value
                    if (pAlpha > 0f) {
                        val playPath = Path().apply {
                            moveTo(cx - 35f + px, cy - 30f)
                            lineTo(cx + 5f + px, cy)
                            lineTo(cx - 35f + px, cy + 30f)
                            close()
                        }
                        drawPath(playPath, gradient, alpha = pAlpha)
                    }

                    // Draw Document Icon (Right half)
                    val dAlpha = docAlpha.value
                    val dx = docOffsetX.value
                    if (dAlpha > 0f) {
                        val docPath = Path().apply {
                            moveTo(cx - 5f + dx, cy - 40f)
                            lineTo(cx + 20f + dx, cy - 40f)
                            lineTo(cx + 35f + dx, cy - 25f)
                            lineTo(cx + 35f + dx, cy + 40f)
                            lineTo(cx - 5f + dx, cy + 40f)
                            close()
                        }
                        drawPath(docPath, gradient, alpha = dAlpha)
                        
                        // Doc Fold
                        val foldPath = Path().apply {
                            moveTo(cx + 20f + dx, cy - 40f)
                            lineTo(cx + 20f + dx, cy - 25f)
                            lineTo(cx + 35f + dx, cy - 25f)
                            close()
                        }
                        drawPath(foldPath, Color.White.copy(alpha = 0.3f), alpha = dAlpha)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Text Expansion
            Text(
                text = "Play2PDF",
                color = BrandColors.TextPrimary,
                style = AppType.display.copy(
                    fontSize = 36.sp,
                    letterSpacing = textLetterSpacing.value.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Intelligent Video Compiler",
                color = BrandColors.TextSecondary,
                style = AppType.bodySmall.copy(
                    letterSpacing = (textLetterSpacing.value / 2).sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
