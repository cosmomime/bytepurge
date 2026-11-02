package com.storagemaid.desktop.ui.analyzer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.engine.DiskSpaceAnalyzer
import com.storagemaid.desktop.model.DesktopJunkItem
import com.storagemaid.desktop.model.DesktopLargeFile
import com.storagemaid.desktop.model.DriveOverview
import com.storagemaid.desktop.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser

private fun getFileTypeInfo(extension: String): Pair<ImageVector, Color> {
    return when (extension.lowercase()) {
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm" ->
            Pair(Icons.Default.Videocam, Color(0xFFA855F7)) // Purple
        "zip", "rar", "7z", "tar", "gz", "iso", "img", "bin" ->
            Pair(Icons.Default.Archive, Color(0xFFF59E0B)) // Amber
        "exe", "msi", "bat", "cmd", "dll", "sys" ->
            Pair(Icons.Default.Code, Color(0xFFEF4444)) // Red
        "vhd", "vhdx", "vmdk", "qcow2" ->
            Pair(Icons.Default.Storage, Color(0xFF06B6D4)) // Cyan
        "mp3", "wav", "flac", "aac", "ogg", "m4a" ->
            Pair(Icons.Default.MusicNote, Color(0xFF10B981)) // Green
        "jpg", "jpeg", "png", "webp", "gif", "psd" ->
            Pair(Icons.Default.Image, Color(0xFF3B82F6)) // Blue
        else ->
            Pair(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF94A3B8)) // Slate
    }
}

enum class DesktopFileThreshold(val minBytes: Long, val label: String) {
    MB_25(25L * 1024 * 1024, "> 25 MB"),
    MB_50(50L * 1024 * 1024, "> 50 MB"),
    MB_100(100L * 1024 * 1024, "> 100 MB"),
    MB_500(500L * 1024 * 1024, "> 500 MB"),
    GB_1(1024L * 1024 * 1024, "> 1 GB")
}

