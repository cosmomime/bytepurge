package com.storagemaid.desktop.ui.squeezer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.engine.MediaSqueezerEngine
import com.storagemaid.desktop.model.DesktopJunkItem
import com.storagemaid.desktop.model.SqueezeTarget
import com.storagemaid.desktop.ui.DarkSurface
import com.storagemaid.desktop.ui.SafeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopMediaSqueezerView(engine: MediaSqueezerEngine) {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var targets by remember { mutableStateOf<List<SqueezeTarget>>(emptyList()) }
    var compressionQuality by remember { mutableStateOf(0.78f) }
    var isCompressingAll by remember { mutableStateOf(false) }

    fun refresh() {
        coroutineScope.launch {
            isScanning = true
            val found = withContext(Dispatchers.IO) { engine.scanHeavyPhotos(minSizeBytes = 1 * 1024 * 1024) }
            targets = found
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val totalOriginal = targets.sumOf { it.originalSizeBytes }
    val totalSaved = targets.filter { it.status == "Compressed" }
        .sumOf { (it.originalSizeBytes - it.compressedSizeBytes).coerceAtLeast(0) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Media Squeezer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Downsize bulky uncompressed photos and screenshots by up to 80% with lossless visual quality.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { refresh() },
                    enabled = !isScanning && !isCompressingAll,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-Scan")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCompressingAll = true
                            val updated = targets.toMutableList()
                            for (i in updated.indices) {
                                if (updated[i].status == "Ready") {
                                    val res = withContext(Dispatchers.IO) {
                                        engine.squeezePhoto(updated[i], compressionQuality)
                                    }
                                    updated[i] = res
                                    targets = updated.toList()
                                }
                            }
                            isCompressingAll = false
                        }
                    },
                    enabled = !isScanning && !isCompressingAll && targets.any { it.status == "Ready" },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCompressingAll) "Compressing..." else "Squeeze All Media")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Stat cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Heavy Photos Found (>1MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${targets.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Uncompressed Footprint", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(DesktopJunkItem.formatSize(totalOriginal), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reclaimed Space", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        DesktopJunkItem.formatSize(totalSaved),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (targets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No heavy media files found in Pictures or Downloads.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(targets, key = { it.file.absolutePath }) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        item.file.absolutePath,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(item.formattedOriginal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (item.status == "Compressed") {
                                        Text(
                                            "-> ${item.formattedCompressed} (-${item.savingsPercentage}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SafeGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                if (item.status == "Ready") {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = withContext(Dispatchers.IO) {
                                                    engine.squeezePhoto(item, compressionQuality)
                                                }
                                                targets = targets.map { if (it.file == item.file) res else it }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Squeeze", fontSize = 12.sp)
                                    }
                                } else if (item.status == "Compressed") {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SafeGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "Done",
                                            color = SafeGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
