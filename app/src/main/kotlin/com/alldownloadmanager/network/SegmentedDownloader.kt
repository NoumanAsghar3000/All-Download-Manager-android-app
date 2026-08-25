package com.alldownloadmanager.network

import com.alldownloadmanager.database.DownloadEntity
import com.alldownloadmanager.storage.SafeFileStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong

/**
 * Uses independent byte ranges only after the server confirms range support with a 206 response.
 * A failed or ignored range request is surfaced instead of corrupting the target.
 */
class SegmentedDownloader(private val client: OkHttpClient = OkHttpClient(), private val connections: Int = 4) {
    suspend fun download(item: DownloadEntity, totalBytes: Long, onProgress: suspend (Long, Long) -> Unit): Result<File> = runCatching {
        require(totalBytes > 0) { "A known content length is required for segmented downloads" }
        val root = File(item.destination).also { it.mkdirs() }
        val target = SafeFileStorage.safeChild(root, item.fileName)
        RandomAccessFile(target, "rw").use { it.setLength(totalBytes) }
        val completed = AtomicLong(0)
        coroutineScope {
            val parts = connections.coerceIn(2, 16)
            (0 until parts).map { index ->
                async {
                    val start = totalBytes * index / parts
                    val end = totalBytes * (index + 1) / parts - 1
                    val request = Request.Builder().url(item.url).header("Range", "bytes=$start-$end").build()
                    client.newCall(request).execute().use { response ->
                        check(response.code == 206) { "Server does not support HTTP ranges" }
                        response.body!!.byteStream().use { input ->
                            RandomAccessFile(target, "rw").use { output ->
                                output.seek(start); val buffer = ByteArray(64 * 1024); var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    currentCoroutineContext().ensureActive()
                                    output.write(buffer, 0, read)
                                    onProgress(completed.addAndGet(read.toLong()), totalBytes)
                                }
                            }
                        }
                    }
                }
            }.awaitAll()
        }
        target
    }
}