package com.storagemaid.app.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.storagemaid.app.core.model.LargeFileThreshold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CleaningHistoryItem(
    val id: String,
    val timestamp: Long,
    val freedBytes: Long,
    val itemsCount: Int,
    val summary: String
)

enum class DuplicateKeepStrategy(val label: String, val description: String) {
    KEEP_OLDEST("Keep Oldest (Original)", "Protects the file with the earliest creation timestamp"),
    KEEP_SHORTEST_PATH("Keep Shortest Path", "Protects the file in the cleanest directory hierarchy")
}

class StoragePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _defaultThreshold = MutableStateFlow(
        LargeFileThreshold.valueOf(prefs.getString(KEY_DEFAULT_THRESHOLD, LargeFileThreshold.MB_25.name) ?: LargeFileThreshold.MB_25.name)
    )
    val defaultThreshold: StateFlow<LargeFileThreshold> = _defaultThreshold.asStateFlow()

    private val _duplicateStrategy = MutableStateFlow(
        DuplicateKeepStrategy.valueOf(prefs.getString(KEY_DUPLICATE_STRATEGY, DuplicateKeepStrategy.KEEP_OLDEST.name) ?: DuplicateKeepStrategy.KEEP_OLDEST.name)
    )
    val duplicateStrategy: StateFlow<DuplicateKeepStrategy> = _duplicateStrategy.asStateFlow()

    private val _whitelist = MutableStateFlow(prefs.getStringSet(KEY_WHITELIST, defaultWhitelist()) ?: defaultWhitelist())
    val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    private val _totalReclaimedBytes = MutableStateFlow(prefs.getLong(KEY_TOTAL_RECLAIMED, 0L))
    val totalReclaimedBytes: StateFlow<Long> = _totalReclaimedBytes.asStateFlow()

    private val _sentinelEnabled = MutableStateFlow(prefs.getBoolean(KEY_SENTINEL_ENABLED, true))
    val sentinelEnabled: StateFlow<Boolean> = _sentinelEnabled.asStateFlow()

    private val _cleaningHistory = MutableStateFlow(loadHistory())
    val cleaningHistory: StateFlow<List<CleaningHistoryItem>> = _cleaningHistory.asStateFlow()

    fun recordClean(freedBytes: Long, itemsCount: Int, summary: String = "") {
        if (freedBytes <= 0 && itemsCount <= 0) return
        val newTotal = _totalReclaimedBytes.value + freedBytes
        _totalReclaimedBytes.value = newTotal
        prefs.edit().putLong(KEY_TOTAL_RECLAIMED, newTotal).apply()

        val newItem = CleaningHistoryItem(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            freedBytes = freedBytes,
            itemsCount = itemsCount,
            summary = summary.ifBlank { "Cleaned storage junk" }
        )
        val updated = (listOf(newItem) + _cleaningHistory.value).take(50)
        _cleaningHistory.value = updated
        saveHistory(updated)
    }

    private fun loadHistory(): List<CleaningHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 5) {
                try {
                    CleaningHistoryItem(
                        id = parts[0],
                        timestamp = parts[1].toLong(),
                        freedBytes = parts[2].toLong(),
                        itemsCount = parts[3].toInt(),
                        summary = parts[4]
                    )
                } catch (_: Exception) { null }
            } else null
        }
    }

    private fun saveHistory(list: List<CleaningHistoryItem>) {
        val raw = list.joinToString("\n") { "${it.id}|${it.timestamp}|${it.freedBytes}|${it.itemsCount}|${it.summary}" }
        prefs.edit().putString(KEY_HISTORY, raw).apply()
    }

    fun clearHistory() {
        _cleaningHistory.value = emptyList()
        _totalReclaimedBytes.value = 0L
        prefs.edit().remove(KEY_HISTORY).putLong(KEY_TOTAL_RECLAIMED, 0L).apply()
    }

    fun setSentinelEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SENTINEL_ENABLED, enabled).apply()
        _sentinelEnabled.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setDefaultThreshold(threshold: LargeFileThreshold) {
        prefs.edit().putString(KEY_DEFAULT_THRESHOLD, threshold.name).apply()
        _defaultThreshold.value = threshold
    }

    fun setDuplicateStrategy(strategy: DuplicateKeepStrategy) {
        prefs.edit().putString(KEY_DUPLICATE_STRATEGY, strategy.name).apply()
        _duplicateStrategy.value = strategy
    }

    fun addWhitelistPath(rawPath: String) {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) return
        val current = _whitelist.value.toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
        _whitelist.value = current
    }

    fun removeWhitelistPath(path: String) {
        val current = _whitelist.value.toMutableSet()
        current.remove(path)
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
        _whitelist.value = current
    }

    fun isPathWhitelisted(path: String): Boolean {
        val currentList = _whitelist.value
        val normalized = path.replace("\\", "/").lowercase()
        for (white in currentList) {
            val whiteNorm = white.replace("\\", "/").lowercase()
            if (normalized.contains(whiteNorm) || normalized.startsWith(whiteNorm)) {
                return true
            }
        }
        return false
    }

    private fun defaultWhitelist(): Set<String> {
        return setOf(
            "/DCIM/Camera",
            "/Documents/KeepSafe"
        )
    }

    companion object {
        private const val PREFS_NAME = "storage_maid_settings"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_DEFAULT_THRESHOLD = "key_default_threshold"
        private const val KEY_DUPLICATE_STRATEGY = "key_duplicate_strategy"
        private const val KEY_WHITELIST = "key_whitelist_paths"
        private const val KEY_TOTAL_RECLAIMED = "key_total_reclaimed"
        private const val KEY_HISTORY = "key_cleaning_history"
        private const val KEY_SENTINEL_ENABLED = "key_sentinel_enabled"
    }
}
