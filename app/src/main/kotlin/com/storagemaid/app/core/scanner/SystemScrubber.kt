package com.storagemaid.app.core.scanner

import com.storagemaid.app.core.model.JunkCategory
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.RiskLevel
import java.io.File

class SystemScrubber {

    private val tempExtensions = setOf(
        "tmp", "msf.tmp", "bak", "part", "crdownload", "log", "dmp", "tombstone", "old"
    )

    private val tempFolderNames = setOf(
        ".thumbnails", "thumbnails", "LOST.DIR", ".cache", "cache", "temp", "tmp"
    )

    fun scan(
        storageRoots: List<File>,
        onProgress: (File) -> Unit
    ): Pair<List<JunkItem>, List<JunkItem>> {
        val tempItems = mutableListOf<JunkItem>()
        val emptyFolders = mutableListOf<JunkItem>()

        for (root in storageRoots) {
            scanDirectoryRecursive(root, tempItems, emptyFolders, onProgress)
        }

        return tempItems to emptyFolders
    }

    private fun scanDirectoryRecursive(
        dir: File,
        tempItems: MutableList<JunkItem>,
        emptyFolders: MutableList<JunkItem>,
        onProgress: (File) -> Unit
    ) {
        val children = try {
            dir.listFiles()
        } catch (_: Exception) {
            null
        } ?: return

        onProgress(dir)

        if (children.isEmpty() && !isSystemRootDirectory(dir)) {
            emptyFolders.add(
                JunkItem(
                    id = "empty_${dir.absolutePath.hashCode()}",
                    path = dir.absolutePath,
                    name = dir.name.ifBlank { dir.absolutePath },
                    sizeBytes = 0L,
                    category = JunkCategory.EMPTY_FOLDERS,
                    riskLevel = RiskLevel.SAFE,
                    isDirectory = true,
                    fileCount = 0,
                    isSelected = true
                )
            )
            return
        }

        for (file in children) {
            onProgress(file)

            if (file.isDirectory) {
                // Check if directory itself is a known temp/cache container
                if (tempFolderNames.contains(file.name)) {
                    val (size, count) = calculateDirectoryStats(file)
                    if (count > 0 || size > 0) {
                        tempItems.add(
                            JunkItem(
                                id = "tempdir_${file.absolutePath.hashCode()}",
                                path = file.absolutePath,
                                name = file.name,
                                sizeBytes = size,
                                category = JunkCategory.SYSTEM_TEMP,
                                riskLevel = RiskLevel.SAFE,
                                isDirectory = true,
                                fileCount = count,
                                isSelected = true
                            )
                        )
                    }
                } else if (!file.name.equals("Android", ignoreCase = true)) {
                    // Do not deep scan Android/ data here (handled by CorpseFinder)
                    scanDirectoryRecursive(file, tempItems, emptyFolders, onProgress)
                }
            } else {
                val fileName = file.name.lowercase()
                val isTemp = tempExtensions.any { ext -> fileName.endsWith(".$ext") }

                if (isTemp) {
                    tempItems.add(
                        JunkItem(
                            id = "temp_${file.absolutePath.hashCode()}",
                            path = file.absolutePath,
                            name = file.name,
                            sizeBytes = file.length(),
                            category = JunkCategory.SYSTEM_TEMP,
                            riskLevel = RiskLevel.SAFE,
                            isDirectory = false,
                            fileCount = 1,
                            isSelected = true
                        )
                    )
                }
            }
        }
    }

    private fun isSystemRootDirectory(dir: File): Boolean {
        val name = dir.name
        return name.equals("Android", ignoreCase = true) ||
                name.equals("DCIM", ignoreCase = true) ||
                name.equals("Pictures", ignoreCase = true) ||
                name.equals("Download", ignoreCase = true) ||
                name.equals("Documents", ignoreCase = true) ||
                name.equals("Music", ignoreCase = true) ||
                name.equals("Movies", ignoreCase = true)
    }

    private fun calculateDirectoryStats(dir: File): Pair<Long, Int> {
        var totalSize = 0L
        var fileCount = 0
        try {
            val files = dir.walkTopDown().maxDepth(10)
            for (file in files) {
                if (file.isFile) {
                    totalSize += file.length()
                    fileCount++
                }
            }
        } catch (_: Exception) {}
        return totalSize to fileCount
    }
}
