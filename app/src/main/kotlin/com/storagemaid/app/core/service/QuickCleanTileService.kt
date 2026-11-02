package com.storagemaid.app.core.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.storagemaid.app.core.cleaner.StorageCleaner
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.notification.ScanNotificationManager
import com.storagemaid.app.core.preferences.StoragePreferences
import com.storagemaid.app.core.scanner.StorageScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QuickCleanTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "Storage Clean"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Tap to scrub"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        tile.state = Tile.STATE_ACTIVE
        tile.label = "Cleaning..."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Scrubbing junk"
        }
        tile.updateTile()

        serviceScope.launch {
            try {
                val prefs = StoragePreferences(applicationContext)
                val notificationManager = ScanNotificationManager(applicationContext)
                val scanner = StorageScanner(applicationContext, notificationManager, prefs)
                val cleaner = StorageCleaner(applicationContext, notificationManager)

                var junkItems = emptyList<com.storagemaid.app.core.model.JunkItem>()
                scanner.startScan().collect { (_, map) ->
                    junkItems = map.values.flatten()
                }

                if (junkItems.isNotEmpty()) {
                    val cleanResult = cleaner.cleanItems(junkItems) { _, _, _ -> }
                    val freed = JunkItem.formatSize(cleanResult.bytesReclaimed)
                    prefs.recordClean(
                        freedBytes = cleanResult.bytesReclaimed,
                        itemsCount = cleanResult.itemsDeleted,
                        summary = "Quick Clean Tile"
                    )
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "Cleaned!"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "$freed freed"
                    }
                    tile.updateTile()
                } else {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "All Clean"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Storage optimal"
                    }
                    tile.updateTile()
                }
            } catch (_: Exception) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Storage Clean"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Error cleaning"
                }
                tile.updateTile()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
