package com.storagemaid.desktop.ui.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.storagemaid.desktop.engine.DesktopDuplicateScanner
import com.storagemaid.desktop.engine.DesktopSimilarPhotoScanner
import com.storagemaid.desktop.model.DesktopDuplicateGroup
import com.storagemaid.desktop.model.DesktopJunkItem
import com.storagemaid.desktop.model.DesktopSimilarPhotoGroup
import com.storagemaid.desktop.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class DesktopDuplicateMode {
    EXACT, SIMILAR_PHOTOS
}

@Composable
fun DesktopDuplicatesView(
    duplicateScanner: DesktopDuplicateScanner,
    similarPhotoScanner: DesktopSimilarPhotoScanner
) {
    val coroutineScope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(DesktopDuplicateMode.EXACT) }
    var duplicateGroups by remember { mutableStateOf<List<DesktopDuplicateGroup>>(emptyList()) }
    var similarGroups by remember { mutableStateOf<List<DesktopSimilarPhotoGroup>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var purgeFeedback by remember { mutableStateOf<String?>(null) }

    val userHome = remember { File(System.getProperty("user.home") ?: "C:\\") }
    val downloadsDir = remember { File(userHome, "Downloads") }
    val documentsDir = remember { File(userHome, "Documents") }
    val picturesDir = remember { File(userHome, "Pictures") }

    fun runScan() {
        if (isScanning) return
        isScanning = true
        purgeFeedback = null
        coroutineScope.launch {
            if (mode == DesktopDuplicateMode.EXACT) {
                val targets = listOf(downloadsDir, documentsDir).filter { it.exists() }
                val res = withContext(Dispatchers.IO) {
                    duplicateScanner.scanDuplicates(targets)
                }
                duplicateGroups = res
            } else {
                val target = if (picturesDir.exists()) picturesDir else downloadsDir
                val res = withContext(Dispatchers.IO) {
                    similarPhotoScanner.scanSimilarPhotos(target)
                }
                similarGroups = res
            }
            isScanning = false
        }
    }

    LaunchedEffect(mode) {
        runScan()
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
                Text("Duplicate & Burst Photo Hunter", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Identifies exact binary duplicates (SHA-256) and perceptual burst photos (64-bit dHash)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { runScan() },
                enabled = !isScanning,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Again")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Mode Switcher Tabs
        TabRow(
            selectedTabIndex = mode.ordinal,
            containerColor = DarkSurface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = mode == DesktopDuplicateMode.EXACT,
                onClick = { mode = DesktopDuplicateMode.EXACT },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exact File Duplicates (${duplicateGroups.size} groups)", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = mode == DesktopDuplicateMode.SIMILAR_PHOTOS,
                onClick = { mode = DesktopDuplicateMode.SIMILAR_PHOTOS },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Similar & Burst Photos (${similarGroups.size} scenes)", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (purgeFeedback != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(purgeFeedback!!, fontSize = 13.sp, color = SafeGreen)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (isScanning) {
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
                    Text(
                        if (mode == DesktopDuplicateMode.EXACT) "Hashing files and matching SHA-256 byte fingerprints..."
                        else "Computing perceptual dHash visual vectors across photo galleries...",
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            if (mode == DesktopDuplicateMode.EXACT) {
                if (duplicateGroups.isEmpty()) {
                    EmptyCard(title = "No duplicate files found", subtitle = "Your scanned folders (Downloads, Documents) are clean of duplicates.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(duplicateGroups) { group ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(group.files.first().name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                "${group.files.size} identical copies • ${group.formattedSize} each",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val (count, freed) = duplicateScanner.deleteSelected(group)
                                                purgeFeedback = "Purged $count copies and reclaimed ${DesktopJunkItem.formatSize(freed)}!"
                                                runScan()
                                            },
                                            enabled = group.selectedFiles.isNotEmpty(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Purge ${group.selectedFiles.size} Selected", fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        group.files.forEachIndexed { idx, file ->
                                            val isOriginal = idx == 0
                                            val isChecked = group.selectedFiles.contains(file)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        if (!isOriginal) {
                                                            if (isChecked) group.selectedFiles.remove(file)
                                                            else group.selectedFiles.add(file)
                                                            duplicateGroups = duplicateGroups.toList()
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (isOriginal) {
                                                        Surface(
                                                            color = SafeGreen.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                "ORIGINAL",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SafeGreen,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Checkbox(
                                                            checked = isChecked,
                                                            onCheckedChange = { checked ->
                                                                if (checked) group.selectedFiles.add(file)
                                                                else group.selectedFiles.remove(file)
                                                                duplicateGroups = duplicateGroups.toList()
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        file.absolutePath,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (isOriginal) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Similar Photos List
                if (similarGroups.isEmpty()) {
                    EmptyCard(title = "No similar burst photos found", subtitle = "Your Pictures folder is clean of redundant burst shots.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(similarGroups) { group ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Similar Scene (${group.photos.size} shots)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                            Text(
                                                "Can save ${DesktopJunkItem.formatSize(group.reclaimableBytes)} by removing redundant shots",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                var count = 0
                                                var freed = 0L
                                                group.photos.filter { it.isSelected && !it.isBestShot }.forEach { photo ->
                                                    try {
                                                        if (photo.file.delete()) {
                                                            count++
                                                            freed += photo.sizeBytes
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                                purgeFeedback = "Purged $count redundant shots and reclaimed ${DesktopJunkItem.formatSize(freed)}!"
                                                runScan()
                                            },
                                            enabled = group.photos.any { it.isSelected && !it.isBestShot },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Clean Redundant", fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        group.photos.forEach { photo ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        if (!photo.isBestShot) {
                                                            photo.isSelected = !photo.isSelected
                                                            similarGroups = similarGroups.toList()
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (photo.isBestShot) {
                                                        Surface(
                                                            color = SafeGreen.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.Star, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("BEST SHOT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                                                            }
                                                        }
                                                    } else {
                                                        Checkbox(
                                                            checked = photo.isSelected,
                                                            onCheckedChange = { checked ->
                                                                photo.isSelected = checked
                                                                similarGroups = similarGroups.toList()
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        photo.file.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (photo.isBestShot) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }

                                                Text(
                                                    DesktopJunkItem.formatSize(photo.sizeBytes),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
