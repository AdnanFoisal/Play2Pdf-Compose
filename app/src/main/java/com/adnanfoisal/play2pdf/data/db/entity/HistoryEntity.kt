package com.adnanfoisal.play2pdf.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the PDF history table.
 *
 * Lists (playlist_urls, topics) are stored as JSON strings because Room
 * doesn't natively support List<String> columns — we use a TypeConverter
 * in [com.adnanfoisal.play2pdf.data.db.Converters].
 *
 * Indexed on [createdAtEpochMs] descending so the History screen's
 * "ORDER BY created_at DESC" query is fast even with 100s of rows.
 */
@Entity(
    tableName = "history",
    indices = [Index("createdAtEpochMs"), Index("subject")]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subject: String,
    val author: String,
    val playlistUrlsJson: String,
    val topicsJson: String,
    val theme: String,
    val createdAtEpochMs: Long,
    val pdfUri: String?,
    val pdfSizeBytes: Long?,
    val videoCount: Int?,
    val topicCount: Int?
)
