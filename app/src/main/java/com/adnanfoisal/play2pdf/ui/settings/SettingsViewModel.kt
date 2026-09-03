package com.adnanfoisal.play2pdf.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.data.repository.ConnectionRepository
import com.adnanfoisal.play2pdf.domain.model.ConnectionStatus
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.model.UserSettings
import com.adnanfoisal.play2pdf.domain.usecase.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Offline,
    val youtubeStatus: ConnectionStatus = ConnectionStatus.Offline,
    val geminiStatus: ConnectionStatus = ConnectionStatus.Offline,
    // Why a check failed (HTTP code + Google's own message), so "Offline"
    // is actionable instead of a dead end.
    val connectionDetail: String? = null,
    val youtubeDetail: String? = null,
    val geminiDetail: String? = null,
    val testingConnection: Boolean = false,
    val savedMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val connection: ConnectionRepository,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = kotlinx.coroutines.flow.combine(
        repo.settings,
        connection.backend,
        connection.youtube,
        connection.gemini
    ) { s, backend, yt, gemini ->
        SettingsUiState(
            settings = s,
            connectionStatus = backend.status,
            youtubeStatus = yt.status,
            geminiStatus = gemini.status,
            connectionDetail = backend.detail,
            youtubeDetail = yt.detail,
            geminiDetail = gemini.detail
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _transient = MutableStateFlow<SettingsUiState?>(null)
    val transient: StateFlow<SettingsUiState?> = _transient.asStateFlow()

    init {
        // Kick off a connection test on first load so the indicator isn't stuck on "Offline".
        viewModelScope.launch { 
            val s = repo.settings.first()
            testConnectionUseCase(s.youtubeApiKey, s.geminiApiKey) 
        }
    }

    // --- Per-field setters with 500ms debounced save indicator ------------
    // Key/URL edits also re-run the connection check (debounced), so the
    // indicators update as soon as you finish typing instead of waiting for
    // a manual "Test Connection" press.

    fun setYoutubeKey(v: String) = save(recheck = true) { it.copy(youtubeApiKey = v) }
    fun setGeminiKey(v: String) = save(recheck = true) { it.copy(geminiApiKey = v) }
    fun setBackendUrl(v: String) = save(recheck = true) { it.copy(backendUrl = v) }
    fun setUserName(v: String) = save { it.copy(userName = v) }
    fun setTheme(t: PdfTheme) = save { it.copy(selectedTheme = t) }
    fun setSoundEnabled(v: Boolean) = save { it.copy(soundEnabled = v) }
    fun setHapticsEnabled(v: Boolean) = save { it.copy(hapticsEnabled = v) }

    private var recheckJob: Job? = null

    private fun scheduleRecheck() {
        recheckJob?.cancel()
        recheckJob = viewModelScope.launch {
            delay(900)  // wait for typing to settle
            val s = repo.settings.first()
            testConnectionUseCase(s.youtubeApiKey, s.geminiApiKey)
        }
    }

    private fun save(recheck: Boolean = false, transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            repo.update(transform)
            // Brief "saved" indicator — collapses after 1.5s.
            _transient.update { it?.copy(savedMessage = "Saved") ?: SettingsUiState(savedMessage = "Saved") }
            if (recheck) scheduleRecheck()
            delay(1500)
            _transient.update { it?.copy(savedMessage = null) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val s = repo.settings.first()
            testConnectionUseCase(s.youtubeApiKey, s.geminiApiKey)
        }
    }
}
