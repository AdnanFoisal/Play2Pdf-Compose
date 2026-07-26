package com.adnanfoisal.play2pdf.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the Play2PDF FastAPI backend.
 *
 * All endpoints are at the root (no `/api/v1/` prefix in v1; the plan
 * §Phase H adds the prefix as a forward-compatibility layer, but the
 * deployed backend at https://adnanfoisal-play2pdf.hf.space currently
 * serves them at root — so we hit root and add the prefix when the
 * backend is upgraded).
 *
 * The backend returns:
 *  - `/ping` → JSON
 *  - `/extract_topics` → JSON
 *  - `/playlist_meta` → JSON
 *  - `/generate_guide` → **binary PDF** (`application/pdf`). We receive
 *    it as [ResponseBody] and stream it to a cache file.
 */
interface Play2PdfApi {

    @GET("/ping")
    suspend fun ping(): Response<PingResponse>

    @POST("/extract_topics")
    suspend fun extractTopics(
        @Body req: ExtractTopicsRequest
    ): Response<ExtractTopicsResponse>

    @POST("/playlist_meta")
    suspend fun playlistMeta(
        @Body req: PlaylistMetaRequest
    ): Response<PlaylistMetaResponse>

    @retrofit2.http.Streaming
    @POST("/generate_guide")
    suspend fun generateGuide(
        @Body req: GenerateGuideRequest
    ): Response<ResponseBody>
}
