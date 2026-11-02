package com.storagemaid.app.core.cleaner

import android.content.Context
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.notification.ScanNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class CleanResult(
    val itemsDeleted: Int,
    val bytesReclaimed: Long,
    val failedItems: Int
)

class StorageCleaner(
    private val context: Context,
    private val notificationManager: ScanNotificationManager
) {

    suspend fun cleanItems(
        items: List<JunkItem>,
        onProgress: (current: Int, total: Int, item: JunkItem) -> Unit
    ): CleanResult = withContext(Dispatchers.IO) {
        val selectedItems = items.filter { it.isSelected }
        val total = selectedItems.size
        var deletedCount = 0
        var reclaimedBytes = 0L
        var failedCount = 0

        for ((index, item) in selectedItems.withIndex()) {
            val currentNumber = index + 1
            onProgress(currentNumber, total, item)
            notificationManager.showCleanProgress(currentNumber, total, item.name)

            val file = File(item.path)
            if (file.exists()) {
                val success = try {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                } catch (_: Exception) {
                    false
                }

                if (success) {
                    deletedCount++
                    reclaimedBytes += item.sizeBytes
                } else {
                    failedCount++
                }
            } else {
                // Already deleted or moved
                deletedCount++
                reclaimedBytes += item.sizeBytes
            }
        }

        notificationManager.showCleanComplete(deletedCount, reclaimedBytes)
        CleanResult(deletedCount, reclaimedBytes, failedCount)
    }
}
