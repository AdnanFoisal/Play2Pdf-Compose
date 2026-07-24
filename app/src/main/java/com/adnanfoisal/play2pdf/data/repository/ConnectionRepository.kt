package com.adnanfoisal.play2pdf.data.repository

import com.adnanfoisal.play2pdf.data.api.Play2PdfApi
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection status wrapper around [Play2PdfApi.ping].
 *
 * Exposes a [StateFlow] so the Settings screen can render a live
 * "Online / Offline / Checking…" indicator without each call site
 * having to manage its own state.
 */
@Singleton
class ConnectionRepository @Inject constructor(
    private val compileRepo: CompileRepository
) {
    private val _status = MutableStateFlow(ConnectionStatus.Offline)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    suspend fun refresh() {
        _status.value = ConnectionStatus.Checking
        _status.value = if (compileRepo.ping()) ConnectionStatus.Online else ConnectionStatus.Offline
    }
}
