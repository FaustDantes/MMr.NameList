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

    private val languageState = MutableStateFlow(AppLanguage.fromTag(savedLanguageTag(context)))

    val language: StateFlow<AppLanguage> = languageState.asStateFlow()

    /** Saves the language. The activity must be recreated to show it (F27). */
    fun setLanguage(language: AppLanguage) {
        prefs.edit().apply { language.tag?.let { putString(KEY_LANGUAGE, it) } ?: remove(KEY_LANGUAGE) }.apply()
        languageState.value = language
    }

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

    companion object {
        /** The saved language tag, or null for "system". Read before the app has a settings store. */
        fun savedLanguageTag(context: Context): String? =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)

        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_ENCRYPT = "encrypt_backup"
    }
}
