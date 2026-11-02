package com.storagemaid.desktop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.DesktopNavTab
import com.storagemaid.desktop.ui.*
import java.io.File
import java.util.Locale

@Composable
fun DesktopHomeView(
    onNavigate: (DesktopNavTab) -> Unit
) {
    // Read Drive (C:) metrics dynamically
    val cDrive = remember { File("C:\\") }
    val totalBytes = remember { cDrive.totalSpace.coerceAtLeast(1L) }
    val freeBytes = remember { cDrive.usableSpace.coerceAtLeast(0L) }
    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedRatio = (usedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Breadcrumb
        Text(
            text = "Home Hub",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Hero Greeting & Professional Headline
        Text(
            text = "SYSTEM OPTIMIZATION HUB",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Keep your PC lean, fast, and responsive.",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scan drives, purge shader bloat, or manage startup apps from one unified workspace.",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 3 Balanced, Goal-Oriented Action Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoalCard(
                title = "Free Up Space",
                subtitle = "Purge temp files, system caches, and browser clutter safely.",
                footerAction = "Clean junk files",
                icon = Icons.Default.CleaningServices,
                iconTint = ElectricIndigo,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onNavigate(DesktopNavTab.SWEEP) }
            )

            GoalCard(
                title = "Boost Gaming Speed",
                subtitle = "Clear bloated GPU shaders, DirectX cache, and launcher junk.",
                footerAction = "Hunt game shaders",
                icon = Icons.Default.SportsEsports,
                iconTint = Color(0xFFA855F7), // Purple accent
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onNavigate(DesktopNavTab.GAMER) }
            )

            GoalCard(
                title = "Analyze Storage",
                subtitle = "Visualize drive treemaps, find huge files, and detect duplicates.",
                footerAction = "Inspect drive usage",
                icon = Icons.Default.PieChart,
                iconTint = SafeGreen,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onNavigate(DesktopNavTab.ANALYZER) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Secondary Functional Cards: Live Drive Health & Startup Optimizer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live System Drive (C:) Status Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
                    .clickable { onNavigate(DesktopNavTab.SWEEP) },
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = ElectricIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "System Drive (C:)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${formatStorageBytes(freeBytes)} free of ${formatStorageBytes(totalBytes)}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Storage Utilization",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(usedRatio * 100).toInt()}% Used",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (usedRatio > 0.85f) DangerRed else TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { usedRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (usedRatio > 0.85f) DangerRed else ElectricIndigo,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            // Startup & Boot Optimizer Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
                    .clickable { onNavigate(DesktopNavTab.STARTUP) },
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speed Up Windows Boot",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Review high-impact startup apps to cut Windows boot time and free idle RAM.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bottom Guarantee & Advanced Mode Navigation Hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Safe by default. Review candidate files and caches before confirming any deletion.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigate(DesktopNavTab.ANALYZER) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Want deep disk treemaps & filters? Switch to Disk Analyzer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    subtitle: String,
    footerAction: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Top-Right Arrow Affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Subtitle with fixed minimum lines for absolute consistency
            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    minLines = 2,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer action text with arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = footerAction,
                    fontSize = 12.sp,
                    color = iconTint,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0) return "0 GB"
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1000) {
        val tb = gb / 1024
        String.format(Locale.US, "%.1f TB", tb)
    } else {
        String.format(Locale.US, "%.1f GB", gb)
    }
}
