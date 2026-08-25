package com.alldownloadmanager.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class SafeFileStorageTest {
    @Test fun sanitizesUnsafeFilenameCharacters() {
        assertEquals("episode_01_.mp4", SafeFileStorage.safeName("", "episode:/01?.mp4"))
    }

    @Test fun rejectsPathTraversal() {
        val root = File("build/test-downloads").canonicalFile
        assertThrows(IllegalArgumentException::class.java) {
            SafeFileStorage.safeChild(root, "../outside.bin")
        }
    }
}