@Composable
fun DesktopAnalyzerView(analyzer: DiskSpaceAnalyzer) {
    val coroutineScope = rememberCoroutineScope()
    var drives by remember { mutableStateOf<List<DriveOverview>>(emptyList()) }
    var largeFiles by remember { mutableStateOf<List<DesktopLargeFile>>(emptyList()) }
    var isLoadingLargeFiles by remember { mutableStateOf(false) }
    var selectedDrive by remember { mutableStateOf<File?>(null) }
    var customTargetFolder by remember { mutableStateOf<File?>(null) }
    var selectedThreshold by remember { mutableStateOf(DesktopFileThreshold.MB_50) }
    var searchQuery by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<DesktopLargeFile?>(null) }
    var deleteFeedback by remember { mutableStateOf<String?>(null) }

    fun refreshDrives() {
        drives = analyzer.getMountedDrives()
        if (selectedDrive == null && drives.isNotEmpty()) {
            selectedDrive = File(drives.first().rootPath)
        }
    }

    val effectiveScanPath = remember(selectedDrive, customTargetFolder) {
        if (customTargetFolder != null) {
            customTargetFolder
        } else if (selectedDrive?.path?.startsWith("C", ignoreCase = true) == true) {
            System.getProperty("user.home")?.let { File(it) } ?: selectedDrive
        } else {
            selectedDrive
        }
    }

    fun scanLargeFiles() {
        val target = effectiveScanPath ?: return
        isLoadingLargeFiles = true
        deleteFeedback = null
        coroutineScope.launch {
            val res = withContext(Dispatchers.IO) {
                analyzer.scanLargeFiles(target, minBytes = selectedThreshold.minBytes)
            }
            largeFiles = res
            isLoadingLargeFiles = false
        }
    }

    fun deleteFile(item: DesktopLargeFile) {
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    item.file.delete()
                } catch (_: Exception) {
                    false
                }
            }
            if (success) {
                deleteFeedback = "Deleted ${item.name} (${item.formattedSize})"
                largeFiles = largeFiles.filter { it.file.absolutePath != item.file.absolutePath }
            } else {
                deleteFeedback = "Could not delete ${item.name} (File may be in use by another program)"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDrives()
        scanLargeFiles()
    }

    LaunchedEffect(selectedThreshold, selectedDrive, customTargetFolder) {
        scanLargeFiles()
    }

    val filteredLargeFiles = remember(largeFiles, searchQuery) {
        if (searchQuery.isBlank()) largeFiles
        else largeFiles.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.extension.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Disk Space Analyzer & Big Files", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Volume breakdown, storage heatbars, and largest disk space consumers",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {
                    refreshDrives()
                    scanLargeFiles()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Mounted Drives Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            drives.forEach { drive ->
                val isCurrent = selectedDrive?.absolutePath == drive.rootPath && customTargetFolder == null
                val usedRatio = drive.usedPercentage
                val statusColor = when {
                    usedRatio >= 0.90f -> DangerRed
                    usedRatio >= 0.75f -> WarningAmber
                    else -> SafeGreen
                }
                val driveIcon = if (drive.rootPath.startsWith("C", ignoreCase = true) || drive.rootPath == "/") {
                    Icons.Default.Computer
                } else {
                    Icons.Default.Storage
                }

                Card(
                    modifier = Modifier
                        .width(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedDrive = File(drive.rootPath)
                            customTargetFolder = null
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else DarkSurfaceVariant
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else DarkSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Icon(
                                    driveIcon,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    drive.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${(usedRatio * 100).toInt()}% used",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { usedRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = DarkSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${drive.formattedFree} free",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Total ${drive.formattedTotal}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Delete Feedback Banner
        if (deleteFeedback != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(deleteFeedback!!, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Controls Row: Size Filter Chips on left, Sleek Search Box on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Threshold selection with icon and label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Min Size:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
                DesktopFileThreshold.values().forEach { threshold ->
                    val isSelected = selectedThreshold == threshold
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else DarkSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else DarkSurfaceVariant
                        ),
                        modifier = Modifier.clickable { selectedThreshold = threshold }
                    ) {
                        Text(
                            threshold.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Sleek Search Input Field
            Surface(
                modifier = Modifier.width(240.dp).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                border = BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else DarkSurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search name or .ext...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scan Location & Summary Bar
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Target: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        effectiveScanPath?.absolutePath ?: "No volume selected",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {
                            val chooser = JFileChooser().apply {
                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                dialogTitle = "Select Folder to Analyze"
                                if (effectiveScanPath?.exists() == true) {
                                    currentDirectory = effectiveScanPath
                                }
                            }
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                customTargetFolder = chooser.selectedFile
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse...", fontSize = 10.sp)
                    }
                    if (customTargetFolder != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { customTargetFolder = null },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Reset", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (!isLoadingLargeFiles) {
                    val totalBytes = filteredLargeFiles.sumOf { it.sizeBytes }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${filteredLargeFiles.size} large files • ${DesktopJunkItem.formatSize(totalBytes)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content Area: Loading, Empty, or List
        if (isLoadingLargeFiles) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Scanning ${effectiveScanPath?.name ?: "volume"} for files over ${selectedThreshold.label}...",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        } else if (filteredLargeFiles.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (searchQuery.isNotEmpty()) WarningAmber else SafeGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isNotEmpty()) "No files match \"$searchQuery\""
                        else "No files larger than ${selectedThreshold.label} found",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (searchQuery.isNotEmpty()) "Try searching for a different keyword or file extension"
                        else "Try selecting a smaller threshold like > 25 MB to inspect more files",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { searchQuery = "" },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Search Filter", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLargeFiles) { item ->
                    val (typeIcon, typeColor) = getFileTypeInfo(item.extension)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = typeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = typeIcon,
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        item.file.parent ?: "",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        item.formattedSize,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Reveal in Explorer Button
                                IconButton(
                                    onClick = {
                                        try {
                                            Desktop.getDesktop().open(item.file.parentFile)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = "Reveal in Explorer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Direct Delete Button
                                IconButton(
                                    onClick = { fileToDelete = item },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete File",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Large File?") },
            text = {
                Column {
                    Text("Are you sure you want to permanently delete this file?", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${target.name} (${target.formattedSize})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        target.file.absolutePath,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteFile(target)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
