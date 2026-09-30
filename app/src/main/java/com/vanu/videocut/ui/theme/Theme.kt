package com.vanu.videocut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF7C5CFF)
val PurpleDim = Color(0xFF5A3FD4)
val Amber = Color(0xFFFFB84D)
val SurfaceDark = Color(0xFF0D0D0F)
val CardDark = Color(0xFF1A1A1F)
val CardElevated = Color(0xFF24242B)
val TextPrimary = Color(0xFFF2F2F5)
val TextSecondary = Color(0xFFA0A0AB)
val Danger = Color(0xFFFF5C7A)

private val DarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Amber,
    onSecondary = Color.Black,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = CardElevated,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = Color.White,
)

@Composable
fun VideoCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
