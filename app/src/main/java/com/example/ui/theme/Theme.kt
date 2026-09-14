package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PythonYellow,
    onPrimary = Color(0xFF1E1F2B),
    primaryContainer = PythonBlue,
    onPrimaryContainer = Color.White,
    secondary = AccentCyan,
    onSecondary = Color(0xFF13141C),
    secondaryContainer = Color(0xFF282A36),
    onSecondaryContainer = AccentCyan,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA6ADC8),
    error = AccentRed,
    onError = Color.White,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PythonBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = PythonBlue,
    secondary = PythonGold,
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = AccentGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    error = AccentRed,
    onError = Color.White,
    outline = LightBorder
)

@Composable
fun PyPocketTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PyPocketTheme(darkTheme = darkTheme, content = content)
}
