package com.michalmarcin.namelist.ui.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.platform.SettingsStore
import com.michalmarcin.namelist.platform.ThemeMode
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun themeIsSavedAndReadBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(ThemeMode.LIGHT, SettingsStore(context).theme.value)
        SettingsStore(context).setTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, SettingsStore(context).theme.value)
    }

    @Test
    fun themeChipsAndImportNeedsConfirmation() {
        var theme: ThemeMode? = null
        var imports = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                SettingsScreen(
                    state = SettingsUiState(),
                    versionName = "m7-rc1",
                    onBack = {},
                    onThemeChange = { theme = it },
                    onExport = {},
                    onImport = { imports++ },
                )
            }
        }
        compose.onNodeWithTag("theme-LIGHT").performClick()
        assertEquals(ThemeMode.LIGHT, theme)
        compose.onNodeWithTag("import").performClick()
        assertEquals(0, imports)
        compose.onNodeWithTag("confirmImport").performClick()
        assertEquals(1, imports)
        compose.onNodeWithText("Name List m7-rc1 (build 0)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun resultMessages() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                SettingsScreen(
                    state = SettingsUiState(message = BackupMessage.Exported(12, 1)),
                    versionName = "1",
                    onBack = {},
                    onThemeChange = {},
                    onExport = {},
                    onImport = {},
                )
            }
        }
        compose.onNodeWithText("Exported 12 people. 1 photo.").assertIsDisplayed()
    }

    @Test
    fun languageDropdownListsAllLanguagesWithFlagsAndReportsTheChoice() {
        var chosen: com.michalmarcin.namelist.platform.AppLanguage? = null
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                SettingsScreen(
                    state = SettingsUiState(),
                    versionName = "1",
                    onBack = {},
                    onThemeChange = {},
                    onExport = {},
                    onImport = {},
                    onLanguageChange = { chosen = it },
                )
            }
        }
        compose.onNodeWithText("\uD83C\uDF10 System language").assertIsDisplayed()
        compose.onNodeWithTag("languageButton").performClick()
        compose.onNodeWithText("\uD83C\uDDEC\uD83C\uDDE7 English").assertIsDisplayed()
        compose.onNodeWithText("\uD83C\uDDE8\uD83C\uDDFF Čeština").assertIsDisplayed()
        compose.onNodeWithTag("language-SLOVAK").assertIsDisplayed()
        compose.onNodeWithTag("language-SLOVAK").performClick()
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.SLOVAK, chosen)
    }

    @Test
    fun aRemovedLanguageFallsBackToTheSystemLanguage() {
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.SYSTEM, com.michalmarcin.namelist.platform.AppLanguage.fromTag("de"))
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.SYSTEM, com.michalmarcin.namelist.platform.AppLanguage.fromTag("pl"))
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.CZECH, com.michalmarcin.namelist.platform.AppLanguage.fromTag("cs"))
    }

    @Test
    fun languageIsSavedAndReadBack() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.SYSTEM, SettingsStore(context).language.value)
        SettingsStore(context).setLanguage(com.michalmarcin.namelist.platform.AppLanguage.CZECH)
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.CZECH, SettingsStore(context).language.value)
        SettingsStore(context).setLanguage(com.michalmarcin.namelist.platform.AppLanguage.SYSTEM)
        assertEquals(com.michalmarcin.namelist.platform.AppLanguage.SYSTEM, SettingsStore(context).language.value)
    }

    @Test
    fun tagsAndRelationshipTypesOpenFromSettings() {
        val opened = mutableListOf<String>()
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                SettingsScreen(
                    state = SettingsUiState(),
                    versionName = "1",
                    onBack = {},
                    onThemeChange = {},
                    onExport = {},
                    onImport = {},
                    onManageTags = { opened += "tags" },
                    onManageRelationTypes = { opened += "types" },
                )
            }
        }
        compose.onNodeWithTag("manageTagsButton").performScrollTo().performClick()
        compose.onNodeWithTag("manageRelationTypesButton").performScrollTo().performClick()
        assertEquals(listOf("tags", "types"), opened)
    }
}
