package com.adnanfoisal.play2pdf.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adnanfoisal.play2pdf.core.designsystem.components.BottomNavItem
import com.adnanfoisal.play2pdf.core.designsystem.components.Play2PdfBottomBar
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.ui.compile.CompileScreen
import com.adnanfoisal.play2pdf.ui.history.HistoryScreen
import com.adnanfoisal.play2pdf.ui.navigation.Routes
import com.adnanfoisal.play2pdf.ui.settings.SettingsScreen

/**
 * Main screen — Scaffold with the 3-tab bottom nav (Compile / History /
 * Settings). Each tab is a nested NavHost destination so backstack is
 * preserved per-tab.
 *
 * The bottom bar is custom (per Phase B §9.5) — not the default M3
 * NavigationBar — so we can apply our brand color, the sliding pill
 * indicator, and the press-scale on each tab.
 *
 * The `onCompileRequest` callback is forwarded from the CompileScreen
 * up to the top-level NavHost so the Compiling screen can be navigated
 * to outside this nested graph.
 */
@Composable
fun MainScreen(
    onCompileRequest: () -> Unit
) {
    val nestedNavController = rememberNavController()
    val backStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.Compile
    val context = androidx.compose.ui.platform.LocalContext.current

    var showExitDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Intercept back presses only when on the Compile tab (the root tab).
    // If on other tabs, let the NavController handle popping back to Compile.
    androidx.activity.compose.BackHandler(enabled = currentRoute == Routes.Compile) {
        showExitDialog = true
    }

    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { androidx.compose.material3.Text("Exit App", style = com.adnanfoisal.play2pdf.theme.AppType.title3) },
            text = { androidx.compose.material3.Text("Are you sure you want to exit?", style = com.adnanfoisal.play2pdf.theme.AppType.body) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showExitDialog = false
                    (context as? android.app.Activity)?.finish()
                }) {
                    androidx.compose.material3.Text("Yes", style = com.adnanfoisal.play2pdf.theme.AppType.button)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitDialog = false }) {
                    androidx.compose.material3.Text("No", style = com.adnanfoisal.play2pdf.theme.AppType.button)
                }
            },
            containerColor = com.adnanfoisal.play2pdf.theme.BrandColors.Surface1,
            titleContentColor = com.adnanfoisal.play2pdf.theme.BrandColors.TextPrimary,
            textContentColor = com.adnanfoisal.play2pdf.theme.BrandColors.TextSecondary
        )
    }

    val items = remember {
        listOf(
            BottomNavItem(Routes.Compile, "Compile", AppIcons.Compile),
            BottomNavItem(Routes.History, "History", AppIcons.History),
            BottomNavItem(Routes.Settings, "Settings", AppIcons.Settings)
        )
    }

    Scaffold(
        bottomBar = {
            Play2PdfBottomBar(
                items = items,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        nestedNavController.navigate(route) {
                            popUpTo(nestedNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = nestedNavController,
                startDestination = Routes.Compile
            ) {
                composable(Routes.Compile) {
                    CompileScreen(onCompileRequest = onCompileRequest)
                }
                composable(Routes.History) {
                    HistoryScreen()
                }
                composable(Routes.Settings) {
                    SettingsScreen()
                }
            }
        }
    }
}
