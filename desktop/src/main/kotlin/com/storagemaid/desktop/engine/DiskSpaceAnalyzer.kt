package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.DesktopFolderHeatmapItem
import com.storagemaid.desktop.model.DesktopLargeFile
import com.storagemaid.desktop.model.DriveOverview
import java.io.File

class DiskSpaceAnalyzer {

    fun getMountedDrives(): List<DriveOverview> {
        val roots = File.listRoots() ?: return emptyList()
        val drives = mutableListOf<DriveOverview>()

        for (root in roots) {
            try {
                val total = root.totalSpace
                val free = root.usableSpace
                val used = (total - free).coerceAtLeast(0L)
                if (total > 0) {
                    val cleanPath = root.path.replace("\\", "").trim()
                    val isSystem = cleanPath.startsWith("C", ignoreCase = true) || cleanPath == "/" || cleanPath.isEmpty()
                    val label = if (isSystem) "System (${if (cleanPath == "/" || cleanPath.isEmpty()) "Root" else "C:"})" else "Drive ($cleanPath)"
                    drives.add(
                        DriveOverview(
                            rootPath = root.absolutePath,
                            totalBytes = total,
                            freeBytes = free,
                            usedBytes = used,
                            label = label
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        return drives
    }

    fun scanLargeFiles(directory: File, minBytes: Long = 50L * 1024 * 1024, maxResults: Int = 100): List<DesktopLargeFile> {
        val results = mutableListOf<DesktopLargeFile>()
        if (!directory.exists() || !directory.canRead()) return emptyList()

        fun walk(dir: File, depth: Int) {
            if (depth > 5 || results.size >= 250) return
            val children = dir.listFiles() ?: return

            for (child in children) {
                try {
                    if (child.isFile) {
                        val len = child.length()
                        if (len >= minBytes) {
                            results.add(
                                DesktopLargeFile(
                                    file = child,
                                    name = child.name,
                                    sizeBytes = len,
                                    extension = child.extension.lowercase(),
                                    lastModified = child.lastModified()
                                )
                            )
                        }
                    } else if (child.isDirectory &&
                        !child.name.startsWith(".") &&
                        child.name != "node_modules" &&
                        child.name != "\$Recycle.Bin" &&
                        !child.name.equals("AppData", ignoreCase = true) &&
                        !child.name.equals("Windows", ignoreCase = true) &&
                        !child.name.equals("ProgramData", ignoreCase = true)
                    ) {
                        walk(child, depth + 1)
                    }
                } catch (_: Exception) {}
            }
        }

        walk(directory, 0)
        return results.sortedByDescending { it.sizeBytes }.take(maxResults)
    }

    fun getFolderHeatmap(directory: File): List<DesktopFolderHeatmapItem> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        val children = directory.listFiles() ?: return emptyList()

        val items = mutableListOf<Pair<File, Long>>()
        var totalParentBytes = 0L

        for (child in children) {
            try {
                val size = if (child.isFile) child.length() else calculateDirectorySize(child, maxDepth = 2)
                if (size > 0) {
                    items.add(child to size)
                    totalParentBytes += size
                }
            } catch (_: Exception) {}
        }

        val sorted = items.sortedByDescending { it.second }
        return sorted.map { (file, size) ->
            val pct = if (totalParentBytes > 0) (size.toFloat() / totalParentBytes) else 0f
            DesktopFolderHeatmapItem(
                file = file,
                name = file.name,
                totalBytes = size,
                parentPercentage = pct
            )
        }
    }

    private fun calculateDirectorySize(dir: File, maxDepth: Int, currentDepth: Int = 0): Long {
        if (currentDepth > maxDepth || !dir.exists() || !dir.canRead()) return 0L
        var total = 0L
        val list = dir.listFiles() ?: return 0L
        for (f in list) {
            try {
                if (f.isFile) {
                    total += f.length()
                } else if (f.isDirectory) {
                    total += calculateDirectorySize(f, maxDepth, currentDepth + 1)
                }
            } catch (_: Exception) {}
        }
        return total
    }
}
