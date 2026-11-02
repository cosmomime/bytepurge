package com.storagemaid.app.core.scanner

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.storagemaid.app.core.model.JunkCategory
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.ScanProgress
import com.storagemaid.app.core.model.StorageOverview
import com.storagemaid.app.core.notification.ScanNotificationManager
import com.storagemaid.app.core.preferences.StoragePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class StorageScanner(
    private val context: Context,
    private val notificationManager: ScanNotificationManager,
    private val preferences: StoragePreferences = StoragePreferences(context)
) {

    private val corpseFinder = CorpseFinder(context)
    private val systemScrubber = SystemScrubber()
    private val mediaPurger = MediaPurger()

    fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()

        // 1. Primary External Storage (/sdcard / /storage/emulated/0)
        val primary = Environment.getExternalStorageDirectory()
        if (primary != null && primary.exists()) {
            roots.add(primary)
        }

        // 2. Secondary External Storage (MicroSD Card / USB)
        try {
            val externalDirs = context.getExternalFilesDirs(null)
            for (dir in externalDirs) {
                if (dir != null) {
                    val path = dir.absolutePath
                    val androidIndex = path.indexOf("/Android")
                    if (androidIndex > 0) {
                        val rootPath = path.substring(0, androidIndex)
                        val rootFile = File(rootPath)
                        if (rootFile.exists() && !roots.contains(rootFile)) {
                            roots.add(rootFile)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return roots
    }

    fun getStorageOverview(): StorageOverview {
        return try {
            val path = Environment.getExternalStorageDirectory().path
            val stat = StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = total - free

            StorageOverview(
                totalBytes = total,
                freeBytes = free,
                usedBytes = used,
                reclaimableBytes = 0L
            )
        } catch (_: Exception) {
            StorageOverview()
        }
    }

    fun startScan(): Flow<Pair<ScanProgress, Map<JunkCategory, List<JunkItem>>>> = flow {
        val storageRoots = getStorageRoots()
        val allJunk = mutableMapOf<JunkCategory, MutableList<JunkItem>>(
            JunkCategory.CORPSES to mutableListOf(),
            JunkCategory.SYSTEM_TEMP to mutableListOf(),
            JunkCategory.EMPTY_FOLDERS to mutableListOf(),
            JunkCategory.SOCIAL_JUNK to mutableListOf()
        )

        var scannedCount = 0
        var totalJunkFound = 0
        var totalReclaimableBytes = 0L

        fun notifyProgress(file: File) {
            scannedCount++
            notificationManager.showScanProgress(
                scannedCount = scannedCount,
                junkCount = totalJunkFound,
                reclaimableBytes = totalReclaimableBytes,
                currentFile = file.name
            )
        }

        // Emit initial progress
        emit(
            ScanProgress(
                currentPath = "Preparing storage scanner…",
                itemsScanned = 0,
                junkItemsFound = 0,
                totalReclaimableBytes = 0L,
                isScanning = true,
                isFinished = false
            ) to allJunk
        )

        // 1. Scan CorpseFinder
        val installedPackages = corpseFinder.getInstalledPackageNames()
        val corpses = corpseFinder.scan(storageRoots, installedPackages) { file ->
            notifyProgress(file)
        }.filter { !preferences.isPathWhitelisted(it.path) }
        allJunk[JunkCategory.CORPSES]?.addAll(corpses)
        totalJunkFound += corpses.size
        totalReclaimableBytes += corpses.sumOf { it.sizeBytes }

        emit(
            ScanProgress(
                currentPath = "Scanning uninstalled app leftovers…",
                itemsScanned = scannedCount,
                junkItemsFound = totalJunkFound,
                totalReclaimableBytes = totalReclaimableBytes,
                isScanning = true,
                isFinished = false
            ) to allJunk
        )

        // 2. Scan SystemScrubber (Temp files & Empty Folders)
        val (tempFilesRaw, emptyDirsRaw) = systemScrubber.scan(storageRoots) { file ->
            notifyProgress(file)
        }
        val tempFiles = tempFilesRaw.filter { !preferences.isPathWhitelisted(it.path) }
        val emptyDirs = emptyDirsRaw.filter { !preferences.isPathWhitelisted(it.path) }

        allJunk[JunkCategory.SYSTEM_TEMP]?.addAll(tempFiles)
        totalJunkFound += tempFiles.size
        totalReclaimableBytes += tempFiles.sumOf { it.sizeBytes }

        allJunk[JunkCategory.EMPTY_FOLDERS]?.addAll(emptyDirs)
        totalJunkFound += emptyDirs.size

        emit(
            ScanProgress(
                currentPath = "Scanning system temp & empty directories…",
                itemsScanned = scannedCount,
                junkItemsFound = totalJunkFound,
                totalReclaimableBytes = totalReclaimableBytes,
                isScanning = true,
                isFinished = false
            ) to allJunk
        )

        // 3. Scan Social Media Duplicates & Statuses
        val socialJunk = mediaPurger.scan(storageRoots) { file ->
            notifyProgress(file)
        }.filter { !preferences.isPathWhitelisted(it.path) }
        allJunk[JunkCategory.SOCIAL_JUNK]?.addAll(socialJunk)
        totalJunkFound += socialJunk.size
        totalReclaimableBytes += socialJunk.filter { it.isSelected }.sumOf { it.sizeBytes }

        notificationManager.dismissProgress()

        // Emit finished state
        emit(
            ScanProgress(
                currentPath = "Scan completed",
                itemsScanned = scannedCount,
                junkItemsFound = totalJunkFound,
                totalReclaimableBytes = totalReclaimableBytes,
                isScanning = false,
                isFinished = true
            ) to allJunk
        )
    }.flowOn(Dispatchers.IO)
}
