package com.adnanfoisal.play2pdf.data.repository

import android.util.Log
import com.adnanfoisal.play2pdf.data.api.Play2PdfApi
import com.adnanfoisal.play2pdf.domain.model.ConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of one connectivity/credential check.
 *
 * [detail] carries the ACTUAL reason on failure (HTTP code + Google's own
 * error message) so "Offline" is never a dead end — the previous version
 * swallowed the status code and the exception entirely.
 */
data class ApiCheck(
    val status: ConnectionStatus,
    val detail: String? = null
)

/**
 * Live status of the three things a compile depends on: our backend, the
 * YouTube Data API key, and the Gemini API key.
 *
 * Design notes (learned the hard way):
 *  - Key validation must NOT depend on one hardcoded model name. The old
 *    Gemini check POSTed to `models/gemini-3.5-flash-lite:generateContent`,
 *    so a perfectly valid key reported "Offline" whenever that particular
 *    model wasn't available to it. We now list models instead, which
 *    validates the key itself — and we additionally report whether the
 *    models the backend needs are actually present.
 *  - Use quota-cheap endpoints: `i18nLanguages` costs 1 unit vs a
 *    `videos?chart=mostPopular` query.
 */
@Singleton
class ConnectionRepository @Inject constructor(
    private val compileRepo: CompileRepository,
    private val okHttpClient: OkHttpClient
) {
    private val _backend = MutableStateFlow(ApiCheck(ConnectionStatus.Offline))
    val backend: StateFlow<ApiCheck> = _backend.asStateFlow()

    private val _youtube = MutableStateFlow(ApiCheck(ConnectionStatus.Offline))
    val youtube: StateFlow<ApiCheck> = _youtube.asStateFlow()

    private val _gemini = MutableStateFlow(ApiCheck(ConnectionStatus.Offline))
    val gemini: StateFlow<ApiCheck> = _gemini.asStateFlow()

    suspend fun refresh(youtubeKey: String, geminiKey: String) {
        _backend.value = ApiCheck(ConnectionStatus.Checking)
        _youtube.value = ApiCheck(ConnectionStatus.Checking)
        _gemini.value = ApiCheck(ConnectionStatus.Checking)

        coroutineScope {
            launch {
                _backend.value =
                    if (compileRepo.ping()) ApiCheck(ConnectionStatus.Online)
                    else ApiCheck(ConnectionStatus.Offline, "No response — the Space may be waking up (30-60s)")
            }
            launch { _youtube.value = testYoutube(youtubeKey) }
            launch { _gemini.value = testGemini(geminiKey) }
        }
    }

    private suspend fun testYoutube(key: String): ApiCheck {
        val k = key.trim()
        if (k.isBlank()) return ApiCheck(ConnectionStatus.Offline, "No API key entered")
        return withContext(Dispatchers.IO) {
            // Cheapest possible authenticated call (1 quota unit).
            get("https://www.googleapis.com/youtube/v3/i18nLanguages?part=snippet&key=$k", "YouTube")
                .let { (ok, code, body) ->
                    if (ok) ApiCheck(ConnectionStatus.Online, "Key valid")
                    else ApiCheck(ConnectionStatus.Offline, describe(code, body, "YouTube Data API v3"))
                }
        }
    }

    private suspend fun testGemini(key: String): ApiCheck {
        val k = key.trim()
        if (k.isBlank()) return ApiCheck(ConnectionStatus.Offline, "No API key entered")
        return withContext(Dispatchers.IO) {
            val (ok, code, body) = get(
                "https://generativelanguage.googleapis.com/v1beta/models?key=$k",
                "Gemini"
            )
            if (!ok) return@withContext ApiCheck(
                ConnectionStatus.Offline, describe(code, body, "Gemini API")
            )

            // Key is valid. Now check the models the BACKEND actually calls —
            // if they're absent, compiling will fail server-side even though
            // the key is fine, so say so plainly.
            val available = MODEL_NAME_REGEX.findAll(body.orEmpty())
                .map { it.groupValues[1] }
                .toList()
            val missing = REQUIRED_MODELS.filter { needed ->
                available.none { it == needed || it.startsWith(needed) }
            }
            Log.i(TAG, "Gemini models available (${available.size}): ${available.take(15)}")
            if (missing.isEmpty()) {
                ApiCheck(ConnectionStatus.Online, "Key valid - ${available.size} models")
            } else {
                ApiCheck(
                    ConnectionStatus.Online,
                    "Key valid, but this key has no ${missing.joinToString()} - compiles may fail"
                )
            }
        }
    }

    /** GET helper returning (success, httpCode, body). Never throws. */
    private fun get(url: String, label: String): Triple<Boolean, Int?, String?> = try {
        okHttpClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                Log.w(TAG, "$label check failed: HTTP ${response.code} — ${body?.take(300)}")
            }
            Triple(response.isSuccessful, response.code, body)
        }
    } catch (e: Exception) {
        Log.w(TAG, "$label check error: ${e.message}", e)
        Triple(false, null, null)
    }

    /** Turn a Google error payload into one readable line. */
    private fun describe(code: Int?, body: String?, api: String): String {
        val message = body?.let { GOOGLE_MESSAGE_REGEX.find(it)?.groupValues?.getOrNull(1) }
        return when {
            code == null -> "No network connection"
            code == 400 -> message ?: "Invalid API key"
            code == 403 -> message ?: "$api not enabled for this key, or quota exceeded"
            code == 429 -> "Quota exceeded — try again later"
            else -> "HTTP $code${message?.let { ": $it" } ?: ""}"
        }
    }

    private companion object {
        const val TAG = "ConnectionRepo"
        // Keep in sync with backend MODEL_EXTRACT / MODEL_MATCH — the live
        // values are also reported by GET /health under "models".
        val REQUIRED_MODELS = listOf("gemini-3.5-flash-lite", "gemini-3.8-flash")
        val MODEL_NAME_REGEX = Regex("\"name\"\\s*:\\s*\"models/([^\"]+)\"")
        val GOOGLE_MESSAGE_REGEX = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
    }
}
