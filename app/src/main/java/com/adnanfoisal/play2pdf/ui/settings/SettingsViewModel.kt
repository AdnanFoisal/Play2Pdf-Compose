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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Offline,
    val testingConnection: Boolean = false,
    val savedMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val connection: ConnectionRepository,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repo.settings,
        connection.status
    ) { s, cs ->
        SettingsUiState(settings = s, connectionStatus = cs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _transient = MutableStateFlow<SettingsUiState?>(null)
    val transient: StateFlow<SettingsUiState?> = _transient.asStateFlow()

    init {
        // Kick off a connection test on first load so the indicator isn't stuck on "Offline".
        viewModelScope.launch { testConnectionUseCase() }
    }

    // --- Per-field setters with 500ms debounced save indicator ------------

    fun setYoutubeKey(v: String) = save { it.copy(youtubeApiKey = v) }
    fun setGeminiKey(v: String) = save { it.copy(geminiApiKey = v) }
    fun setBackendUrl(v: String) = save { it.copy(backendUrl = v) }
    fun setUserName(v: String) = save { it.copy(userName = v) }
    fun setTheme(t: PdfTheme) = save { it.copy(selectedTheme = t) }
    fun setSoundEnabled(v: Boolean) = save { it.copy(soundEnabled = v) }
    fun setHapticsEnabled(v: Boolean) = save { it.copy(hapticsEnabled = v) }

    private fun save(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            repo.update(transform)
            // Brief "saved" indicator — collapses after 1.5s.
            _transient.update { it?.copy(savedMessage = "Saved") ?: SettingsUiState(savedMessage = "Saved") }
            delay(1500)
            _transient.update { it?.copy(savedMessage = null) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            testConnectionUseCase()
        }
    }
