package com.alldownloadmanager.provider

data class MediaFormat(val url: String, val label: String, val mimeType: String, val sizeBytes: Long? = null)
interface MediaProvider { suspend fun resolve(url: String): Result<List<MediaFormat>> }
interface TorrentDownloader { suspend fun add(uri: String): Result<String>; suspend fun pause(id: String); suspend fun resume(id: String) }
interface MegaDownloader { suspend fun resolve(publicUrl: String): Result<List<MediaFormat>> }
interface ArchiveManager { suspend fun inspect(file: java.io.File): Result<List<String>>; suspend fun extractSelected(file: java.io.File, entries: List<String>, destination: java.io.File): Result<List<java.io.File>> }
class UnsupportedProvider(private val reason: String) : MediaProvider { override suspend fun resolve(url: String) = Result.failure<List<MediaFormat>>(UnsupportedOperationException(reason)) }