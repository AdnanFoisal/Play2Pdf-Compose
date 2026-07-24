package com.adnanfoisal.play2pdf.data.repository

import com.adnanfoisal.play2pdf.data.db.dao.HistoryDao
import com.adnanfoisal.play2pdf.data.db.entity.HistoryEntity
import com.adnanfoisal.play2pdf.domain.model.PdfHistory
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRUD wrapper over [HistoryDao] that maps Room entities ↔ domain models.
 *
 * Caps history at 30 entries per the v2.0 spec — after every insert we
 * delete the oldest extras. This is a no-op if the table has < 30 rows.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter =
        moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

    fun observeAll(): Flow<List<PdfHistory>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun search(query: String): Flow<List<PdfHistory>> =
        dao.search(query).map { rows -> rows.map { it.toDomain() } }

    fun observeSince(sinceEpochMs: Long): Flow<List<PdfHistory>> =
        dao.observeSince(sinceEpochMs).map { rows -> rows.map { it.toDomain() } }

    suspend fun getById(id: Long): PdfHistory? = dao.getById(id)?.toDomain()

    suspend fun insert(entry: PdfHistory): Long {
        val id = dao.insert(entry.toEntity())
        // Enforce the 30-row cap.
        val total = dao.count()
        if (total > 30) dao.deleteOldest(total - 30)
        return id
    }

    suspend fun update(entry: PdfHistory) = dao.update(entry.toEntity())

    suspend fun delete(entry: PdfHistory) = dao.delete(entry.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    // --- Mappers ------------------------------------------------------

    private fun HistoryEntity.toDomain(): PdfHistory = PdfHistory(
        id = id,
        subject = subject,
        author = author,
        playlistUrls = stringListAdapter.fromJson(playlistUrlsJson) ?: emptyList(),
        topics = stringListAdapter.fromJson(topicsJson) ?: emptyList(),
        theme = PdfTheme.fromApiName(theme),
        createdAtEpochMs = createdAtEpochMs,
        pdfUri = pdfUri,
        pdfSizeBytes = pdfSizeBytes,
        videoCount = videoCount,
        topicCount = topicCount
    )

    private fun PdfHistory.toEntity(): HistoryEntity = HistoryEntity(
        id = id,
        subject = subject,
        author = author,
        playlistUrlsJson = stringListAdapter.toJson(playlistUrls),
        topicsJson = stringListAdapter.toJson(topics),
        theme = theme.apiName,
        createdAtEpochMs = createdAtEpochMs,
        pdfUri = pdfUri,
        pdfSizeBytes = pdfSizeBytes,
        videoCount = videoCount,
        topicCount = topicCount
    )
}
