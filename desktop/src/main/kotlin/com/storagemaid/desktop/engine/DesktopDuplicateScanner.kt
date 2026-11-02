package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.DesktopDuplicateGroup
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class DesktopDuplicateScanner {

    fun scanDuplicates(directories: List<File>, minSizeBytes: Long = 1024L): List<DesktopDuplicateGroup> {
        val sizeMap = mutableMapOf<Long, MutableList<File>>()

        for (dir in directories) {
            walk(dir, sizeMap, minSizeBytes, 0)
        }

        val candidates = sizeMap.filter { it.value.size > 1 }
        val hashMap = mutableMapOf<String, MutableList<File>>()

        for ((size, files) in candidates) {
            for (f in files) {
                val hash = computeSha256(f)
                if (hash != null) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(f)
                }
            }
        }

        return hashMap
            .filter { it.value.size > 1 }
            .map { (hash, files) ->
                val sorted = files.sortedBy { it.lastModified() }
                // Default: keep the oldest original, select the remaining duplicates
                val selected = sorted.drop(1).toMutableSet()
                DesktopDuplicateGroup(
                    hash = hash,
                    sizeBytes = sorted.first().length(),
                    files = sorted,
                    selectedFiles = selected
                )
            }
            .sortedByDescending { it.reclaimableBytes }
    }

    private fun walk(dir: File, sizeMap: MutableMap<Long, MutableList<File>>, minSize: Long, depth: Int) {
        if (depth > 5 || !dir.exists() || !dir.canRead()) return
        val list = dir.listFiles() ?: return

        for (f in list) {
            try {
                if (f.isFile) {
                    val len = f.length()
                    if (len >= minSize) {
                        sizeMap.getOrPut(len) { mutableListOf() }.add(f)
                    }
                } else if (f.isDirectory && !f.name.startsWith(".")) {
                    walk(f, sizeMap, minSize, depth + 1)
                }
            } catch (_: Exception) {}
        }
    }

    private fun computeSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(65536)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteSelected(group: DesktopDuplicateGroup): Pair<Int, Long> {
        var count = 0
        var freed = 0L
        for (f in group.selectedFiles) {
            try {
                val len = f.length()
                if (f.delete()) {
                    count++
                    freed += len
                }
            } catch (_: Exception) {}
        }
        return count to freed
    }
}
