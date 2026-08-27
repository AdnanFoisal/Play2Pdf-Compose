package com.adnanfoisal.play2pdf.data.repository

import androidx.compose.ui.graphics.Color
import com.adnanfoisal.play2pdf.data.api.Play2PdfApi
import com.adnanfoisal.play2pdf.data.api.ServerTheme
import com.adnanfoisal.play2pdf.ui.compile.components.PdfThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live theme palettes published by GET /themes.
 *
 * The generated fallback in PdfThemePalette.kt mirrors server truth at
 * compile time; this object lets the app pick up palette changes at
 * runtime without a release. [PdfTheme.palette] consults it first.
 */
object ServerThemePalettes {
    @Volatile
    var byApiName: Map<String, PdfThemePalette> = emptyMap()
        private set

    fun update(palettes: Map<String, PdfThemePalette>) {
        byApiName = palettes
    }
}

/** [r, g, b] (0-255) from the API -> Compose color. */
private fun rgb(list: List<Int>): Color = Color(
    red = (list.getOrElse(0) { 0 }) / 255f,
    green = (list.getOrElse(1) { 0 }) / 255f,
    blue = (list.getOrElse(2) { 0 }) / 255f
)

private fun ServerTheme.toPalette(): PdfThemePalette = PdfThemePalette(
    coverBg = rgb(cover.bg),
    coverText = rgb(cover.text),
    coverSubtext = rgb(cover.subtext),
    pageBg = rgb(page.bg),
    pageText = rgb(page.text),
    pageBorder = rgb(page.border),
    accent = rgb(cover.accent),
    serif = fontFamily == "Times",
    fontName = fontFamily
)

/**
 * Fetches the server-authoritative theme palettes (GET /themes) and
 * publishes them to [ServerThemePalettes]. Failures are silent — the
 * generated offline fallback is already truthful, so there is nothing
 * to recover to; we simply retry on the next [refresh].
 */
@Singleton
class ThemeRepository @Inject constructor(
    private val api: Play2PdfApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()

    fun refresh() {
        scope.launch {
            try {
                val resp = api.themes()
                if (resp.isSuccessful) {
                    val body = resp.body() ?: return@launch
                    ServerThemePalettes.update(
                        body.themes.mapValues { (_, t) -> t.toPalette() }
                    )
                    _lastUpdated.value = System.currentTimeMillis()
                }
            } catch (_: Exception) {
                // Offline / cold Space — keep the generated fallback.
            }
        }
    }
}
