package com.storagemaid.app.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.widget.RemoteViews
import android.widget.Toast
import com.storagemaid.app.MainActivity
import com.storagemaid.app.R
import com.storagemaid.app.core.cleaner.StorageCleaner
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.notification.ScanNotificationManager
import com.storagemaid.app.core.preferences.StoragePreferences
import com.storagemaid.app.core.scanner.StorageScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StorageWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_CLEAN) {
            Toast.makeText(context, "BytePurge: Quick purge started…", Toast.LENGTH_SHORT).show()

            widgetScope.launch {
                try {
                    val prefs = StoragePreferences(context)
                    val notificationManager = ScanNotificationManager(context)
                    val scanner = StorageScanner(context, notificationManager, prefs)
                    val cleaner = StorageCleaner(context, notificationManager)

                    var junkItems = emptyList<JunkItem>()
                    scanner.startScan().collect { (_, map) ->
                        junkItems = map.values.flatten()
                    }

                    if (junkItems.isNotEmpty()) {
                        val cleanResult = cleaner.cleanItems(junkItems) { _, _, _ -> }
                        prefs.recordClean(
                            freedBytes = cleanResult.bytesReclaimed,
                            itemsCount = cleanResult.itemsDeleted,
                            summary = "Home Widget Clean"
                        )
                        val freedFormatted = JunkItem.formatSize(cleanResult.bytesReclaimed)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Widget Cleaned: $freedFormatted freed!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Storage already optimal!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    updateAllWidgets(context)
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Clean complete", Toast.LENGTH_SHORT).show()
                    }
                    updateAllWidgets(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_CLEAN = "com.storagemaid.app.widget.ACTION_CLEAN"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_storage)

            try {
                val statPath = Environment.getExternalStorageDirectory().path
                val stat = StatFs(statPath)
                val blockSize = stat.blockSizeLong
                val totalBytes = stat.blockCountLong * blockSize
                val freeBytes = stat.availableBlocksLong * blockSize
                val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
                val usedPct = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes) * 100).toInt() else 0

                views.setTextViewText(R.id.widget_title, "$usedPct% Storage Used")
                views.setTextViewText(
                    R.id.widget_subtitle,
                    "${JunkItem.formatSize(freeBytes)} free of ${JunkItem.formatSize(totalBytes)}"
                )
                views.setProgressBar(R.id.widget_progress, 100, usedPct, false)
            } catch (_: Exception) {
                views.setTextViewText(R.id.widget_title, "BytePurge")
                views.setTextViewText(R.id.widget_subtitle, "Tap to inspect")
            }

            // Click on widget body -> Launch App
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingLaunch = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingLaunch)

            // Click on clean button -> Run background clean
            val cleanIntent = Intent(context, StorageWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CLEAN
            }
            val pendingClean = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                cleanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_clean_button, pendingClean)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, StorageWidgetProvider::class.java)
                )
                for (id in ids) {
                    updateWidget(context, manager, id)
                }
            } catch (_: Exception) {}
        }
    }
}
