package com.storagemaid.app.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.scanner.SocialCategory
import com.storagemaid.app.core.scanner.SocialMediaFileItem
import com.storagemaid.app.core.scanner.SocialMediaScanner
import com.storagemaid.app.ui.theme.DangerRed
import com.storagemaid.app.ui.theme.DarkSurfaceVariant
import com.storagemaid.app.ui.theme.SafeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaCleanerSheet(
    scanner: SocialMediaScanner,
    roots: List<File>,
    onRecordClean: (Long, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentCategory by remember { mutableStateOf(SocialCategory.SENT_MEDIA) }
    var isLoading by remember { mutableStateOf(true) }
    val itemsMap = remember { mutableStateMapOf<SocialCategory, List<SocialMediaFileItem>>() }

    // Audio Player State
    var currentlyPlayingPath by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentlyPlayingPath = null
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAudio()
        }
    }

    fun playAudio(file: File) {
        if (currentlyPlayingPath == file.absolutePath) {
            stopAudio()
            return
        }
        stopAudio()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopAudio()
                }
            }
            mediaPlayer = mp
            currentlyPlayingPath = file.absolutePath
        } catch (_: Exception) {
            stopAudio()
            Toast.makeText(context, "Cannot play audio preview", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshItems() {
        coroutineScope.launch {
            isLoading = true
            val res = scanner.scanSocialMedia(roots)
            itemsMap.clear()
            itemsMap.putAll(res)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshItems()
    }

    val currentItems = itemsMap[currentCategory].orEmpty()
    val allSelectedItems = itemsMap.values.flatten().filter { it.isSelected }
    val totalSelectedCount = allSelectedItems.size
    val totalSelectedBytes = allSelectedItems.sumOf { it.sizeBytes }

    ModalBottomSheet(
        onDismissRequest = {
            stopAudio()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Visual Media Purger",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WhatsApp & Telegram visual & voice cleaner",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        stopAudio()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(DarkSurfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = currentCategory.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 20.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentCategory.ordinal]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                SocialCategory.values().forEach { cat ->
                    val count = itemsMap[cat]?.size ?: 0
                    Tab(
                        selected = currentCategory == cat,
                        onClick = {
                            stopAudio()
                            currentCategory = cat
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (cat) {
                                        SocialCategory.SENT_MEDIA -> Icons.AutoMirrored.Filled.Send
                                        SocialCategory.STICKERS -> Icons.Default.Mood
                                        SocialCategory.VOICE_NOTES -> Icons.Default.Mic
                                        SocialCategory.TELEGRAM -> Icons.Default.Forum
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${cat.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (currentCategory == cat) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Quick Select / Deselect Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allCurrentSelected = currentItems.isNotEmpty() && currentItems.all { it.isSelected }

                TextButton(
                    onClick = {
                        val newSelected = !allCurrentSelected
                        val updated = currentItems.map { it.copy(isSelected = newSelected) }
                        itemsMap[currentCategory] = updated
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (allCurrentSelected) "Deselect All" else "Select All in Tab",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${currentItems.size} items • ${JunkItem.formatSize(currentItems.sumOf { it.sizeBytes })}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Scanning social media junk...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (currentItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No ${currentCategory.label.lowercase()} found", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // True Visual Purger: 3-column Grid for Sent Media and Stickers
                    val isVisualGrid = currentCategory == SocialCategory.SENT_MEDIA || currentCategory == SocialCategory.STICKERS

                    if (isVisualGrid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentItems, key = { it.id }) { item ->
                                VisualMediaGridCard(
                                    item = item,
                                    onToggle = {
                                        val updated = currentItems.map {
                                            if (it.id == item.id) it.copy(isSelected = !it.isSelected) else it
                                        }
                                        itemsMap[currentCategory] = updated
                                    },
                                    onClick = {
                                        openFileSafely(context, item.file)
                                    }
                                )
                            }
                        }
                    } else {
                        // Voice Notes & Telegram Audio/Doc List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentItems, key = { it.id }) { item ->
                                VoiceNoteListRow(
                                    item = item,
                                    isPlaying = currentlyPlayingPath == item.file.absolutePath,
                                    onToggle = {
                                        val updated = currentItems.map {
                                            if (it.id == item.id) it.copy(isSelected = !it.isSelected) else it
                                        }
                                        itemsMap[currentCategory] = updated
                                    },
                                    onPlayPauseAudio = {
                                        playAudio(item.file)
                                    },
                                    onClick = {
                                        if (item.isAudio) {
                                            playAudio(item.file)
                                        } else {
                                            openFileSafely(context, item.file)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Batch Deletion Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = totalSelectedCount > 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "$totalSelectedCount media selected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Reclaim ${JunkItem.formatSize(totalSelectedBytes)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeGreen
                                    )
                                }

                                Button(
                                    onClick = { showDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Purge", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Purge $totalSelectedCount items?") },
            text = { Text("Permanently delete $totalSelectedCount selected file(s) freeing ${JunkItem.formatSize(totalSelectedBytes)}. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        stopAudio()
                        coroutineScope.launch {
                            val (count, freed) = scanner.deleteSocialFiles(allSelectedItems)
                            onRecordClean(freed, count)
                            Toast.makeText(context, "Cleaned $count items (${JunkItem.formatSize(freed)})", Toast.LENGTH_SHORT).show()
                            refreshItems()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 3-Column Photo Grid Thumbnail Card for Visual Purger
 */
@Composable
private fun VisualMediaGridCard(
    item: SocialMediaFileItem,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    var thumbnailBitmap by remember(item.file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                if (item.file.exists()) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(item.file.absolutePath, bounds)
                    val targetDim = 200
                    var sample = 1
                    if (bounds.outHeight > targetDim || bounds.outWidth > targetDim) {
                        val halfHeight = bounds.outHeight / 2
                        val halfWidth = bounds.outWidth / 2
                        while ((halfHeight / sample) >= targetDim && (halfWidth / sample) >= targetDim) {
                            sample *= 2
                        }
                    }
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    thumbnailBitmap = BitmapFactory.decodeFile(item.file.absolutePath, opts)
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = if (item.isSelected) 2.dp else 1.dp,
                color = if (item.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Gradient shadow at the bottom for size text legibility
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // File Size Badge (Bottom Left)
        Text(
            text = item.formattedSize,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        // Preview Eye Icon (Bottom Right)
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .padding(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Preview full size",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
        }

        // Selection Checkmark Overlay (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .background(
                    if (item.isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not selected",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * List Row for Voice Notes and Telegram Documents
 */
@Composable
private fun VoiceNoteListRow(
    item: SocialMediaFileItem,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onPlayPauseAudio: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio Play/Pause Button
            if (item.isAudio) {
                IconButton(
                    onClick = onPlayPauseAudio,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isPlaying) SafeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play voice note",
                        tint = if (isPlaying) SafeGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = item.formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun openFileSafely(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open this file", Toast.LENGTH_SHORT).show()
    }
}
