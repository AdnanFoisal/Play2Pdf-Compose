package com.adnanfoisal.play2pdf.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.adnanfoisal.play2pdf.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for PDF history.
 *
 * Methods return [Flow]s so the History screen reactively recomposes
 * when entries are added, updated, or deleted.
 *
 * Per the v2.0 plan we cap history at 30 entries — [insert] enforces
 * this by deleting the oldest extras after each insert.
 */
@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE subject LIKE '%' || :query || '%' ORDER BY createdAtEpochMs DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE createdAtEpochMs >= :sinceEpochMs ORDER BY createdAtEpochMs DESC")
    fun observeSince(sinceEpochMs: Long): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Update
    suspend fun update(entity: HistoryEntity)

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE id IN (SELECT id FROM history ORDER BY createdAtEpochMs ASC LIMIT :n)")
    suspend fun deleteOldest(n: Int)

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}
