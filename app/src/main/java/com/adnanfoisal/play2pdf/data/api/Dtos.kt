package com.adnanfoisal.play2pdf.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for the FastAPI backend at https://adnanfoisal-play2pdf.hf.space.
 *
 * Field names match the backend's snake_case JSON exactly (via @Json).
 * The Moshi codegen plugin (KSP) generates the adapters at compile time.
 *
 * Endpoints (per server.py):
 *  GET  /ping                  → PingResponse
 *  POST /extract_topics        → ExtractTopicsRequest  → ExtractTopicsResponse
 *  POST /playlist_meta         → PlaylistMetaRequest   → PlaylistMetaResponse
 *  POST /generate_guide        → GenerateGuideRequest  → binary PDF (application/pdf)
 */

@JsonClass(generateAdapter = true)
data class PingResponse(
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class ExtractTopicsRequest(
    @Json(name = "youtube_key") val youtubeKey: String,
    @Json(name = "gemini_key") val geminiKey: String,
    @Json(name = "playlist_urls") val playlistUrls: List<String>
)

@JsonClass(generateAdapter = true)
data class ExtractTopicsResponse(
    @Json(name = "topics") val topics: List<String>
)

@JsonClass(generateAdapter = true)
data class PlaylistMetaRequest(
    @Json(name = "youtube_key") val youtubeKey: String,
    @Json(name = "playlist_url") val playlistUrl: String
)

@JsonClass(generateAdapter = true)
data class PlaylistMetaResponse(
    @Json(name = "title") val title: String,
    @Json(name = "channel") val channel: String,
    @Json(name = "video_count") val videoCount: Int,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?
)

@JsonClass(generateAdapter = true)
data class GenerateGuideRequest(
    @Json(name = "youtube_key") val youtubeKey: String,
    @Json(name = "gemini_key") val geminiKey: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "author") val author: String,
    @Json(name = "playlist_urls") val playlistUrls: List<String>,
    @Json(name = "topics") val topics: String,
    @Json(name = "theme") val theme: String
)

/**
 * Error envelope returned by FastAPI when an endpoint raises HTTPException.
 * FastAPI's default error JSON looks like: { "detail": "..." }.
 */
@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "detail") val detail: String? = null
)

// --- GET /themes (server-authoritative theme palettes) ------------------------

@JsonClass(generateAdapter = true)
data class ThemesResponse(
    @Json(name = "version") val version: String? = null,
    @Json(name = "themes") val themes: Map<String, ServerTheme>
)

@JsonClass(generateAdapter = true)
data class ServerTheme(
    @Json(name = "font_family") val fontFamily: String,
    @Json(name = "cover") val cover: ServerThemeSurface,
    @Json(name = "page") val page: ServerThemePage
)

@JsonClass(generateAdapter = true)
data class ServerThemeSurface(
    @Json(name = "bg") val bg: List<Int>,
    @Json(name = "text") val text: List<Int>,
    @Json(name = "subtext") val subtext: List<Int>,
    @Json(name = "accent") val accent: List<Int>
)

@JsonClass(generateAdapter = true)
data class ServerThemePage(
    @Json(name = "bg") val bg: List<Int>,
    @Json(name = "text") val text: List<Int>,
    @Json(name = "border") val border: List<Int>,
    @Json(name = "accent") val accent: List<Int>
)
