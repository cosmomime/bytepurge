package com.storagemaid.app.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.storagemaid.app.MainActivity
import com.storagemaid.app.R
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.notification.ScanNotificationManager
import com.storagemaid.app.core.preferences.StoragePreferences
import com.storagemaid.app.core.scanner.StorageScanner
import java.util.concurrent.TimeUnit

class StorageSentinelWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = StoragePreferences(context)
        if (!prefs.sentinelEnabled.value) {
            return Result.success()
        }

        try {
            val statPath = Environment.getExternalStorageDirectory().path
            val stat = StatFs(statPath)
            val blockSize = stat.blockSizeLong
            val totalBytes = stat.blockCountLong * blockSize
            val freeBytes = stat.availableBlocksLong * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val usedPercentage = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes * 100).toInt() else 0

            val notificationManager = ScanNotificationManager(context)
            val scanner = StorageScanner(context, notificationManager, prefs)

            var accumulatedJunkBytes = 0L
            var accumulatedJunkCount = 0
            scanner.startScan().collect { (_, map) ->
                val allJunk = map.values.flatten()
                accumulatedJunkBytes = allJunk.sumOf { it.sizeBytes }
                accumulatedJunkCount = allJunk.size
            }

            val shouldAlert = usedPercentage >= 85 || accumulatedJunkBytes >= 500L * 1024 * 1024

            if (shouldAlert) {
                postSentinelNotification(
                    context = context,
                    usedPercentage = usedPercentage,
                    junkBytes = accumulatedJunkBytes,
                    junkCount = accumulatedJunkCount
                )
            }
        } catch (_: Exception) {}

        return Result.success()
    }

    private fun postSentinelNotification(
        context: Context,
        usedPercentage: Int,
        junkBytes: Long,
        junkCount: Int
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "storage_sentinel_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Storage Sentinel Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders when storage is full or junk accumulates"
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val junkFormatted = JunkItem.formatSize(junkBytes)
        val contentText = if (junkBytes > 0) {
            "$usedPercentage% used • Reclaim up to $junkFormatted of safe-to-clean junk."
        } else {
            "Storage is $usedPercentage% full. Tap to inspect large files."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_clean_tile)
            .setContentTitle("BytePurge Sentinel")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingLaunch)
            .setAutoCancel(true)
            .build()

        nm.notify(9901, notification)
    }

    companion object {
        private const val WORK_NAME = "bytepurge_sentinel_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StorageSentinelWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
