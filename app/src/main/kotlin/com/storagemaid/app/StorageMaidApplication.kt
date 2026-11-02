package com.storagemaid.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.storagemaid.app.core.preferences.StoragePreferences
import com.storagemaid.app.core.service.StorageSentinelWorker

class StorageMaidApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initSentinel()
    }

    private fun initSentinel() {
        try {
            val prefs = StoragePreferences(this)
            if (prefs.sentinelEnabled.value) {
                StorageSentinelWorker.schedule(this)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                getString(R.string.channel_scan_progress),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live storage scan and clean progress"
                setShowBadge(false)
                setSound(null, null)
            }

            val sentinelChannel = NotificationChannel(
                CHANNEL_SENTINEL,
                "Storage Sentinel Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders when storage is full or junk accumulates"
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(sentinelChannel)
        }
    }

    companion object {
        const val CHANNEL_PROGRESS = "storage_maid_progress_channel"
        const val CHANNEL_SENTINEL = "storage_sentinel_channel"
        const val NOTIFICATION_ID_PROGRESS = 1001
        const val NOTIFICATION_ID_COMPLETE = 1002
    }
}
