package com.alldownloadmanager.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY status = 'DOWNLOADING' DESC, priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>
    @Query("SELECT * FROM downloads WHERE id = :id") suspend fun get(id: String): DownloadEntity?
    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING') ORDER BY priority DESC, createdAt ASC")
    suspend fun resumable(): List<DownloadEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: DownloadEntity)
    @Query("UPDATE downloads SET status = :status, updatedAt = :at WHERE id = :id") suspend fun setStatus(id: String, status: DownloadStatus, at: Long = System.currentTimeMillis())
    @Query("DELETE FROM downloads WHERE id = :id") suspend fun delete(id: String)
}