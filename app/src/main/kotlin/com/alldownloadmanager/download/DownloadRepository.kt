package com.alldownloadmanager.download
import android.content.Context
import com.alldownloadmanager.database.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DownloadRepository(context: Context) {
    private val dao = AppDatabase.get(context).downloads()
    fun observe(): Flow<List<DownloadEntity>> = dao.observeAll()
    suspend fun add(url: String, name: String? = null): DownloadEntity {
        val item = DownloadEntity(UUID.randomUUID().toString(), url, com.alldownloadmanager.storage.SafeFileStorage.safeName(url, name), com.alldownloadmanager.storage.SafeFileStorage.defaultDirectory(context).absolutePath)
        dao.upsert(item); return item
    }
    suspend fun update(item: DownloadEntity) = dao.upsert(item)
    suspend fun status(id: String, status: DownloadStatus) = dao.setStatus(id, status)
    suspend fun remove(id: String) = dao.delete(id)
    private val context = context
}