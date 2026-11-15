package com.michalmarcin.namelist.platform

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App settings in SharedPreferences (ADR-020). Only the theme for now. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val themeState = MutableStateFlow(readTheme())

    val theme: StateFlow<ThemeMode> = themeState.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        themeState.value = mode
    }

    private fun readTheme(): ThemeMode =
        // Default is Light: the owner wants a white background (ADR-023), even when the phone is in dark mode.
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.LIGHT

    private companion object {
        const val KEY_THEME = "theme"
    }
}
