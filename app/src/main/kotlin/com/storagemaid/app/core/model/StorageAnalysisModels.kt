package com.storagemaid.app.core.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    SWEEP("Byte Sweep"),
    ANALYZER("Analyzer"),
    DUPLICATES("Duplicates"),
    APPS("App Manager")
}

enum class LargeFileType(val displayName: String, val extensions: Set<String>) {
    VIDEO(
        "Videos",
        setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "wmv", "m4v")
    ),
    ARCHIVE(
        "Archives",
        setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz")
    ),
    APK(
        "APKs",
        setOf("apk", "xapk", "apks", "apkm")
    ),
    AUDIO(
        "Audio",
        setOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "wma")
    ),
    DOCUMENT(
        "Documents",
        setOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv")
    ),
    OTHER(
        "Other",
        emptySet()
    );

    companion object {
        fun fromExtension(ext: String): LargeFileType {
            val lower = ext.lowercase()
            return values().firstOrNull { it.extensions.contains(lower) } ?: OTHER
        }

        fun fromFile(file: File): LargeFileType {
            val ext = file.extension
            return fromExtension(ext)
        }
    }
}

data class LargeFileItem(
    val id: String,
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val type: LargeFileType,
    var isSelected: Boolean = false
) {
    val formattedSize: String
        get() = JunkItem.formatSize(sizeBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val parentFolder: String
        get() = file.parentFile?.name ?: "Storage"

    val isStaleDownload: Boolean
        get() {
            val isDownloadFolder = path.contains("/Download/", ignoreCase = true) ||
                    path.contains("/Downloads/", ignoreCase = true)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            return isDownloadFolder && lastModified < thirtyDaysAgo
        }
}

data class StorageBreakdown(
    val imagesBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val apksBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val usedBytes: Long
        get() = (totalBytes - freeBytes).coerceAtLeast(0L)

    fun percentageOfUsed(bytes: Long): Float {
        return if (usedBytes > 0) (bytes.toFloat() / usedBytes).coerceIn(0f, 1f) else 0f
    }

    fun percentageOfTotal(bytes: Long): Float {
        return if (totalBytes > 0) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }
}

enum class LargeFileThreshold(val minBytes: Long, val label: String) {
    MB_25(25L * 1024 * 1024, "> 25 MB"),
    MB_50(50L * 1024 * 1024, "> 50 MB"),
    MB_100(100L * 1024 * 1024, "> 100 MB"),
    MB_500(500L * 1024 * 1024, "> 500 MB")
}

enum class LargeFileSort(val label: String) {
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First")
}

data class DeviceHardwareStats(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val totalRamBytes: Long = 0L,
    val availRamBytes: Long = 0L,
    val isLowMemory: Boolean = false,
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val batteryPct: Int = -1,
    val batteryTempCelsius: Float = 0f,
    val isCharging: Boolean = false,
    val uptimeMillis: Long = 0L
) {
    val usedRamBytes: Long
        get() = (totalRamBytes - availRamBytes).coerceAtLeast(0L)

    val ramUsagePercentage: Float
        get() = if (totalRamBytes > 0) (usedRamBytes.toFloat() / totalRamBytes).coerceIn(0f, 1f) else 0f

    val formattedTotalRam: String
        get() = JunkItem.formatSize(totalRamBytes)

    val formattedUsedRam: String
        get() = JunkItem.formatSize(usedRamBytes)

    val formattedAvailRam: String
        get() = JunkItem.formatSize(availRamBytes)

    val formattedUptime: String
        get() {
            val totalSeconds = uptimeMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return "${hours}h ${minutes}m"
        }
}

data class FolderNodeItem(
    val file: File,
    val name: String,
    val path: String,
    val totalSizeBytes: Long,
    val directFileCount: Int,
    val subFolderCount: Int,
    val isDirectory: Boolean,
    val lastModified: Long,
    val percentageOfParent: Float = 0f
) {
    val formattedSize: String
        get() = JunkItem.formatSize(totalSizeBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}

data class FolderExplorerState(
    val currentPath: String = "",
    val items: List<FolderNodeItem> = emptyList(),
    val totalFolderBytes: Long = 0L,
    val isLoading: Boolean = false,
    val canGoUp: Boolean = false
)
