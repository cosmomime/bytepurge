package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.GamerCacheCategory
import com.storagemaid.desktop.model.GamerCacheItem
import java.io.File
import java.util.UUID

class GamerJunkScanner {

    fun scanGamerJunk(): Map<GamerCacheCategory, List<GamerCacheItem>> {
        val results = mutableMapOf<GamerCacheCategory, MutableList<GamerCacheItem>>()
        GamerCacheCategory.values().forEach { results[it] = mutableListOf() }

        val appData = System.getenv("APPDATA")?.let { File(it) }
        val localAppData = System.getenv("LOCALAPPDATA")?.let { File(it) }

        // 1. Replays & Temporary Buffers (GSE Saves, Goldberg, CODEX)
        if (appData != null) {
            // GSE Saves - only scan local/tmp, NEVER touch remote/
            val gseDir = File(appData, "GSE Saves")
            if (gseDir.exists() && gseDir.isDirectory) {
                gseDir.listFiles()?.forEach { appFolder ->
                    if (appFolder.isDirectory) {
                        val tmpFolder = File(appFolder, "local/tmp")
                        if (tmpFolder.exists() && tmpFolder.isDirectory) {
                            scanFilesRecursively(tmpFolder, GamerCacheCategory.REPLAY_TMP, "Game ID #${appFolder.name}", results)
                        }
                    }
                }
            }

            // CODEX Replays and Buffers - only scan local/, NEVER remote/
            val codexDir = File(appData, "Steam/CODEX")
            if (codexDir.exists() && codexDir.isDirectory) {
                codexDir.listFiles()?.forEach { appFolder ->
                    if (appFolder.isDirectory) {
                        val localFolder = File(appFolder, "local")
                        if (localFolder.exists() && localFolder.isDirectory) {
                            localFolder.listFiles()?.forEach { f ->
                                if (f.isFile && (f.name.contains("replay", ignoreCase = true) || f.name.endsWith(".tmp", ignoreCase = true))) {
                                    addFileItem(f, GamerCacheCategory.REPLAY_TMP, "Steam App #${appFolder.name}", results)
                                } else if (f.isFile && (f.name.equals("CDN.BIN", ignoreCase = true) || f.name.equals("SYNC.BIN", ignoreCase = true))) {
                                    addFileItem(f, GamerCacheCategory.EMULATOR_BLOBS, "Steam App #${appFolder.name} (CDN/Sync Blob)", results)
                                }
                            }
                        }
                    }
                }
            }

            // Goldberg SteamEmu Saves - tmp buffers only
            val goldbergDir = File(appData, "Goldberg SteamEmu Saves")
            if (goldbergDir.exists() && goldbergDir.isDirectory) {
                goldbergDir.listFiles()?.forEach { appFolder ->
                    val tmpFolder = File(appFolder, "local/tmp")
                    if (tmpFolder.exists()) {
                        scanFilesRecursively(tmpFolder, GamerCacheCategory.REPLAY_TMP, "Goldberg #${appFolder.name}", results)
                    }
                }
            }
        }

        // 2. DirectX & GPU Shader Caches
        if (localAppData != null) {
            val d3dCache = File(localAppData, "D3DSCache")
            if (d3dCache.exists()) {
                scanFilesRecursively(d3dCache, GamerCacheCategory.SHADER_CACHE, "DirectX 12 Shader Pipeline", results)
            }

            val nvidiaDx = File(localAppData, "NVIDIA/DXCache")
            if (nvidiaDx.exists()) {
                scanFilesRecursively(nvidiaDx, GamerCacheCategory.SHADER_CACHE, "NVIDIA DirectX Shader Cache", results)
            }

            val nvidiaGl = File(localAppData, "NVIDIA/GLCache")
            if (nvidiaGl.exists()) {
                scanFilesRecursively(nvidiaGl, GamerCacheCategory.SHADER_CACHE, "NVIDIA OpenGL Shader Cache", results)
            }

            val amdDx = File(localAppData, "AMD/DxCache")
            if (amdDx.exists()) {
                scanFilesRecursively(amdDx, GamerCacheCategory.SHADER_CACHE, "AMD Radeon Shader Cache", results)
            }

            val intelShader = File(localAppData, "Intel/ShaderCache")
            if (intelShader.exists()) {
                scanFilesRecursively(intelShader, GamerCacheCategory.SHADER_CACHE, "Intel Graphics Shader Cache", results)
            }
        }

        // 3. Game Launcher Web & CDN Caches
        if (localAppData != null) {
            val steamHtml = File(localAppData, "Steam/htmlcache")
            if (steamHtml.exists()) {
                scanFilesRecursively(steamHtml, GamerCacheCategory.LAUNCHER_CACHE, "Steam Client Web Cache", results)
            }

            val epicCache = File(localAppData, "EpicGamesLauncher/Saved/webcache")
            if (epicCache.exists()) {
                scanFilesRecursively(epicCache, GamerCacheCategory.LAUNCHER_CACHE, "Epic Games Launcher Cache", results)
            }

            val eaCache = File(localAppData, "Electronic Arts/EA Desktop/Cache")
            if (eaCache.exists()) {
                scanFilesRecursively(eaCache, GamerCacheCategory.LAUNCHER_CACHE, "EA Desktop App Cache", results)
            }

            val ubisoftCache = File(localAppData, "Ubisoft Game Launcher/cache")
            if (ubisoftCache.exists()) {
                scanFilesRecursively(ubisoftCache, GamerCacheCategory.LAUNCHER_CACHE, "Ubisoft Connect Cache", results)
            }
        }

        if (appData != null) {
            val bnetCache = File(appData, "Battle.net/BrowserCaches")
            if (bnetCache.exists()) {
                scanFilesRecursively(bnetCache, GamerCacheCategory.LAUNCHER_CACHE, "Battle.net Client Web Cache", results)
            }

            val gogCache = File(appData, "GOG.com/Galaxy/webcache")
            if (gogCache.exists()) {
                scanFilesRecursively(gogCache, GamerCacheCategory.LAUNCHER_CACHE, "GOG Galaxy Web Cache", results)
            }
        }

        return results
    }

    private fun scanFilesRecursively(
        dir: File,
        category: GamerCacheCategory,
        title: String,
        results: MutableMap<GamerCacheCategory, MutableList<GamerCacheItem>>,
        depth: Int = 0,
        maxDepth: Int = 4
    ) {
        if (!dir.exists() || depth > maxDepth) return
        val list = dir.listFiles() ?: return

        for (item in list) {
            try {
                if (item.isFile) {
                    val len = item.length()
                    if (len > 0) {
                        addFileItem(item, category, title, results)
                    }
                } else if (item.isDirectory) {
                    scanFilesRecursively(item, category, title, results, depth + 1, maxDepth)
                }
            } catch (_: Exception) {}
        }
    }

    private fun addFileItem(
        file: File,
        category: GamerCacheCategory,
        title: String,
        results: MutableMap<GamerCacheCategory, MutableList<GamerCacheItem>>
    ) {
        results[category]?.add(
            GamerCacheItem(
                id = UUID.randomUUID().toString(),
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                category = category,
                gameTitle = title,
                isSelected = true
            )
        )
    }

    fun purgeSelected(items: List<GamerCacheItem>): Pair<Int, Long> {
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
