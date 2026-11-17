package com.michalmarcin.namelist.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "Line" design (owner, 2026-11-17, ADR-038): white or black, thin lines instead of cards, one blue accent.
// Wallpaper colors (dynamic color) stay off, so the app looks the same on every phone.

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)

/** Accent in the light theme: buttons, the selected tag, links. 6.6:1 on white. */
private val Blue = Color(0xFF0B5CAD)

/** Accent in the dark theme. 7.9:1 on black. */
private val SkyBlue = Color(0xFF5AA9FF)

/** Kept for the launcher background and old references; the add button now uses the accent (ADR-038). */
val Navy = Color(0xFF0B2545)

internal val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = White,
    primaryContainer = Blue,
    onPrimaryContainer = White,
    secondary = Blue,
    onSecondary = White,
    secondaryContainer = Color(0xFFE8F1FB),
    onSecondaryContainer = Black,
    tertiary = Color(0xFF3D3D3D),
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Color(0xFFF2F2F2),
    // Muted text: 5.3:1 on white.
    onSurfaceVariant = Color(0xFF6B6B6B),
    // Field lines and outlined buttons: 3:1 or more on white.
    outline = Color(0xFF8F8F8F),
    // Hairlines between rows.
    outlineVariant = Color(0xFFEAEAEA),
    error = Color(0xFFC62828),
    onError = White,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Color(0xFFF2F2F2),
    surfaceTint = White,
    scrim = Black,
)

internal val DarkColors = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Black,
    primaryContainer = SkyBlue,
    onPrimaryContainer = Black,
    secondary = SkyBlue,
    onSecondary = Black,
    secondaryContainer = Color(0xFF0E2236),
    onSecondaryContainer = White,
    tertiary = Color(0xFFCFCFCF),
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Color(0xFF1A1A1A),
    // Muted text: 6.6:1 on black.
    onSurfaceVariant = Color(0xFF8C8C8C),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF1F1F1F),
    error = Color(0xFFFF8A80),
    onError = Black,
    surfaceContainerLowest = Black,
    surfaceContainerLow = Black,
    surfaceContainer = Color(0xFF0A0A0A),
    // Dialogs and sheets lift off the black background.
    surfaceContainerHigh = Color(0xFF111111),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceTint = Black,
    scrim = Black,
)
