package com.adnanfoisal.play2pdf.data.repository

import com.adnanfoisal.play2pdf.data.api.Play2PdfApi
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val compileRepo: CompileRepository,
    private val okHttpClient: okhttp3.OkHttpClient
) {
    private val _status = MutableStateFlow(ConnectionStatus.Offline)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _youtubeStatus = MutableStateFlow(ConnectionStatus.Offline)
    val youtubeStatus: StateFlow<ConnectionStatus> = _youtubeStatus.asStateFlow()

    private val _geminiStatus = MutableStateFlow(ConnectionStatus.Offline)
    val geminiStatus: StateFlow<ConnectionStatus> = _geminiStatus.asStateFlow()

    suspend fun refresh(youtubeKey: String, geminiKey: String) {
        _status.value = ConnectionStatus.Checking
        _youtubeStatus.value = ConnectionStatus.Checking
        _geminiStatus.value = ConnectionStatus.Checking

        coroutineScope {
            launch {
                _status.value = if (compileRepo.ping()) ConnectionStatus.Online else ConnectionStatus.Offline
            }
            launch {
                _youtubeStatus.value = testYoutube(youtubeKey)
            }
            launch {
                _geminiStatus.value = testGemini(geminiKey)
            }
        }
    }

    private suspend fun testYoutube(key: String): ConnectionStatus {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) return ConnectionStatus.Offline
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://youtube.googleapis.com/youtube/v3/videos?part=id&chart=mostPopular&maxResults=1&key=$trimmedKey"
                val request = okhttp3.Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) ConnectionStatus.Online else ConnectionStatus.Offline
                }
            } catch (e: Exception) {
                ConnectionStatus.Offline
            }
        }
    }

    private suspend fun testGemini(key: String): ConnectionStatus {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) return ConnectionStatus.Offline
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$trimmedKey"
                val json = "{\"contents\": [{\"parts\":[{\"text\": \"hi\"}]}]}"
                val jsonMediaType = "application/json".toMediaType()
                val body = json.toRequestBody(jsonMediaType)
                val request = okhttp3.Request.Builder().url(url).post(body).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) ConnectionStatus.Online else ConnectionStatus.Offline
                }
            } catch (e: Exception) {
                ConnectionStatus.Offline
            }
        }
    }
}
