package com.alldownloadmanager.archive

import com.alldownloadmanager.provider.ArchiveManager
import java.io.File
import java.util.zip.ZipFile

class ZipArchiveManager : ArchiveManager {
    override suspend fun inspect(file: File): Result<List<String>> = runCatching {
        ZipFile(file).use { zip -> zip.entries().asSequence().filterNot { it.isDirectory }.map { it.name }.toList() }
    }
    override suspend fun extractSelected(file: File, entries: List<String>, destination: File): Result<List<File>> = runCatching {
        destination.mkdirs()
        ZipFile(file).use { zip ->
            entries.map { entryName ->
                val entry = zip.getEntry(entryName) ?: error("Archive entry not found: $entryName")
                require(!entry.isDirectory && !entryName.startsWith("/") && !entryName.contains("../") && !entryName.contains("..\\"))
                val output = File(destination, entryName).canonicalFile
                require(output.toPath().startsWith(destination.canonicalFile.toPath())) { "Unsafe archive path" }
                output.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input -> output.outputStream().use { input.copyTo(it) } }
                output
            }
        }
    }
}