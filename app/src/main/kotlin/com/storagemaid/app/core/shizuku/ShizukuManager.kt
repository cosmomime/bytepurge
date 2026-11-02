package com.storagemaid.app.core.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

enum class ShizukuStatus {
    NOT_INSTALLED_OR_RUNNING,
    PERMISSION_NEEDED,
    READY
}

class ShizukuManager {

    fun checkStatus(): ShizukuStatus {
        return try {
            if (!Shizuku.pingBinder()) {
                ShizukuStatus.NOT_INSTALLED_OR_RUNNING
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                ShizukuStatus.PERMISSION_NEEDED
            } else {
                ShizukuStatus.READY
            }
        } catch (_: Exception) {
            ShizukuStatus.NOT_INSTALLED_OR_RUNNING
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) {}
    }

    suspend fun trimAllSystemCaches(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            if (checkStatus() != ShizukuStatus.READY) {
                return@withContext false to "Shizuku permission not granted or service offline"
            }

            // Execute pm trim-caches to free maximum system cache rootlessly via Shizuku
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true

            val command = arrayOf("pm", "trim-caches", "999999999999999")
            val process = newProcessMethod.invoke(null, command, null, null) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            process.destroy()

            if (exitCode == 0) {
                true to "All system-wide and third-party app caches trimmed successfully!"
            } else {
                false to "Trim command exited with code $exitCode: ${output.toString().trim()}"
            }
        } catch (e: Exception) {
            false to (e.message ?: "Execution failed")
        }
    }
}
