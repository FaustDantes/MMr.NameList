package com.michalmarcin.namelist.ui.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("Name List m7-rc1 (build 0)").assertIsDisplayed()
    }

    @Test
    fun resultMessages() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                SettingsScreen(SettingsUiState(message = BackupMessage.Exported(12, 1)), "1", {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Exported 12 people. 1 photo.").assertIsDisplayed()
    }
}
