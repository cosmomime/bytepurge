package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.DesktopJunkCategory
import com.storagemaid.desktop.model.DesktopJunkItem
import java.io.File
import java.util.UUID

class WindowsJunkScanner {

    fun scanWindowsJunk(): Map<DesktopJunkCategory, List<DesktopJunkItem>> {
        val resultMap = mutableMapOf<DesktopJunkCategory, MutableList<DesktopJunkItem>>()
        DesktopJunkCategory.values().forEach { resultMap[it] = mutableListOf() }

        // 1. Windows & User Temp
        val userTemp = System.getenv("TEMP")?.let { File(it) }
        if (userTemp != null && userTemp.exists()) {
            scanDirectory(userTemp, DesktopJunkCategory.WINDOWS_TEMP, resultMap, maxDepth = 2)
        }
        val winTemp = File("C:\\Windows\\Temp")
        if (winTemp.exists() && winTemp.canRead()) {
            scanDirectory(winTemp, DesktopJunkCategory.WINDOWS_TEMP, resultMap, maxDepth = 2)
        }

        // 2. Windows Update Download Cache
        val updateDownload = File("C:\\Windows\\SoftwareDistribution\\Download")
        if (updateDownload.exists() && updateDownload.canRead()) {
            scanDirectory(updateDownload, DesktopJunkCategory.WINDOWS_UPDATE, resultMap, maxDepth = 2)
        }

        // 3. Crash Dumps
        val localAppData = System.getenv("LOCALAPPDATA")
        if (localAppData != null) {
            val crashDumps = File(localAppData, "CrashDumps")
            if (crashDumps.exists()) {
                scanDirectory(crashDumps, DesktopJunkCategory.CRASH_DUMPS, resultMap, maxDepth = 1)
            }
        }
        val minidump = File("C:\\Windows\\Minidump")
        if (minidump.exists() && minidump.canRead()) {
            scanDirectory(minidump, DesktopJunkCategory.CRASH_DUMPS, resultMap, maxDepth = 1)
        }

        // 4. Browser Caches
        val appData = System.getenv("APPDATA")
        if (localAppData != null) {
            val chromeCache = File(localAppData, "Google\\Chrome\\User Data\\Default\\Cache\\Cache_Data")
            if (chromeCache.exists()) {
                scanDirectory(chromeCache, DesktopJunkCategory.BROWSER_CACHE, resultMap, maxDepth = 1)
            }
            val edgeCache = File(localAppData, "Microsoft\\Edge\\User Data\\Default\\Cache\\Cache_Data")
            if (edgeCache.exists()) {
                scanDirectory(edgeCache, DesktopJunkCategory.BROWSER_CACHE, resultMap, maxDepth = 1)
            }
            val braveCache = File(localAppData, "BraveSoftware\\Brave-Browser\\User Data\\Default\\Cache\\Cache_Data")
            if (braveCache.exists()) {
                scanDirectory(braveCache, DesktopJunkCategory.BROWSER_CACHE, resultMap, maxDepth = 1)
            }
            val vivaldiCache = File(localAppData, "Vivaldi\\User Data\\Default\\Cache\\Cache_Data")
            if (vivaldiCache.exists()) {
                scanDirectory(vivaldiCache, DesktopJunkCategory.BROWSER_CACHE, resultMap, maxDepth = 1)
            }
        }
        if (appData != null) {
            val operaCache = File(appData, "Opera Software\\Opera Stable\\Cache\\Cache_Data")
            if (operaCache.exists()) {
                scanDirectory(operaCache, DesktopJunkCategory.BROWSER_CACHE, resultMap, maxDepth = 1)
            }
            val operaGxCache = File(appData, "Opera Software\\Opera GX Stable\\Cache\\Cache_Data")
            if (operaGxCache.exists()) {
                scanDirectory(operaGxCache, DesktopJunkCategory.BROWSER_CACHE, resultMap, maxDepth = 1)
            }
        }

        // 5. Desktop App Caches (Discord, Spotify, VS Code)
        if (appData != null) {
            val discordCache = File(appData, "discord\\Cache\\Cache_Data")
            if (discordCache.exists()) {
                scanDirectory(discordCache, DesktopJunkCategory.APP_CACHE, resultMap, maxDepth = 1)
            }
            val vscodeCache = File(appData, "Code\\Cache\\Cache_Data")
            if (vscodeCache.exists()) {
                scanDirectory(vscodeCache, DesktopJunkCategory.APP_CACHE, resultMap, maxDepth = 1)
            }
        }
        if (localAppData != null) {
            val spotifyStorage = File(localAppData, "Spotify\\Storage")
            if (spotifyStorage.exists()) {
                scanDirectory(spotifyStorage, DesktopJunkCategory.APP_CACHE, resultMap, maxDepth = 1)
            }
            val spotifyData = File(localAppData, "Spotify\\Data")
            if (spotifyData.exists()) {
                scanDirectory(spotifyData, DesktopJunkCategory.APP_CACHE, resultMap, maxDepth = 1)
            }
        }

        return resultMap
    }

    private fun scanDirectory(
        dir: File,
        category: DesktopJunkCategory,
        result: MutableMap<DesktopJunkCategory, MutableList<DesktopJunkItem>>,
        currentDepth: Int = 0,
        maxDepth: Int = 2
    ) {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead() || currentDepth > maxDepth) return
        val currentCount = result[category]?.size ?: 0
        if (currentCount >= 2000) return
        val files = try { dir.listFiles() } catch (_: Throwable) { null } ?: return

        for (f in files) {
            try {
                if (f.isFile) {
                    val len = f.length()
                    if (len > 0) {
                        result[category]?.add(
                            DesktopJunkItem(
                                id = UUID.randomUUID().toString(),
                                file = f,
                                name = f.name,
                                sizeBytes = len,
                                category = category,
                                isSelected = true
                            )
                        )
                    }
                } else if (f.isDirectory) {
                    scanDirectory(f, category, result, currentDepth + 1, maxDepth)
                }
            } catch (_: Throwable) {}
        }
    }

    fun cleanSelectedItems(items: List<DesktopJunkItem>): Pair<Int, Long> {
        var count = 0
        var freed = 0L
        for (item in items) {
            try {
                val size = item.sizeBytes
                if (item.file.delete()) {
                    count++
                    freed += size
                }
            } catch (_: Exception) {}
        }
        return count to freed
    }
}
