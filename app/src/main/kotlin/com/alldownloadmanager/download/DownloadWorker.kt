package com.alldownloadmanager.download
import android.content.Context
import androidx.work.*
import com.alldownloadmanager.database.*
import com.alldownloadmanager.network.HttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("id") ?: return@withContext Result.failure()
        val dao = AppDatabase.get(applicationContext).downloads(); val item = dao.get(id) ?: return@withContext Result.failure()
        dao.upsert(item.copy(status = DownloadStatus.DOWNLOADING, error = null))
        val result = HttpDownloader().download(item) { done, total ->
            dao.upsert(item.copy(downloadedBytes = done, totalBytes = total, status = DownloadStatus.DOWNLOADING, updatedAt = System.currentTimeMillis()))
            setProgress(workDataOf("done" to done, "total" to total))
        }
        result.fold({ dao.upsert(item.copy(downloadedBytes = it.length(), status = DownloadStatus.COMPLETE)); Result.success() },
            { dao.upsert(item.copy(status = DownloadStatus.FAILED, error = it.message)); if (runAttemptCount < 3) Result.retry() else Result.failure() })
    }
    companion object {
        fun request(id: String) = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("id" to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
}