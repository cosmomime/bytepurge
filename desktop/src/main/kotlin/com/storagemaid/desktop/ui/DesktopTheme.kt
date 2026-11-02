package com.storagemaid.desktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0D0F14)
val DarkSurface = Color(0xFF161922)
val DarkSurfaceVariant = Color(0xFF1F2432)
val ElectricIndigo = Color(0xFF6366F1)
val SafeGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val DesktopDarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = ElectricIndigo.copy(alpha = 0.2f),
    secondary = SafeGreen,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun StorageMaidDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopDarkColorScheme
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides TextPrimary,
            content = content
        )
    }
}
