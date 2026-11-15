package com.michalmarcin.namelist.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Owner palette (ADR-023): white background, black text, light blue buttons.
// Wallpaper colors (dynamic color) are off, so the app looks the same on every phone.

private val LightBlue = Color(0xFFCFE6FF)
private val Blue = Color(0xFF0B5CAD) // Text buttons and accents: light blue text on white is not readable.
/** Dark navy: the add button in the app and on the home-screen widget (ADR-032). */
val Navy = Color(0xFF0B2545)
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)

internal val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = White,
    primaryContainer = LightBlue,
    onPrimaryContainer = Black,
    secondary = Color(0xFF2F5F8A),
    onSecondary = White,
    secondaryContainer = LightBlue,
    onSecondaryContainer = Black,
    tertiary = Color(0xFF4A5F75),
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Color(0xFFE6EBF0),
    onSurfaceVariant = Color(0xFF3D4349),
    outline = Color(0xFF73787E),
    outlineVariant = Color(0xFFC3C8CE),
    surfaceContainerLowest = White,
    surfaceContainerLow = Color(0xFFF5F7F9),
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceContainerHigh = Color(0xFFEBEEF1),
    surfaceContainerHighest = Color(0xFFE5E8EC),
    surfaceTint = White,
)

// Dark theme (Settings, Theme, Dark): black background, white text, the same light blue buttons.
internal val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = LightBlue,
    onPrimaryContainer = Black,
    secondary = Color(0xFFB4CCE6),
    onSecondary = Color(0xFF1C3249),
    secondaryContainer = LightBlue,
    onSecondaryContainer = Black,
    tertiary = Color(0xFFB4C8DE),
    onTertiary = Color(0xFF1E3245),
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Color(0xFF2A2E33),
    onSurfaceVariant = Color(0xFFC3C8CE),
    outline = Color(0xFF8D9298),
    outlineVariant = Color(0xFF3D4349),
    surfaceContainerLowest = Black,
    surfaceContainerLow = Color(0xFF121417),
    surfaceContainer = Color(0xFF17191C),
    surfaceContainerHigh = Color(0xFF1F2226),
    surfaceContainerHighest = Color(0xFF272A2E),
    surfaceTint = Black,
)
