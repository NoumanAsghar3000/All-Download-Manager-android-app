package com.alldownloadmanager.network

import com.alldownloadmanager.database.DownloadEntity
import com.alldownloadmanager.storage.SafeFileStorage
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

interface Downloader { suspend fun download(item: DownloadEntity, onProgress: suspend (Long, Long) -> Unit): Result<File> }

class HttpDownloader(private val client: OkHttpClient = OkHttpClient()) : Downloader {
    override suspend fun download(item: DownloadEntity, onProgress: suspend (Long, Long) -> Unit): Result<File> = runCatching {
        require(item.url.startsWith("https://") || item.url.startsWith("http://")) { "Only HTTP(S) URLs are supported" }
        val root = File(item.destination).also { it.mkdirs() }
        val target = SafeFileStorage.safeChild(root, item.fileName)
        val start = if (target.exists()) target.length() else 0L
        val request = Request.Builder().url(item.url).apply { if (start > 0) header("Range", "bytes=$start-") }.build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val append = start > 0 && response.code == 206
            val total = if (response.body?.contentLength() ?: -1 > 0) (if (append) start else 0) + response.body!!.contentLength() else -1
            response.body!!.byteStream().use { input ->
                RandomAccessFile(target, "rw").use { out ->
                    if (append) out.seek(start) else out.setLength(0)
                    val buffer = ByteArray(64 * 1024); var done = if (append) start else 0L; var read: Int
                    while (input.read(buffer).also { read = it } != -1) { currentCoroutineContext().ensureActive(); out.write(buffer, 0, read); done += read; onProgress(done, total) }
                }
            }
        }
        target
    }
}