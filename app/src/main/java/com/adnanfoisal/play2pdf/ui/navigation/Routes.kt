package com.adnanfoisal.play2pdf.ui.navigation

/**
 * Navigation routes for Play2PDF.
 *
 * The 3 main tabs (Compile / History / Settings) live inside a
 * [MainScreen] Scaffold with a bottom bar. Splash, Onboarding, and
 * Compiling are full-screen routes outside the main scaffold.
 */
object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Main = "main"
    const val Compile = "main/compile"
    const val History = "main/history"
    const val Settings = "main/settings"
    const val Compiling = "compiling"

    /** Build the Compiling route with the request payload encoded as query
     *  params. We pass these via a saved-state handle in the ViewModel
     *  instead of via the URL (URLs are limited in length and playlists
     *  + topics can be large). */
    fun compiling(): String = Compiling
}
