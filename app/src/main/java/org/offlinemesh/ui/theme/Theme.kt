package org.offlinemesh.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkAirLinkColors.primary,
    onPrimary = Color.White,
    primaryContainer = DarkAirLinkColors.primaryDark,
    onPrimaryContainer = DarkAirLinkColors.textPrimary,
    secondary = DarkAirLinkColors.cyan,
    onSecondary = Color.White,
    background = DarkAirLinkColors.background,
    onBackground = DarkAirLinkColors.textPrimary,
    surface = DarkAirLinkColors.surface,
    onSurface = DarkAirLinkColors.textPrimary,
    surfaceVariant = DarkAirLinkColors.surfaceVariant,
    onSurfaceVariant = DarkAirLinkColors.textSecondary,
    error = DarkAirLinkColors.red,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightAirLinkColors.primary,
    onPrimary = Color.White,
    primaryContainer = LightAirLinkColors.surfaceVariant,
    onPrimaryContainer = LightAirLinkColors.textPrimary,
    secondary = LightAirLinkColors.cyan,
    onSecondary = Color.White,
    background = LightAirLinkColors.background,
    onBackground = LightAirLinkColors.textPrimary,
    surface = LightAirLinkColors.surface,
    onSurface = LightAirLinkColors.textPrimary,
    surfaceVariant = LightAirLinkColors.surfaceVariant,
    onSurfaceVariant = LightAirLinkColors.textSecondary,
    error = LightAirLinkColors.red,
    onError = Color.White
)

@Composable
fun OfflineMeshTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val airLinkColors = if (darkTheme) DarkAirLinkColors else LightAirLinkColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalAirLinkColors provides airLinkColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
