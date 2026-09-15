package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TubeP3ColorScheme = darkColorScheme(
    primary = TubeP3Red,
    onPrimary = WhiteText,
    primaryContainer = DarkGraySurfaceVariant,
    onPrimaryContainer = WhiteText,
    secondary = TubeP3Red,
    onSecondary = WhiteText,
    secondaryContainer = DarkGraySurface,
    onSecondaryContainer = LightGraySecondary,
    tertiary = TubeP3Red,
    onTertiary = WhiteText,
    background = AmoledBlack,
    onBackground = WhiteText,
    surface = AmoledBlack,
    onSurface = WhiteText,
    surfaceVariant = DarkGraySurface,
    onSurfaceVariant = LightGraySecondary,
    surfaceContainer = DarkGraySurface,
    surfaceContainerHigh = DarkGraySurfaceVariant,
    outline = SubtleGrayBorder,
    outlineVariant = SubtleGrayBorder,
    error = ErrorRed,
    onError = WhiteText
)

@Composable
fun TubeP3Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TubeP3ColorScheme,
        typography = Typography,
        content = content
    )
}
