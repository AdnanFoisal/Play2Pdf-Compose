package com.adnanfoisal.play2pdf.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App theme entry point.
 *
 * Dark-only for now — the v2.0 plan locks the app to a dark surface
 * palette (`Surface0 = #09090B`). If the Design Agent delivers a
 * light palette (Asset A), flip [ForceDark] to false and provide a
 * real [lightColorScheme] below.
 *
 * Edge-to-edge system bar tinting happens here as a [SideEffect] so
 * every Activity that uses this theme gets consistent system bars.
 * The status bar gets a transparent background + light icons (since
 * the app is dark). The nav bar gets transparent + light icons too.
 */
@Composable
fun Play2PdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val forceDark = ForceDark
    val effectiveDark = if (forceDark) true else darkTheme

    val colorScheme = if (effectiveDark) {
        darkColorScheme(
            primary = BrandColors.Brand,
            onPrimary = BrandColors.PureWhite,
            primaryContainer = BrandColors.BrandDark,
            onPrimaryContainer = BrandColors.PureWhite,
            secondary = BrandColors.BrandLight,
            onSecondary = BrandColors.PureWhite,
            tertiary = BrandColors.BrandLight,
            onTertiary = BrandColors.PureWhite,
            background = BrandColors.Surface0,
            onBackground = BrandColors.TextPrimary,
            surface = BrandColors.Surface1,
            onSurface = BrandColors.TextPrimary,
            surfaceVariant = BrandColors.Surface2,
            onSurfaceVariant = BrandColors.TextSecondary,
            surfaceTint = BrandColors.Brand,
            inverseSurface = BrandColors.Surface3,
            inverseOnSurface = BrandColors.TextPrimary,
            error = BrandColors.Error,
            onError = BrandColors.PureWhite,
            errorContainer = BrandColors.ErrorBg,
            onErrorContainer = BrandColors.Error,
            outline = BrandColors.SurfaceBorder,
            outlineVariant = BrandColors.SurfaceBorder,
            scrim = BrandColors.PureBlack
        )
    } else {
        lightColorScheme(
            primary = BrandColors.Brand,
            onPrimary = BrandColors.PureWhite,
            primaryContainer = BrandColors.BrandLight,
            onPrimaryContainer = BrandColors.PureWhite,
            secondary = BrandColors.BrandDark,
            onSecondary = BrandColors.PureWhite,
            tertiary = BrandColors.BrandDark,
            onTertiary = BrandColors.PureWhite,
            background = BrandColors.PureWhite,
            onBackground = BrandColors.PureBlack,
            surface = BrandColors.PureWhite,
            onSurface = BrandColors.PureBlack,
            surfaceVariant = Color(0xFFF4F4F5),
            onSurfaceVariant = Color(0xFF52525B),
            surfaceTint = BrandColors.Brand,
            error = BrandColors.Error,
            onError = BrandColors.PureWhite,
            outline = Color(0xFFE4E4E7),
            outlineVariant = Color(0xFFD4D4D8),
            scrim = BrandColors.PureBlack
        )
    }

    // Tint system bars to match the app background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !effectiveDark  // light icons = false in dark
            controller.isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppType.material,
        shapes = AppShape.material,
        content = content
    )
}

/** When true, the app forces dark theme regardless of system setting. */
private const val ForceDark = true
