package com.adnanfoisal.play2pdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.adnanfoisal.play2pdf.ui.navigation.Play2PdfNavHost
import com.adnanfoisal.play2pdf.ui.theme.Play2PdfTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the Compose UI.
 *
 * Order of operations on cold start:
 *  1. installSplashScreen() — installs the Android 12+ SplashScreen API
 *     (no-op on older versions). The splash drawable from
 *     [R.drawable.splash_icon] shows before Compose boots.
 *  2. enableEdgeToEdge() — draws behind system bars so we can tint them
 *     from inside [com.adnanfoisal.play2pdf.ui.theme.Play2PdfTheme].
 *  3. setContent { Play2PdfTheme { Play2PdfNavHost() } } — Compose UI tree.
 *
 * The splash is dismissed automatically once [setContent] draws the first
 * frame. For an extra-long hold (e.g. while loading initial DataStore
 * values) we'd pass a condition to setKeepOnScreenCondition, but our
 * DataStore reads are fast enough not to need it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate so the system
        // picks up our SplashScreen theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Play2PdfTheme {
                Play2PdfNavHost()
            }
        }
    }
}
