package com.shavlegi.docreader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shao's style: black / dark blue / white — same palette as his other apps.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4C8DFF),
    onPrimary = Color.White,
    secondary = Color(0xFF7FB0FF),
    onSecondary = Color(0xFF0B0F19),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF121A2B),
    surfaceVariant = Color(0xFF1A2438),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8C2D4),
    error = Color(0xFFFF6B6B),
    onError = Color.White
)

@Composable
fun DocReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
