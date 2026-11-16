package com.michalmarcin.namelist.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * The language of the app (F27). [SYSTEM] follows the phone. The others are chosen in Settings.
 * [nativeName] is written in the language itself, so a user can find it even when the app shows another one.
 */
enum class AppLanguage(val tag: String?, val flag: String, val nativeName: String) {
    SYSTEM(null, "🌐", ""),
    ENGLISH("en", "🇬🇧", "English"),
    SLOVAK("sk", "🇸🇰", "Slovenčina"),
    CZECH("cs", "🇨🇿", "Čeština"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag != null && it.tag == tag } ?: SYSTEM

        /** The context with the chosen language, for `Activity.attachBaseContext`. Also sets the default locale for dates. */
        fun wrap(base: Context): Context {
            val chosen = fromTag(SettingsStore.savedLanguageTag(base))
            val tag = chosen.tag ?: return base
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocales(LocaleList(locale))
            return base.createConfigurationContext(config)
        }
    }
}
