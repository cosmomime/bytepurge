package com.storagemaid.app.ui.dashboard

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.storagemaid.app.core.analyzer.StorageAnalyzer
import com.storagemaid.app.core.analyzer.StorageBenchmark
import com.storagemaid.app.core.analyzer.StorageBenchmarkResult
import com.storagemaid.app.core.cleaner.CleanResult
import com.storagemaid.app.core.cleaner.StorageCleaner
import com.storagemaid.app.core.manager.AppManager
import com.storagemaid.app.core.model.AppFilter
import com.storagemaid.app.core.model.AppInfoItem
import com.storagemaid.app.core.model.AppManagerSummary
import com.storagemaid.app.core.model.AppSortOrder
import com.storagemaid.app.core.model.AppTab
import com.storagemaid.app.core.model.DeviceHardwareStats
import com.storagemaid.app.core.model.DuplicateFileItem
import com.storagemaid.app.core.model.DuplicateGroup
import com.storagemaid.app.core.model.DuplicateMode
import com.storagemaid.app.core.model.FolderExplorerState
import com.storagemaid.app.core.model.FolderNodeItem
import com.storagemaid.app.core.model.JunkCategory
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.LargeFileItem
import com.storagemaid.app.core.model.LargeFileSort
import com.storagemaid.app.core.model.LargeFileThreshold
import com.storagemaid.app.core.model.LargeFileType
import com.storagemaid.app.core.model.ScanProgress
import com.storagemaid.app.core.model.SimilarPhotoGroup
import com.storagemaid.app.core.model.SimilarPhotoItem
import com.storagemaid.app.core.model.StorageBreakdown
import com.storagemaid.app.core.model.StorageOverview
import com.storagemaid.app.core.notification.ScanNotificationManager
import com.storagemaid.app.core.preferences.DuplicateKeepStrategy
import com.storagemaid.app.core.preferences.StoragePreferences
import com.storagemaid.app.core.scanner.DuplicateScanner
import com.storagemaid.app.core.scanner.SimilarPhotoScanner
import com.storagemaid.app.core.scanner.StorageScanner
import com.storagemaid.app.core.service.StorageSentinelWorker
import com.storagemaid.app.core.sound.RetroSoundManager
import com.storagemaid.app.core.widget.StorageWidgetProvider
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = StoragePreferences(application)
    private val notificationManager = ScanNotificationManager(application)
    private val scanner = StorageScanner(application, notificationManager, preferences)
    private val cleaner = StorageCleaner(application, notificationManager)
    private val analyzer = StorageAnalyzer(application, preferences)
    private val duplicateScanner = DuplicateScanner(application, preferences)
    private val similarPhotoScanner = SimilarPhotoScanner(application, preferences)
    private val appManager = AppManager(application)

    // Sound & Preferences State
    val isSoundEnabled: StateFlow<Boolean> = preferences.soundEnabled
    val isSentinelEnabled: StateFlow<Boolean> = preferences.sentinelEnabled
    val defaultLargeFileThreshold: StateFlow<LargeFileThreshold> = preferences.defaultThreshold
    val duplicateStrategy: StateFlow<DuplicateKeepStrategy> = preferences.duplicateStrategy
    val whitelistPaths: StateFlow<Set<String>> = preferences.whitelist

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.SWEEP)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Dashboard Storage Overview
    private val _storageOverview = MutableStateFlow(StorageOverview())
    val storageOverview: StateFlow<StorageOverview> = _storageOverview.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _junkByCategory = MutableStateFlow<Map<JunkCategory, List<JunkItem>>>(emptyMap())
    val junkByCategory: StateFlow<Map<JunkCategory, List<JunkItem>>> = _junkByCategory.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _cleanProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val cleanProgress: StateFlow<Pair<Int, Int>?> = _cleanProgress.asStateFlow()

    private val _lastCleanResult = MutableStateFlow<CleanResult?>(null)
    val lastCleanResult: StateFlow<CleanResult?> = _lastCleanResult.asStateFlow()

    // Storage Analyzer State
    private val _storageBreakdown = MutableStateFlow<StorageBreakdown?>(null)
    val storageBreakdown: StateFlow<StorageBreakdown?> = _storageBreakdown.asStateFlow()

    private val _hardwareStats = MutableStateFlow(analyzer.getDeviceHardwareStats())
    val hardwareStats: StateFlow<DeviceHardwareStats> = _hardwareStats.asStateFlow()

    // Storage Speed Benchmark
    private val benchmark = StorageBenchmark(application)
    private val _benchmarkResult = MutableStateFlow<StorageBenchmarkResult?>(null)
    val benchmarkResult: StateFlow<StorageBenchmarkResult?> = _benchmarkResult.asStateFlow()

    private val _isRunningBenchmark = MutableStateFlow(false)
    val isRunningBenchmark: StateFlow<Boolean> = _isRunningBenchmark.asStateFlow()

    private val _benchmarkStatus = MutableStateFlow("")
    val benchmarkStatus: StateFlow<String> = _benchmarkStatus.asStateFlow()

    private val _allLargeFiles = MutableStateFlow<List<LargeFileItem>>(emptyList())

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analyzerStatus = MutableStateFlow("")
    val analyzerStatus: StateFlow<String> = _analyzerStatus.asStateFlow()

    private val _selectedThreshold = MutableStateFlow(LargeFileThreshold.MB_25)
    val selectedThreshold: StateFlow<LargeFileThreshold> = _selectedThreshold.asStateFlow()

    private val _selectedFilterType = MutableStateFlow<LargeFileType?>(null)
    val selectedFilterType: StateFlow<LargeFileType?> = _selectedFilterType.asStateFlow()

    private val _onlyStaleDownloads = MutableStateFlow(false)
    val onlyStaleDownloads: StateFlow<Boolean> = _onlyStaleDownloads.asStateFlow()

    private val _sortOrder = MutableStateFlow(LargeFileSort.SIZE_DESC)
    val sortOrder: StateFlow<LargeFileSort> = _sortOrder.asStateFlow()

    private val _lastDeleteResult = MutableStateFlow<Pair<Int, Long>?>(null)
    val lastDeleteResult: StateFlow<Pair<Int, Long>?> = _lastDeleteResult.asStateFlow()

    // Filtered & sorted view of large files
    private val _filteredLargeFiles = MutableStateFlow<List<LargeFileItem>>(emptyList())
    val filteredLargeFiles: StateFlow<List<LargeFileItem>> = _filteredLargeFiles.asStateFlow()

    // Duplicate Finder State
    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _isScanningDuplicates = MutableStateFlow(false)
    val isScanningDuplicates: StateFlow<Boolean> = _isScanningDuplicates.asStateFlow()

    private val _duplicateScanStatus = MutableStateFlow("")
    val duplicateScanStatus: StateFlow<String> = _duplicateScanStatus.asStateFlow()

    private val _duplicateDeleteResult = MutableStateFlow<Pair<Int, Long>?>(null)
    val duplicateDeleteResult: StateFlow<Pair<Int, Long>?> = _duplicateDeleteResult.asStateFlow()

    // Similar Photos State
    private val _duplicateMode = MutableStateFlow(DuplicateMode.EXACT)
    val duplicateMode: StateFlow<DuplicateMode> = _duplicateMode.asStateFlow()

    private val _similarPhotoGroups = MutableStateFlow<List<SimilarPhotoGroup>>(emptyList())
    val similarPhotoGroups: StateFlow<List<SimilarPhotoGroup>> = _similarPhotoGroups.asStateFlow()

    private val _isScanningSimilar = MutableStateFlow(false)
    val isScanningSimilar: StateFlow<Boolean> = _isScanningSimilar.asStateFlow()

    private val _similarScanStatus = MutableStateFlow("")
    val similarScanStatus: StateFlow<String> = _similarScanStatus.asStateFlow()

    private val _similarDeleteResult = MutableStateFlow<Pair<Int, Long>?>(null)
    val similarDeleteResult: StateFlow<Pair<Int, Long>?> = _similarDeleteResult.asStateFlow()

    // App Manager State
    private val _allApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    private val _filteredApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    val filteredApps: StateFlow<List<AppInfoItem>> = _filteredApps.asStateFlow()

    private val _appSummary = MutableStateFlow(AppManagerSummary())
    val appSummary: StateFlow<AppManagerSummary> = _appSummary.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _appLoadStatus = MutableStateFlow("")
    val appLoadStatus: StateFlow<String> = _appLoadStatus.asStateFlow()

    private val _selectedAppFilter = MutableStateFlow(AppFilter.USER_ONLY)
    val selectedAppFilter: StateFlow<AppFilter> = _selectedAppFilter.asStateFlow()

    private val _selectedAppSort = MutableStateFlow(AppSortOrder.SIZE_DESC)
    val selectedAppSort: StateFlow<AppSortOrder> = _selectedAppSort.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    // Folder Explorer / Heatmap State
    private val _folderExplorerState = MutableStateFlow(FolderExplorerState())
    val folderExplorerState: StateFlow<FolderExplorerState> = _folderExplorerState.asStateFlow()

    private val _isFolderExplorerOpen = MutableStateFlow(false)
    val isFolderExplorerOpen: StateFlow<Boolean> = _isFolderExplorerOpen.asStateFlow()

    init {
        RetroSoundManager.isSoundEnabled = preferences.soundEnabled.value
        _selectedThreshold.value = preferences.defaultThreshold.value
        refreshStorageOverview()
    }

    fun toggleSound() {
        val newState = !isSoundEnabled.value
        preferences.setSoundEnabled(newState)
        RetroSoundManager.isSoundEnabled = newState
        if (newState) {
            RetroSoundManager.playMascotTap()
        }
    }

    fun setSentinelEnabled(enabled: Boolean) {
        preferences.setSentinelEnabled(enabled)
        if (enabled) {
            StorageSentinelWorker.schedule(getApplication())
        } else {
            StorageSentinelWorker.cancel(getApplication())
        }
    }

    fun setDefaultThresholdPreference(threshold: LargeFileThreshold) {
        preferences.setDefaultThreshold(threshold)
        setThreshold(threshold)
    }

    fun setDuplicateStrategy(strategy: DuplicateKeepStrategy) {
        preferences.setDuplicateStrategy(strategy)
        if (_duplicateGroups.value.isNotEmpty()) {
            scanDuplicates()
        }
    }

    fun addWhitelistPath(path: String) {
        preferences.addWhitelistPath(path)
    }

    fun removeWhitelistPath(path: String) {
        preferences.removeWhitelistPath(path)
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.ANALYZER && _storageBreakdown.value == null && !_isAnalyzing.value) {
            runStorageAnalysis()
        }
        if (tab == AppTab.DUPLICATES && _duplicateGroups.value.isEmpty() && !_isScanningDuplicates.value) {
            scanDuplicates()
        }
        if (tab == AppTab.APPS && _allApps.value.isEmpty() && !_isLoadingApps.value) {
            loadInstalledApps()
        }
    }

    fun refreshStorageOverview() {
        _storageOverview.value = scanner.getStorageOverview()
        _hardwareStats.value = analyzer.getDeviceHardwareStats()
    }

    fun startScan() {
        if (_scanProgress.value.isScanning || _isCleaning.value) return
        RetroSoundManager.playScanStart()

        viewModelScope.launch {
            _lastCleanResult.value = null
            scanner.startScan().collect { (progress, junkMap) ->
                _scanProgress.value = progress
                _junkByCategory.value = junkMap.mapValues { it.value.toList() }

                val currentReclaimable = junkMap.values.flatten()
                    .filter { it.isSelected }
                    .sumOf { it.sizeBytes }

                _storageOverview.update {
                    it.copy(reclaimableBytes = currentReclaimable)
                }
            }
        }
    }

    fun toggleItemSelection(category: JunkCategory, item: JunkItem) {
        val currentMap = _junkByCategory.value.toMutableMap()
        val list = currentMap[category]?.toMutableList() ?: return

        val index = list.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            val updated = list[index].copy(isSelected = !list[index].isSelected)
            list[index] = updated
            currentMap[category] = list
            _junkByCategory.value = currentMap

            updateReclaimableTotal()
        }
    }

    fun toggleCategorySelection(category: JunkCategory, selectAll: Boolean) {
        val currentMap = _junkByCategory.value.toMutableMap()
        val list = currentMap[category]?.map { it.copy(isSelected = selectAll) } ?: return
        currentMap[category] = list
        _junkByCategory.value = currentMap

        updateReclaimableTotal()
    }

    private fun updateReclaimableTotal() {
        val totalReclaimable = _junkByCategory.value.values.flatten()
            .filter { it.isSelected }
            .sumOf { it.sizeBytes }

        _storageOverview.update {
            it.copy(reclaimableBytes = totalReclaimable)
        }
    }

    fun cleanSelected() {
        val itemsToClean = _junkByCategory.value.values.flatten().filter { it.isSelected }
        if (itemsToClean.isEmpty() || _isCleaning.value) return

        viewModelScope.launch {
            _isCleaning.value = true
            _cleanProgress.value = 0 to itemsToClean.size

            val result = cleaner.cleanItems(itemsToClean) { current, total, _ ->
                _cleanProgress.value = current to total
            }

            _isCleaning.value = false
            _cleanProgress.value = null
            _lastCleanResult.value = result
            RetroSoundManager.playCleanSuccess()

            preferences.recordClean(
                freedBytes = result.bytesReclaimed,
                itemsCount = result.itemsDeleted,
                summary = "Byte Sweep Cleanup"
            )

            // Remove deleted items from UI state
            val updatedMap = _junkByCategory.value.mapValues { (_, list) ->
                list.filter { !it.isSelected }
            }
            _junkByCategory.value = updatedMap

            refreshStorageOverview()
            StorageWidgetProvider.updateAllWidgets(getApplication())
            updateReclaimableTotal()
            StorageWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun dismissCleanResult() {
        _lastCleanResult.value = null
    }

    // ==========================================
    // Storage Analyzer & Large Files
    // ==========================================

    fun runStorageAnalysis() {
        if (_isAnalyzing.value) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analyzerStatus.value = "Analyzing storage distribution…"

            val roots = scanner.getStorageRoots()

            // 1. Calculate category breakdown
            val breakdown = analyzer.analyzeStorage(roots)
            _storageBreakdown.value = breakdown

            // 2. Scan large files using current threshold
            _analyzerStatus.value = "Hunting for large files…"
            val files = analyzer.scanLargeFiles(
                roots = roots,
                minBytes = _selectedThreshold.value.minBytes
            ) { count, currentName ->
                _analyzerStatus.value = "Found $count big files ($currentName)…"
            }

            _allLargeFiles.value = files
            applyFiltersAndSort()

            _isAnalyzing.value = false
            _analyzerStatus.value = ""
        }
    }

    fun setThreshold(threshold: LargeFileThreshold) {
        if (_selectedThreshold.value == threshold) return
        _selectedThreshold.value = threshold
        runStorageAnalysis()
    }

    fun setFilterType(type: LargeFileType?) {
        _selectedFilterType.value = type
        applyFiltersAndSort()
    }

    fun toggleOnlyStaleDownloads() {
        _onlyStaleDownloads.value = !_onlyStaleDownloads.value
        applyFiltersAndSort()
    }

    fun setSortOrder(sort: LargeFileSort) {
        _sortOrder.value = sort
        applyFiltersAndSort()
    }

    fun toggleLargeFileSelection(id: String) {
        val updated = _allLargeFiles.value.map { item ->
            if (item.id == id) item.copy(isSelected = !item.isSelected) else item
        }
        _allLargeFiles.value = updated
        applyFiltersAndSort()
    }

    fun selectAllLargeFiles(selectAll: Boolean) {
        val filteredIds = _filteredLargeFiles.value.map { it.id }.toSet()
        val updated = _allLargeFiles.value.map { item ->
            if (filteredIds.contains(item.id)) item.copy(isSelected = selectAll) else item
        }
        _allLargeFiles.value = updated
        applyFiltersAndSort()
    }

    fun deleteSelectedLargeFiles() {
        val selected = _allLargeFiles.value.filter { it.isSelected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analyzerStatus.value = "Deleting ${selected.size} selected file(s)…"

            val (deletedCount, freedBytes) = analyzer.deleteFiles(selected)
            _lastDeleteResult.value = deletedCount to freedBytes
            RetroSoundManager.playDeleteTrash()

            preferences.recordClean(
                freedBytes = freedBytes,
                itemsCount = deletedCount,
                summary = "Big files removed"
            )

            // Remove deleted items
            val remaining = _allLargeFiles.value.filter { !it.isSelected }
            _allLargeFiles.value = remaining
            applyFiltersAndSort()

            // Refresh breakdown and storage stats
            refreshStorageOverview()
            StorageWidgetProvider.updateAllWidgets(getApplication())
            val roots = scanner.getStorageRoots()
            _storageBreakdown.value = analyzer.analyzeStorage(roots)
            StorageWidgetProvider.updateAllWidgets(getApplication())

            _isAnalyzing.value = false
            _analyzerStatus.value = ""
        }
    }

    fun dismissDeleteResult() {
        _lastDeleteResult.value = null
    }

    fun runStorageBenchmark() {
        if (_isRunningBenchmark.value) return
        viewModelScope.launch {
            _isRunningBenchmark.value = true
            _benchmarkStatus.value = "Preparing speed test payload…"

            val result = benchmark.runBenchmark { status ->
                _benchmarkStatus.value = status
            }

            _benchmarkResult.value = result
            _isRunningBenchmark.value = false
            _benchmarkStatus.value = ""
            RetroSoundManager.playAudioChime()
        }
    }

    private fun applyFiltersAndSort() {
        var list = _allLargeFiles.value

        // Filter by stale downloads
        if (_onlyStaleDownloads.value) {
            list = list.filter { it.isStaleDownload }
        }

        // Filter by type
        val typeFilter = _selectedFilterType.value
        if (typeFilter != null) {
            list = list.filter { it.type == typeFilter }
        }

        // Sort
        list = when (_sortOrder.value) {
            LargeFileSort.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
            LargeFileSort.SIZE_ASC -> list.sortedBy { it.sizeBytes }
            LargeFileSort.DATE_DESC -> list.sortedByDescending { it.lastModified }
            LargeFileSort.DATE_ASC -> list.sortedBy { it.lastModified }
        }

        _filteredLargeFiles.value = list
    }

    // ==========================================
    // Exact Duplicate File Finder
    // ==========================================

    fun scanDuplicates() {
        if (_isScanningDuplicates.value) return

        viewModelScope.launch {
            _isScanningDuplicates.value = true
            _duplicateScanStatus.value = "Starting duplicate scan…"

            val roots = scanner.getStorageRoots()
            val groups = duplicateScanner.scanDuplicates(roots) { status ->
                _duplicateScanStatus.value = status
            }

            _duplicateGroups.value = groups
            _isScanningDuplicates.value = false
            _duplicateScanStatus.value = ""
        }
    }

    fun autoSelectDuplicates() {
        val updated = _duplicateGroups.value.map { group ->
            val updatedFiles = group.files.mapIndexed { index, file ->
                file.copy(isSelected = (index > 0)) // Keep index 0 (original) safe, select all duplicates!
            }
            group.copy(files = updatedFiles)
        }
        _duplicateGroups.value = updated
    }

    fun deselectAllDuplicates() {
        val updated = _duplicateGroups.value.map { group ->
            val updatedFiles = group.files.map { it.copy(isSelected = false) }
            group.copy(files = updatedFiles)
        }
        _duplicateGroups.value = updated
    }

    fun toggleDuplicateSelection(groupHash: String, fileId: String) {
        val updated = _duplicateGroups.value.map { group ->
            if (group.hash == groupHash) {
                val updatedFiles = group.files.map { file ->
                    if (file.id == fileId && !file.isOriginal) {
                        file.copy(isSelected = !file.isSelected)
                    } else {
                        file
                    }
                }
                group.copy(files = updatedFiles)
            } else {
                group
            }
        }
        _duplicateGroups.value = updated
    }

    fun deleteSelectedDuplicates() {
        val allSelected = _duplicateGroups.value.flatMap { it.files }.filter { it.isSelected && !it.isOriginal }
        if (allSelected.isEmpty()) return

        viewModelScope.launch {
            _isScanningDuplicates.value = true
            _duplicateScanStatus.value = "Deleting ${allSelected.size} duplicate file(s)…"

            val (count, freed) = duplicateScanner.deleteSelectedDuplicates(allSelected)
            _duplicateDeleteResult.value = count to freed
            RetroSoundManager.playDeleteTrash()

            preferences.recordClean(
                freedBytes = freed,
                itemsCount = count,
                summary = "Duplicate files deleted"
            )

            // Refresh groups by removing deleted files
            val updated = _duplicateGroups.value.mapNotNull { group ->
                val remainingFiles = group.files.filter { !it.isSelected }
                if (remainingFiles.size >= 2) {
                    group.copy(files = remainingFiles)
                } else {
                    null // No longer a duplicate group if < 2 files remain!
                }
            }
            _duplicateGroups.value = updated

            refreshStorageOverview()
            StorageWidgetProvider.updateAllWidgets(getApplication())
            _isScanningDuplicates.value = false
            _duplicateScanStatus.value = ""
        }
    }

    fun dismissDuplicateDeleteResult() {
        _duplicateDeleteResult.value = null
    }

    fun setDuplicateMode(mode: DuplicateMode) {
        _duplicateMode.value = mode
        if (mode == DuplicateMode.SIMILAR_PHOTOS && _similarPhotoGroups.value.isEmpty() && !_isScanningSimilar.value) {
            scanSimilarPhotos()
        }
    }

    fun scanSimilarPhotos() {
        if (_isScanningSimilar.value) return
        viewModelScope.launch {
            _isScanningSimilar.value = true
            _similarScanStatus.value = "Analyzing photo similarities…"
            RetroSoundManager.playScanStart()
            val roots = scanner.getStorageRoots()
            val groups = similarPhotoScanner.scanSimilarPhotos(roots) { count, name ->
                _similarScanStatus.value = "Analyzing ($count): $name…"
            }
            _similarPhotoGroups.value = groups
            _isScanningSimilar.value = false
            _similarScanStatus.value = ""
            RetroSoundManager.playCleanSuccess()
        }
    }

    fun toggleSimilarPhotoSelection(groupId: String, photoId: String) {
        val updated = _similarPhotoGroups.value.map { group ->
            if (group.groupId == groupId) {
                val updatedPhotos = group.photos.map { photo ->
                    if (photo.id == photoId && !photo.isBestShot) {
                        photo.copy(isSelected = !photo.isSelected)
                    } else {
                        photo
                    }
                }
                group.copy(photos = updatedPhotos)
            } else group
        }
        _similarPhotoGroups.value = updated
    }

    fun setBestShot(groupId: String, photoId: String) {
        val updated = _similarPhotoGroups.value.map { group ->
            if (group.groupId == groupId) {
                val updatedPhotos = group.photos.map { photo ->
                    val isBest = photo.id == photoId
                    photo.copy(isBestShot = isBest, isSelected = !isBest)
                }
                group.copy(photos = updatedPhotos)
            } else group
        }
        _similarPhotoGroups.value = updated
    }

    fun autoSelectSimilarPhotos() {
        val updated = _similarPhotoGroups.value.map { group ->
            val updatedPhotos = group.photos.map { photo ->
                photo.copy(isSelected = !photo.isBestShot)
            }
            group.copy(photos = updatedPhotos)
        }
        _similarPhotoGroups.value = updated
    }

    fun deselectAllSimilarPhotos() {
        val updated = _similarPhotoGroups.value.map { group ->
            val updatedPhotos = group.photos.map { it.copy(isSelected = false) }
            group.copy(photos = updatedPhotos)
        }
        _similarPhotoGroups.value = updated
    }

    fun deleteSelectedSimilarPhotos() {
        val allSelected = _similarPhotoGroups.value.flatMap { it.photos }.filter { it.isSelected && !it.isBestShot }
        if (allSelected.isEmpty()) return

        viewModelScope.launch {
            _isScanningSimilar.value = true
            _similarScanStatus.value = "Deleting ${allSelected.size} similar photo(s)…"

            val (count, freed) = similarPhotoScanner.deleteSimilarPhotos(allSelected)
            _similarDeleteResult.value = count to freed
            RetroSoundManager.playDeleteTrash()

            preferences.recordClean(
                freedBytes = freed,
                itemsCount = count,
                summary = "Similar photos cleaned"
            )

            val updated = _similarPhotoGroups.value.mapNotNull { group ->
                val remaining = group.photos.filter { !it.isSelected }
                if (remaining.size >= 2) {
                    group.copy(photos = remaining)
                } else null
            }
            _similarPhotoGroups.value = updated

            refreshStorageOverview()
            StorageWidgetProvider.updateAllWidgets(getApplication())
            _isScanningSimilar.value = false
            _similarScanStatus.value = ""
        }
    }

    fun dismissSimilarPhotoDeleteResult() {
        _similarDeleteResult.value = null
    }

    // ==========================================
    // App Manager
    // ==========================================

    fun loadInstalledApps() {
        if (_isLoadingApps.value) return

        viewModelScope.launch {
            _isLoadingApps.value = true
            _appLoadStatus.value = "Inspecting installed applications…"

            val apps = appManager.getInstalledApps { count, name ->
                _appLoadStatus.value = "Inspecting ($count): $name…"
            }

            _allApps.value = apps

            val userCount = apps.count { !it.isSystemApp }
            val systemCount = apps.count { it.isSystemApp }
            val userBytes = apps.filter { !it.isSystemApp }.sumOf { it.apkSizeBytes }

            _appSummary.value = AppManagerSummary(
                totalApps = apps.size,
                userApps = userCount,
                systemApps = systemCount,
                totalUserSizeBytes = userBytes
            )

            applyAppFiltersAndSort()

            _isLoadingApps.value = false
            _appLoadStatus.value = ""
        }
    }

    fun setAppFilter(filter: AppFilter) {
        _selectedAppFilter.value = filter
        applyAppFiltersAndSort()
    }

    fun setAppSort(sort: AppSortOrder) {
        _selectedAppSort.value = sort
        applyAppFiltersAndSort()
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
        applyAppFiltersAndSort()
    }

    private fun applyAppFiltersAndSort() {
        var list = _allApps.value

        // Filter by category
        list = when (_selectedAppFilter.value) {
            AppFilter.USER_ONLY -> list.filter { !it.isSystemApp }
            AppFilter.SYSTEM_ONLY -> list.filter { it.isSystemApp }
            AppFilter.LARGE_ONLY -> list.filter { it.apkSizeBytes >= 100 * 1024 * 1024 }
            AppFilter.LARGE_CACHE -> list.filter { it.isLargeCache }
            AppFilter.ALL -> list
        }

        // Search query
        val query = _appSearchQuery.value.trim().lowercase()
        if (query.isNotEmpty()) {
            list = list.filter {
                it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }

        // Sort order
        list = when (_selectedAppSort.value) {
            AppSortOrder.SIZE_DESC -> list.sortedByDescending { it.totalStorageBytes }
            AppSortOrder.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
            AppSortOrder.DATE_DESC -> list.sortedByDescending { it.installTimeMillis }
            AppSortOrder.DATE_ASC -> list.sortedBy { it.installTimeMillis }
        }

        _filteredApps.value = list
    }

    fun extractApk(app: AppInfoItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = appManager.extractApk(app)
            if (res.first) {
                RetroSoundManager.playCleanSuccess()
            }
            onResult(res.first, res.second)
        }
    }

    fun shareApk(app: AppInfoItem) {
        appManager.shareApk(app)
    }

    fun getAppIcon(packageName: String): android.graphics.drawable.Drawable? {
        return appManager.getAppIcon(packageName)
    }

    fun launchApp(packageName: String): Boolean {
        return appManager.launchApp(packageName)
    }

    fun openAppDetails(packageName: String): Boolean {
        return appManager.openAppDetails(packageName)
    }

    fun requestUninstall(packageName: String): Boolean {
        return appManager.requestUninstall(packageName)
    }

    fun onPackageRemoved(packageName: String) {
        val current = _allApps.value
        val updated = current.filter { it.packageName != packageName }
        if (updated.size != current.size) {
            _allApps.value = updated
            val userCount = updated.count { !it.isSystemApp }
            val systemCount = updated.count { it.isSystemApp }
            val userBytes = updated.filter { !it.isSystemApp }.sumOf { it.apkSizeBytes }
            _appSummary.value = AppManagerSummary(
                totalApps = updated.size,
                userApps = userCount,
                systemApps = systemCount,
                totalUserSizeBytes = userBytes
            )
            applyAppFiltersAndSort()
            refreshStorageOverview()
        }
    }

    fun pruneUninstalledApps(packageManager: PackageManager) {
        val current = _allApps.value
        if (current.isEmpty()) return
        val updated = current.filter { app ->
            try {
                packageManager.getPackageInfo(app.packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            } catch (_: Throwable) {
                true
            }
        }
        if (updated.size != current.size) {
            _allApps.value = updated
            val userCount = updated.count { !it.isSystemApp }
            val systemCount = updated.count { it.isSystemApp }
            val userBytes = updated.filter { !it.isSystemApp }.sumOf { it.apkSizeBytes }
            _appSummary.value = AppManagerSummary(
                totalApps = updated.size,
                userApps = userCount,
                systemApps = systemCount,
                totalUserSizeBytes = userBytes
            )
            applyAppFiltersAndSort()
            refreshStorageOverview()
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        return appManager.hasUsageStatsPermission()
    }

    fun openUsageAccessSettings() {
        appManager.openUsageAccessSettings()
    }

    fun openFolderExplorer(initialDir: File? = null) {
        val root = initialDir ?: android.os.Environment.getExternalStorageDirectory()
        _isFolderExplorerOpen.value = true
        loadFolder(root)
    }

    fun closeFolderExplorer() {
        _isFolderExplorerOpen.value = false
    }

    fun loadFolder(dir: File) {
        viewModelScope.launch {
            _folderExplorerState.value = _folderExplorerState.value.copy(
                isLoading = true,
                currentPath = dir.canonicalPath
            )
            val result = analyzer.exploreDirectory(dir)
            _folderExplorerState.value = result
        }
    }

    fun navigateUpFolder() {
        val current = File(_folderExplorerState.value.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadFolder(parent)
        }
    }

    fun deleteFolderNode(item: FolderNodeItem, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = analyzer.deleteDirectoryOrFile(item.file)
            if (success) {
                RetroSoundManager.playDeleteTrash()
                val current = File(_folderExplorerState.value.currentPath)
                val result = analyzer.exploreDirectory(current)
                _folderExplorerState.value = result
                refreshStorageOverview()
            StorageWidgetProvider.updateAllWidgets(getApplication())
            }
            onFinished(success)
        }
    }
}
