package com.michalmarcin.namelist.platform

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What the export does about encryption (F17): ask each time, or always or never encrypt. */
enum class EncryptChoice { ASK, ALWAYS, NEVER }

/** App settings in SharedPreferences (ADR-020): theme and the export encryption default. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val themeState = MutableStateFlow(readTheme())

    val theme: StateFlow<ThemeMode> = themeState.asStateFlow()

    private val encryptState = MutableStateFlow(readEncryptChoice())

    val encryptChoice: StateFlow<EncryptChoice> = encryptState.asStateFlow()

    fun setEncryptChoice(choice: EncryptChoice) {
        prefs.edit().putString(KEY_ENCRYPT, choice.name).apply()
        encryptState.value = choice
    }

    private fun readEncryptChoice(): EncryptChoice =
        EncryptChoice.entries.firstOrNull { it.name == prefs.getString(KEY_ENCRYPT, null) } ?: EncryptChoice.ASK

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        themeState.value = mode
    }

    private fun readTheme(): ThemeMode =
        // Default is Light: the owner wants a white background (ADR-023), even when the phone is in dark mode.
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.LIGHT

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_ENCRYPT = "encrypt_backup"
    }
}
