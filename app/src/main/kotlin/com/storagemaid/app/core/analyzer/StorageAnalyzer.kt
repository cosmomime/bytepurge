package com.storagemaid.app.core.analyzer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import com.storagemaid.app.core.model.DeviceHardwareStats
import com.storagemaid.app.core.model.FolderExplorerState
import com.storagemaid.app.core.model.FolderNodeItem
import com.storagemaid.app.core.model.LargeFileItem
import com.storagemaid.app.core.model.LargeFileType
import com.storagemaid.app.core.model.StorageBreakdown
import com.storagemaid.app.core.preferences.StoragePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class StorageAnalyzer(
    private val context: Context,
    private val preferences: StoragePreferences = StoragePreferences(context)
) {

    private val excludedDirs = setOf(
        ".git", ".gradle", ".idea", "node_modules", "proc", "sys", "dev"
    )

    suspend fun analyzeStorage(roots: List<File>): StorageBreakdown = withContext(Dispatchers.IO) {
        val statPath = Environment.getExternalStorageDirectory().path
        val stat = StatFs(statPath)
        val blockSize = stat.blockSizeLong
        val total = stat.blockCountLong * blockSize
        val free = stat.availableBlocksLong * blockSize

        var imagesBytes = queryMediaStoreCategorySize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        var videosBytes = queryMediaStoreCategorySize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        var audioBytes = queryMediaStoreCategorySize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        var documentsBytes = 0L
        var archivesBytes = 0L
        var apksBytes = 0L

        // Fast scan across roots for Documents, Archives, and APKs
        val archiveExts = LargeFileType.ARCHIVE.extensions
        val docExts = LargeFileType.DOCUMENT.extensions
        val apkExts = LargeFileType.APK.extensions

        for (root in roots) {
            root.walkTopDown()
                .onEnter { dir ->
                    val name = dir.name
                    !name.startsWith(".") && !excludedDirs.contains(name)
                }
                .filter { it.isFile && !it.name.startsWith(".") }
                .forEach { file ->
                    val ext = file.extension.lowercase()
                    val len = file.length()
                    when {
                        archiveExts.contains(ext) -> archivesBytes += len
                        docExts.contains(ext) -> documentsBytes += len
                        apkExts.contains(ext) -> apksBytes += len
                    }
                }
        }

        val classifiedUsed = imagesBytes + videosBytes + audioBytes + documentsBytes + archivesBytes + apksBytes
        val usedTotal = (total - free).coerceAtLeast(0L)
        val otherBytes = (usedTotal - classifiedUsed).coerceAtLeast(0L)

        StorageBreakdown(
            imagesBytes = imagesBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            documentsBytes = documentsBytes,
            archivesBytes = archivesBytes,
            apksBytes = apksBytes,
            otherBytes = otherBytes,
            freeBytes = free,
            totalBytes = total
        )
    }

    private fun queryMediaStoreCategorySize(contentUri: Uri): Long {
        var total = 0L
        try {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            val cursor: Cursor? = context.contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                while (it.moveToNext()) {
                    if (sizeIndex != -1) {
                        total += it.getLong(sizeIndex).coerceAtLeast(0L)
                    }
                }
            }
        } catch (_: Exception) {}
        return total
    }

    suspend fun scanLargeFiles(
        roots: List<File>,
        minBytes: Long,
        onProgress: ((count: Int, currentPath: String) -> Unit)? = null
    ): List<LargeFileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LargeFileItem>()
        val seenPaths = HashSet<String>()
        var count = 0

        for (root in roots) {
            if (!root.exists()) continue

            root.walkTopDown()
                .onEnter { dir ->
                    val name = dir.name
                    !name.startsWith(".") && !excludedDirs.contains(name) && !preferences.isPathWhitelisted(dir.absolutePath)
                }
                .filter { it.isFile }
                .forEach { file ->
                    count++
                    if (count % 100 == 0) {
                        onProgress?.invoke(results.size, file.name)
                    }

                    try {
                        val length = file.length()
                        if (length >= minBytes && !preferences.isPathWhitelisted(file.absolutePath)) {
                            val canonical = file.canonicalPath
                            if (seenPaths.add(canonical)) {
                                val type = LargeFileType.fromFile(file)
                                results.add(
                                    LargeFileItem(
                                        id = UUID.nameUUIDFromBytes(canonical.toByteArray()).toString(),
                                        file = file,
                                        name = file.name,
                                        path = canonical,
                                        sizeBytes = length,
                                        lastModified = file.lastModified(),
                                        type = type,
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }
        }

        results.sortedByDescending { it.sizeBytes }
    }

    suspend fun deleteFiles(items: List<LargeFileItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var freedBytes = 0L

        for (item in items) {
            try {
                val file = item.file
                val len = item.sizeBytes
                var deleted = false

                if (file.exists()) {
                    deleted = file.delete()
                }

                // Fallback to ContentResolver delete if physical delete fails
                if (!deleted) {
                    try {
                        val uri = MediaStore.Files.getContentUri("external")
                        val rows = context.contentResolver.delete(
                            uri,
                            "${MediaStore.MediaColumns.DATA} = ?",
                            arrayOf(item.path)
                        )
                        deleted = rows > 0
                    } catch (_: Exception) {}
                }

                if (deleted) {
                    deletedCount++
                    freedBytes += len
                }
            } catch (_: Exception) {}
        }

        deletedCount to freedBytes
    }

    fun getDeviceHardwareStats(): DeviceHardwareStats {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"

        // Battery & Thermal Stats
        var batteryPct = -1
        var tempCelsius = 0f
        var isCharging = false
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            tempCelsius = tempRaw / 10f
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {}

        val uptime = SystemClock.elapsedRealtime()

        return DeviceHardwareStats(
            deviceModel = deviceName,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            totalRamBytes = memoryInfo.totalMem,
            availRamBytes = memoryInfo.availMem,
            isLowMemory = memoryInfo.lowMemory,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            batteryPct = batteryPct,
            batteryTempCelsius = tempCelsius,
            isCharging = isCharging,
            uptimeMillis = uptime
        )
    }

    suspend fun exploreDirectory(directory: File): FolderExplorerState = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext FolderExplorerState(
                currentPath = directory.canonicalPath,
                items = emptyList(),
                totalFolderBytes = 0L,
                isLoading = false,
                canGoUp = false
            )
        }

        val rawChildren = directory.listFiles() ?: emptyArray()
        val nodeList = mutableListOf<FolderNodeItem>()
        var accumulatedBytes = 0L

        for (child in rawChildren) {
            val name = child.name
            if (name.startsWith(".") || excludedDirs.contains(name)) continue
            if (preferences.isPathWhitelisted(child.absolutePath)) continue

            try {
                if (child.isDirectory) {
                    var dirBytes = 0L
                    var files = 0
                    var subdirs = 0

                    child.walkTopDown()
                        .onEnter { !it.name.startsWith(".") && !excludedDirs.contains(it.name) && !preferences.isPathWhitelisted(it.absolutePath) }
                        .forEach { f ->
                            if (f.isFile) {
                                dirBytes += f.length()
                                files++
                            } else if (f != child) {
                                subdirs++
                            }
                        }

                    accumulatedBytes += dirBytes
                    nodeList.add(
                        FolderNodeItem(
                            file = child,
                            name = name,
                            path = child.canonicalPath,
                            totalSizeBytes = dirBytes,
                            directFileCount = files,
                            subFolderCount = subdirs,
                            isDirectory = true,
                            lastModified = child.lastModified()
                        )
                    )
                } else if (child.isFile) {
                    val fileLen = child.length()
                    accumulatedBytes += fileLen
                    nodeList.add(
                        FolderNodeItem(
                            file = child,
                            name = name,
                            path = child.canonicalPath,
                            totalSizeBytes = fileLen,
                            directFileCount = 1,
                            subFolderCount = 0,
                            isDirectory = false,
                            lastModified = child.lastModified()
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Compute percentage relative to total
        val enrichedList = nodeList.map { item ->
            val ratio = if (accumulatedBytes > 0) item.totalSizeBytes.toFloat() / accumulatedBytes else 0f
            item.copy(percentageOfParent = ratio.coerceIn(0f, 1f))
        }.sortedByDescending { it.totalSizeBytes }

        val rootStorage = Environment.getExternalStorageDirectory().canonicalPath
        val canGoUp = directory.parentFile != null && directory.canonicalPath != rootStorage

        FolderExplorerState(
            currentPath = directory.canonicalPath,
            items = enrichedList,
            totalFolderBytes = accumulatedBytes,
            isLoading = false,
            canGoUp = canGoUp
        )
    }

    suspend fun deleteDirectoryOrFile(target: File): Boolean = withContext(Dispatchers.IO) {
        if (preferences.isPathWhitelisted(target.absolutePath)) return@withContext false
        try {
            if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
        } catch (_: Exception) {
            false
        }
    }
}
