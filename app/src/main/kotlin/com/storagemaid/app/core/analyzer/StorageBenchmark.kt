package com.storagemaid.app.core.analyzer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Random

data class StorageBenchmarkResult(
    val readSpeedMBps: Double,
    val writeSpeedMBps: Double,
    val grade: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedReadSpeed: String
        get() = String.format("%.1f MB/s", readSpeedMBps)

    val formattedWriteSpeed: String
        get() = String.format("%.1f MB/s", writeSpeedMBps)
}

class StorageBenchmark(private val context: Context) {

    suspend fun runBenchmark(
        onProgress: (String) -> Unit = {}
    ): StorageBenchmarkResult = withContext(Dispatchers.IO) {
        val testFile = File(context.cacheDir, "benchmark_test.tmp")
        val bufferSize = 64 * 1024 // 64 KB chunk
        val totalBytes = 16 * 1024 * 1024L // 16 MB test payload
        val chunks = (totalBytes / bufferSize).toInt()

        val randomBytes = ByteArray(bufferSize)
        Random().nextBytes(randomBytes)

        var writeSpeedMBps = 0.0
        var readSpeedMBps = 0.0

        try {
            // 1. Sequential Write Test
            onProgress("Testing Sequential Write speed…")
            val writeStart = System.nanoTime()
            FileOutputStream(testFile).use { fos ->
                for (i in 0 until chunks) {
                    fos.write(randomBytes)
                }
                fos.flush()
                fos.fd.sync()
            }
            val writeElapsedNanos = System.nanoTime() - writeStart
            val writeElapsedSeconds = writeElapsedNanos / 1_000_000_000.0
            writeSpeedMBps = (totalBytes / (1024.0 * 1024.0)) / writeElapsedSeconds.coerceAtLeast(0.001)

            // 2. Sequential Read Test
            onProgress("Testing Sequential Read speed…")
            val readBuffer = ByteArray(bufferSize)
            val readStart = System.nanoTime()
            var bytesReadTotal = 0L
            FileInputStream(testFile).use { fis ->
                while (bytesReadTotal < totalBytes) {
                    val read = fis.read(readBuffer)
                    if (read == -1) break
                    bytesReadTotal += read
                }
            }
            val readElapsedNanos = System.nanoTime() - readStart
            val readElapsedSeconds = readElapsedNanos / 1_000_000_000.0
            readSpeedMBps = (totalBytes / (1024.0 * 1024.0)) / readElapsedSeconds.coerceAtLeast(0.001)

        } finally {
            // Clean up temporary file immediately
            if (testFile.exists()) {
                testFile.delete()
            }
        }

        val grade = when {
            writeSpeedMBps >= 350.0 -> "UFS 3.1+ (Ultra Fast)"
            writeSpeedMBps >= 150.0 -> "UFS 2.2 (High Speed)"
            writeSpeedMBps >= 60.0 -> "eMMC 5.1 (Standard)"
            else -> "Standard Flash"
        }

        StorageBenchmarkResult(
            readSpeedMBps = readSpeedMBps,
            writeSpeedMBps = writeSpeedMBps,
            grade = grade
        )
    }
}
