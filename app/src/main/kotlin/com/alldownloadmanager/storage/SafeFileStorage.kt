package com.alldownloadmanager.storage
import android.content.Context
import android.os.Environment
import java.io.File
import java.net.URLDecoder

object SafeFileStorage {
    fun defaultDirectory(context: Context): File = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    fun safeName(url: String, suggested: String? = null): String {
        val raw = suggested?.takeIf { it.isNotBlank() } ?: URLDecoder.decode(url.substringAfterLast('/').substringBefore('?'), "UTF-8")
        return raw.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(180).ifBlank { "download.bin" }
    }
    fun safeChild(root: File, name: String): File {
        require(!name.contains("..") || name == "..") { "Unsafe path" }
        val file = File(root, safeName("", name)).canonicalFile
        require(file.toPath().startsWith(root.canonicalFile.toPath())) { "Unsafe path" }
        return file
    }
}