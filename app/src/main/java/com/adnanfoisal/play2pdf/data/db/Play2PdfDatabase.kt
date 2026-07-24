package com.adnanfoisal.play2pdf.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.adnanfoisal.play2pdf.data.db.dao.HistoryDao
import com.adnanfoisal.play2pdf.data.db.entity.HistoryEntity

/**
 * Room database for Play2PDF.
 *
 * Currently a single table: [HistoryEntity]. Settings are persisted via
 * DataStore (not Room) because they're a small fixed set of primitives
 * — DataStore is the right tool for that.
 *
 * Version 1. Bump + add a [Migration] in [com.adnanfoisal.play2pdf.di.AppModule]
 * if the schema changes.
 */
@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class Play2PdfDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "play2pdf.db"
    }
}
