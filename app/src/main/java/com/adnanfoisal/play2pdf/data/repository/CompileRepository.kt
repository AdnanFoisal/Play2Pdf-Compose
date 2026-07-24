package com.adnanfoisal.play2pdf.data.repository

import com.adnanfoisal.play2pdf.data.api.GenerateGuideRequest
import com.adnanfoisal.play2pdf.data.api.Play2PdfApi
import com.adnanfoisal.play2pdf.data.api.ExtractTopicsRequest
import com.adnanfoisal.play2pdf.data.api.PlaylistMetaRequest
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import kotlinx.coroutines.flow.first
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result wrapper for repository calls — surfaces a user-friendly message
 * for both expected (API 4xx) and unexpected (network) errors.
 */
sealed class CompileResult {
    data class Success(val pdfFile: File, val sizeBytes: Long) : CompileResult()
    data class Failure(val message: String, val httpCode: Int? = null) : CompileResult()
}

/**
 * Wraps [Play2PdfApi] with:
 *  - Runtime backend URL (read from [SettingsRepository] on each call so
 *    the user can change it in Settings without restarting the app).
 *  - PDF response streaming to a cache file (so we can pass it to the
 *    FileProvider + SavePdfToDownloadsUseCase without loading it into memory).
 *  - User-friendly error extraction from FastAPI's `{"detail": "..."}` envelope.
 */
@Singleton
class CompileRepository @Inject constructor(
    private val api: Play2PdfApi,
    private val settings: SettingsRepository,
    private val cacheDir: File
) {

    /** Quick liveness check. Returns true on HTTP 200 + status=="awake". */
    suspend fun ping(): Boolean = try {
        val resp = api.ping()
        resp.isSuccessful && resp.body()?.status == "awake"
    } catch (t: Throwable) {
        false
    }

    /** Auto-extract syllabus topics from the playlist's video titles. */
    suspend fun extractTopics(playlistUrls: List<String>): Result<List<String>> = runCatching {
        val s = settings.settings.first()
        val resp = api.extractTopics(
            ExtractTopicsRequest(
                youtubeKey = s.youtubeApiKey,
                geminiKey = s.geminiApiKey,
                playlistUrls = playlistUrls
            )
        )
        if (resp.isSuccessful) {
            resp.body()?.topics ?: emptyList()
        } else {
            throw RuntimeException(parseError(resp.errorBody()?.string()))
        }
    }

    /** Fetch metadata for a single playlist (title, channel, video count). */
    suspend fun fetchPlaylistMeta(playlistUrl: String): Result<PlaylistMetaResult> = runCatching {
        val s = settings.settings.first()
        val resp = api.playlistMeta(
            PlaylistMetaRequest(
                youtubeKey = s.youtubeApiKey,
                playlistUrl = playlistUrl
            )
        )
        if (resp.isSuccessful) {
            val b = resp.body() ?: throw RuntimeException("Empty response")
            PlaylistMetaResult(
                title = b.title,
                channel = b.channel,
                videoCount = b.videoCount,
                thumbnailUrl = b.thumbnailUrl
            )
        } else {
            throw RuntimeException(parseError(resp.errorBody()?.string()))
        }
    }

    /**
     * Generate a study guide PDF. Streams the response to a cache file
     * and returns it via [CompileResult].
     */
    suspend fun generateGuide(
        subject: String,
        author: String,
        playlistUrls: List<String>,
        topics: List<String>,
        theme: PdfTheme
    ): CompileResult {
        val s = settings.settings.first()
        return try {
            val resp = api.generateGuide(
                GenerateGuideRequest(
                    youtubeKey = s.youtubeApiKey,
                    geminiKey = s.geminiApiKey,
                    subject = subject,
                    author = author,
                    playlistUrls = playlistUrls,
                    topics = topics.joinToString(","),
                    theme = theme.apiName
                )
            )
            if (!resp.isSuccessful) {
                val errBody = resp.errorBody()?.string()
                return CompileResult.Failure(
                    message = parseError(errBody),
                    httpCode = resp.code()
                )
            }
            val body: ResponseBody = resp.body()
                ?: return CompileResult.Failure("Empty response from backend", resp.code())

            val outFile = File(cacheDir, "play2pdf_${System.currentTimeMillis()}.pdf")
            body.byteStream().use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            CompileResult.Success(outFile, outFile.length())
        } catch (t: Throwable) {
            CompileResult.Failure(t.message ?: "Unknown network error")
        }
    }

    /** Try to pull a human-readable error out of FastAPI's `{"detail": ...}` envelope. */
    private fun parseError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Unknown error"
        // Quick regex — avoid a full JSON parse for a one-liner.
        val match = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)
        return match?.groupValues?.getOrNull(1) ?: errorBody.take(200)
    }
}

data class PlaylistMetaResult(
    val title: String,
    val channel: String,
    val videoCount: Int,
    val thumbnailUrl: String?
)
