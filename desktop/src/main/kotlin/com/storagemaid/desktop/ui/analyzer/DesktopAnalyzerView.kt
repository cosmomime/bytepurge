package com.storagemaid.desktop.ui.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    fun scanLargeFiles() {
        val target = selectedDrive ?: return
        isLoadingLargeFiles = true
        deleteFeedback = null
        coroutineScope.launch {
            val res = withContext(Dispatchers.IO) {
                val scanTarget = if (target.path.startsWith("C", ignoreCase = true)) {
                    val userHome = System.getProperty("user.home")?.let { File(it) }
                    userHome ?: target
                } else target
                analyzer.scanLargeFiles(scanTarget, minBytes = selectedThreshold.minBytes)
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

    LaunchedEffect(selectedThreshold, selectedDrive) {
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
                val isCurrent = selectedDrive?.absolutePath == drive.rootPath
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedDrive = File(drive.rootPath)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else DarkSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(drive.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${(drive.usedPercentage * 100).toInt()}% used",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (drive.usedPercentage > 0.85f) WarningAmber else SafeGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { drive.usedPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (drive.usedPercentage > 0.85f) WarningAmber else MaterialTheme.colorScheme.primary,
                            trackColor = DarkSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "${drive.formattedFree} free of ${drive.formattedTotal}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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

        // Filter Chips & Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Threshold Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopFileThreshold.values().forEach { threshold ->
                    FilterChip(
                        selected = selectedThreshold == threshold,
                        onClick = { selectedThreshold = threshold },
                        label = { Text(threshold.label, fontSize = 12.sp) }
                    )
                }
            }

            // Quick Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter files...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.width(220.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoadingLargeFiles) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Scanning storage volume for large files...", fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLargeFiles) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Text(
                                        item.file.parent ?: "",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.formattedSize,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
                                        modifier = Modifier.size(18.dp)
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
                                        modifier = Modifier.size(18.dp)
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
