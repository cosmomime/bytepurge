package com.storagemaid.app.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.storagemaid.app.MainActivity
import com.storagemaid.app.R
import com.storagemaid.app.StorageMaidApplication
import com.storagemaid.app.core.model.JunkItem
import java.util.concurrent.atomic.AtomicLong

class ScanNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val lastUpdateTime = AtomicLong(0)

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showScanProgress(scannedCount: Int, junkCount: Int, reclaimableBytes: Long, currentFile: String) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime.get() < 120 && scannedCount % 50 != 0) return
        lastUpdateTime.set(now)

        val notification = NotificationCompat.Builder(context, StorageMaidApplication.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("Scanning storage ($scannedCount items checked)")
            .setContentText("Found: $junkCount items (${JunkItem.formatSize(reclaimableBytes)})")
            .setSubText(currentFile.takeLast(35))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createContentIntent())
            .build()

        notificationManager.notify(StorageMaidApplication.NOTIFICATION_ID_PROGRESS, notification)
    }

    fun showCleanProgress(current: Int, total: Int, currentItem: String) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime.get() < 100 && current != total) return
        lastUpdateTime.set(now)

        val notification = NotificationCompat.Builder(context, StorageMaidApplication.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("Cleaning storage ($current/$total)")
            .setContentText(currentItem.takeLast(40))
            .setProgress(total, current, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createContentIntent())
            .build()

        notificationManager.notify(StorageMaidApplication.NOTIFICATION_ID_PROGRESS, notification)
    }

    fun showCleanComplete(cleanedCount: Int, reclaimedBytes: Long) {
        notificationManager.cancel(StorageMaidApplication.NOTIFICATION_ID_PROGRESS)

        val notification = NotificationCompat.Builder(context, StorageMaidApplication.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Storage Clean Complete")
            .setContentText("Successfully freed ${JunkItem.formatSize(reclaimedBytes)} ($cleanedCount items removed)")
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(createContentIntent())
            .build()

        notificationManager.notify(StorageMaidApplication.NOTIFICATION_ID_COMPLETE, notification)
    }

    fun dismissProgress() {
        notificationManager.cancel(StorageMaidApplication.NOTIFICATION_ID_PROGRESS)
    }
}
