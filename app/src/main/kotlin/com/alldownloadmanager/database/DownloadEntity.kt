package com.alldownloadmanager.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETE, FAILED, CANCELLED }

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val destination: String,
    val totalBytes: Long = -1,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val error: String? = null,
    val supportsRange: Boolean = false
)