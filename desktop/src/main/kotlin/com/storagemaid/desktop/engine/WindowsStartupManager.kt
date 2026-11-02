package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.DesktopStartupItem
import com.storagemaid.desktop.model.StartupImpact
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

class WindowsStartupManager {

    private val hkcuRun = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private val hkcuDisabled = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run-Disabled"
    private val hklmRun = "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"

    fun listStartupItems(): List<DesktopStartupItem> {
        val items = mutableListOf<DesktopStartupItem>()

        // 1. HKCU Run (Enabled)
        queryRegistry(hkcuRun, isEnabled = true).forEach { items.add(it) }

        // 2. HKCU Run-Disabled (Disabled)
        queryRegistry(hkcuDisabled, isEnabled = false).forEach { items.add(it) }

        // 3. HKLM Run (Enabled system items)
        queryRegistry(hklmRun, isEnabled = true).forEach { items.add(it) }

        // 4. User Startup folder
        val appData = System.getenv("APPDATA")
        if (appData != null) {
            val startupFolder = File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
            if (startupFolder.exists() && startupFolder.isDirectory) {
                startupFolder.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".lnk", ignoreCase = true)) {
                        items.add(
                            DesktopStartupItem(
                                id = UUID.randomUUID().toString(),
                                name = file.nameWithoutExtension,
                                command = file.absolutePath,
                                sourceLocation = "Startup Folder (User)",
                                isEnabled = true,
                                impact = evaluateImpact(file.nameWithoutExtension, file.absolutePath),
                                targetFile = file
                            )
                        )
                    } else if (file.name.endsWith(".lnk.disabled", ignoreCase = true)) {
                        items.add(
                            DesktopStartupItem(
                                id = UUID.randomUUID().toString(),
                                name = file.name.removeSuffix(".disabled").removeSuffix(".lnk"),
                                command = file.absolutePath,
                                sourceLocation = "Startup Folder (User)",
                                isEnabled = false,
                                impact = evaluateImpact(file.name, file.absolutePath),
                                targetFile = file
                            )
                        )
                    }
                }
            }
        }

        return items
    }

    private fun queryRegistry(regPath: String, isEnabled: Boolean): List<DesktopStartupItem> {
        val results = mutableListOf<DesktopStartupItem>()
        try {
            val process = ProcessBuilder("reg", "query", regPath).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.isEmpty() || trimmed.startsWith("HKEY_")) continue

                // Format typically: "<Name>    <REG_SZ|REG_EXPAND_SZ>    <Command>"
                val tokens = trimmed.split(Regex("\\s{4,}"))
                if (tokens.size >= 3) {
                    val name = tokens[0].trim()
                    val command = tokens[2].trim()
                    results.add(
                        DesktopStartupItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            command = command,
                            sourceLocation = regPath,
                            isEnabled = isEnabled,
                            impact = evaluateImpact(name, command)
                        )
                    )
                }
            }
            process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {}
        return results
    }

    fun toggleStartupItem(item: DesktopStartupItem): Boolean {
        return try {
            if (item.targetFile != null) {
                // File-based startup toggle
                if (item.isEnabled) {
                    val target = File(item.targetFile.parentFile, "${item.targetFile.name}.disabled")
                    item.targetFile.renameTo(target)
                } else {
                    val newName = item.targetFile.name.removeSuffix(".disabled")
                    val target = File(item.targetFile.parentFile, newName)
                    item.targetFile.renameTo(target)
                }
            } else if (item.sourceLocation.startsWith("HKCU")) {
                if (item.isEnabled) {
                    // Disable: move from Run to Run-Disabled
                    execReg("add", hkcuDisabled, item.name, item.command)
                    execReg("delete", hkcuRun, item.name, null)
                } else {
                    // Enable: move back to Run
                    execReg("add", hkcuRun, item.name, item.command)
                    execReg("delete", hkcuDisabled, item.name, null)
                }
                true
            } else {
                false // HKLM requires elevated admin privileges
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun execReg(action: String, path: String, valueName: String, data: String?): Boolean {
        val cmd = mutableListOf("reg", action, path, "/v", valueName, "/f")
        if (action == "add" && data != null) {
            cmd.add("/t")
            cmd.add("REG_SZ")
            cmd.add("/d")
            cmd.add(data)
        }
        val p = ProcessBuilder(cmd).start()
        return p.waitFor() == 0
    }

    private fun evaluateImpact(name: String, command: String): StartupImpact {
        val lower = (name + command).lowercase()
        return when {
            lower.contains("steam") || lower.contains("discord") || lower.contains("torrent") ||
            lower.contains("epic") || lower.contains("eadesktop") || lower.contains("teams") ||
            lower.contains("spotify") || lower.contains("onedrive") -> StartupImpact.HIGH
            lower.contains("edge") || lower.contains("chrome") || lower.contains("googledrive") -> StartupImpact.MEDIUM
            else -> StartupImpact.LOW
        }
    }
}
