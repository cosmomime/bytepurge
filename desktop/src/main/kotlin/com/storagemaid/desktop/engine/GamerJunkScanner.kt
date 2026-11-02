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
            val userHome = System.getProperty("user.home")?.let { File(it) }

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

            val nvidiaNv = File(localAppData, "NVIDIA Corporation/NV_Cache")
            if (nvidiaNv.exists()) {
                scanFilesRecursively(nvidiaNv, GamerCacheCategory.SHADER_CACHE, "NVIDIA Driver Compiled Cache", results)
            }

            if (userHome != null) {
                val nvidiaLocalLow = File(userHome, "AppData/LocalLow/NVIDIA/PerDriverVersion/DXCache")
                if (nvidiaLocalLow.exists()) {
                    scanFilesRecursively(nvidiaLocalLow, GamerCacheCategory.SHADER_CACHE, "NVIDIA Per-Driver Shader Cache", results)
                }
            }

            val amdDx = File(localAppData, "AMD/DxCache")
            if (amdDx.exists()) {
                scanFilesRecursively(amdDx, GamerCacheCategory.SHADER_CACHE, "AMD Radeon DirectX Shader Cache", results)
            }

            val amdGl = File(localAppData, "AMD/GLCache")
            if (amdGl.exists()) {
                scanFilesRecursively(amdGl, GamerCacheCategory.SHADER_CACHE, "AMD Radeon OpenGL Cache", results)
            }

            val amdVk = File(localAppData, "AMD/VkCache")
            if (amdVk.exists()) {
                scanFilesRecursively(amdVk, GamerCacheCategory.SHADER_CACHE, "AMD Radeon Vulkan Cache", results)
            }

            val intelShader = File(localAppData, "Intel/ShaderCache")
            if (intelShader.exists()) {
                scanFilesRecursively(intelShader, GamerCacheCategory.SHADER_CACHE, "Intel Graphics Shader Cache", results)
            }

            val unityCache = File(localAppData, "Unity/cache")
            if (unityCache.exists()) {
                scanFilesRecursively(unityCache, GamerCacheCategory.SHADER_CACHE, "Unity Engine Shader Cache", results)
            }
        }

        // 3. Game Launcher Web & CDN Caches
        val programFilesX86 = System.getenv("ProgramFiles(x86)")?.let { File(it) } ?: File("C:\\Program Files (x86)")
        val programData = System.getenv("ProgramData")?.let { File(it) } ?: File("C:\\ProgramData")

        // Steam Shader Pre-Cache & HTML Caches
        val steamShaderCache = File(programFilesX86, "Steam/steamapps/shadercache")
        if (steamShaderCache.exists()) {
            scanFilesRecursively(steamShaderCache, GamerCacheCategory.SHADER_CACHE, "Steam Library Shader Pre-Cache", results)
        }

        if (localAppData != null) {
            val steamHtml = File(localAppData, "Steam/htmlcache")
            if (steamHtml.exists()) {
                scanFilesRecursively(steamHtml, GamerCacheCategory.LAUNCHER_CACHE, "Steam Client Web Cache", results)
            }

            val epicCache = File(localAppData, "EpicGamesLauncher/Saved/webcache")
            if (epicCache.exists()) {
                scanFilesRecursively(epicCache, GamerCacheCategory.LAUNCHER_CACHE, "Epic Games Launcher Cache", results)
            }

            val epicVault = File(programData, "Epic/EpicGamesLauncher/VaultCache")
            if (epicVault.exists()) {
                scanFilesRecursively(epicVault, GamerCacheCategory.LAUNCHER_CACHE, "Epic Games Vault Download Cache", results)
            }

            val eaCache = File(localAppData, "Electronic Arts/EA Desktop/Cache")
            if (eaCache.exists()) {
                scanFilesRecursively(eaCache, GamerCacheCategory.LAUNCHER_CACHE, "EA Desktop App Cache", results)
            }

            val eaIgo = File(localAppData, "Electronic Arts/EA Desktop/IGOCache")
            if (eaIgo.exists()) {
                scanFilesRecursively(eaIgo, GamerCacheCategory.LAUNCHER_CACHE, "EA In-Game Overlay Cache", results)
            }

            val originCache = File(localAppData, "Origin/cache")
            if (originCache.exists()) {
                scanFilesRecursively(originCache, GamerCacheCategory.LAUNCHER_CACHE, "Origin Legacy Cache", results)
            }

            val ubisoftCache = File(localAppData, "Ubisoft Game Launcher/cache")
            if (ubisoftCache.exists()) {
                scanFilesRecursively(ubisoftCache, GamerCacheCategory.LAUNCHER_CACHE, "Ubisoft Connect Cache", results)
            }

            val rockstarCache = File(localAppData, "Rockstar Games/Launcher/cache")
            if (rockstarCache.exists()) {
                scanFilesRecursively(rockstarCache, GamerCacheCategory.LAUNCHER_CACHE, "Rockstar Games Launcher Cache", results)
            }

            val xboxCache = File(localAppData, "Packages/Microsoft.GamingApp_8wekyb3d8bbwe/LocalCache")
            if (xboxCache.exists() && xboxCache.canRead()) {
                scanFilesRecursively(xboxCache, GamerCacheCategory.LAUNCHER_CACHE, "Xbox App Package Cache", results)
            }

            val riotLogs = File(localAppData, "Riot Games/Riot Client/Logs")
            if (riotLogs.exists()) {
                scanFilesRecursively(riotLogs, GamerCacheCategory.LAUNCHER_CACHE, "Riot Client Diagnostic Logs", results)
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
        if (!dir.exists() || !dir.canRead() || depth > maxDepth) return
        val currentCategoryCount = results[category]?.size ?: 0
        if (currentCategoryCount >= 1500) return
        val list = try { dir.listFiles() } catch (_: Throwable) { null } ?: return

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
            } catch (_: Throwable) {}
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
