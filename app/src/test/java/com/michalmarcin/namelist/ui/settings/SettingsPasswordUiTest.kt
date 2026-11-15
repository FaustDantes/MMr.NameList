package com.michalmarcin.namelist.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.platform.EncryptChoice
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** F17: the settings sections and the dialog contents. Contents are tested outside the dialog window (see ADR on tests). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsPasswordUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun settings(state: SettingsUiState, actions: SettingsActions = SettingsActions()) {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                SettingsScreen(state, versionName = "1.1.0", onBack = {}, onThemeChange = {}, onExport = {}, onImport = {}, actions = actions)
            }
        }
    }

    @Test
    fun withoutAPasswordOnlySetIsOffered() {
        var set = 0
        settings(SettingsUiState(hasPassword = false), SettingsActions(onSetPasswordClicked = { set++ }))
        compose.onNodeWithText("No password set.").assertIsDisplayed()
        compose.onNodeWithTag("changePassword").assertDoesNotExist()
        compose.onNodeWithTag("forgotPassword").assertDoesNotExist()
        compose.onNodeWithTag("setPassword").performClick()
        assertEquals(1, set)
    }

    @Test
    fun withAPasswordChangeRemoveAndForgotAreOffered() {
        var change = 0
        var remove = 0
        var forgot = 0
        settings(
            SettingsUiState(hasPassword = true),
            SettingsActions(
                onChangePasswordClicked = { change++ },
                onRemovePasswordClicked = { remove++ },
                onForgotPasswordClicked = { forgot++ },
            ),
        )
        compose.onNodeWithText("A password is set.").assertIsDisplayed()
        compose.onNodeWithTag("setPassword").assertDoesNotExist()
        compose.onNodeWithTag("changePassword").performClick()
        compose.onNodeWithTag("removePassword").performClick()
        compose.onNodeWithTag("forgotPassword").performClick()
        assertEquals(listOf(1, 1, 1), listOf(change, remove, forgot))
    }

    @Test
    fun theExportDefaultCanBeChosenAndShowsTheCurrentChoice() {
        val chosen = mutableListOf<EncryptChoice>()
        settings(SettingsUiState(encryptChoice = EncryptChoice.NEVER), SettingsActions(onEncryptChoice = { chosen += it }))
        compose.onNodeWithTag("encrypt-ALWAYS").performClick()
        compose.onNodeWithTag("encrypt-ASK").performClick()
        assertEquals(listOf(EncryptChoice.ALWAYS, EncryptChoice.ASK), chosen)
    }

    @Test
    fun passwordMessagesAreShown() {
        settings(SettingsUiState(message = BackupMessage.WrongBackupPassword))
        compose.onNodeWithText("Wrong password, or the file is damaged. Nothing was changed.").assertIsDisplayed()
    }

    @Test
    fun theEncryptQuestionReportsTheAnswerAndTheCheckbox() {
        val answers = mutableListOf<Pair<Boolean, Boolean>>()
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                EncryptQuestionContent(onAnswer = { e, d -> answers += e to d }, onCancel = {})
            }
        }
        compose.onNodeWithText("Encrypt this backup?").assertIsDisplayed()
        compose.onNodeWithTag("encryptNo").performClick()
        compose.onNodeWithTag("dontAskAgain").performClick()
        compose.onNodeWithTag("encryptYes").performClick()
        assertEquals(listOf(false to false, true to true), answers)
    }

    @Test
    fun theChangeFormSendsAllThreePasswordsInOrder() {
        var sent: List<String>? = null
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PasswordFormContent(
                    title = "Change password",
                    fields = listOf(com.michalmarcin.namelist.R.string.old_password, com.michalmarcin.namelist.R.string.new_password, com.michalmarcin.namelist.R.string.repeat_password),
                    error = null,
                    confirmLabel = "Save",
                    onConfirm = { sent = it },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithTag("password-0").performTextInput("old one 1")
        compose.onNodeWithTag("password-1").performTextInput("new one 2")
        compose.onNodeWithTag("password-2").performTextInput("new one 2")
        compose.onNodeWithTag("confirmPassword").performClick()
        assertEquals(listOf("old one 1", "new one 2", "new one 2"), sent)
    }

    @Test
    fun theFormShowsTheErrorText() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PasswordFormContent(
                    title = "Set password",
                    fields = listOf(com.michalmarcin.namelist.R.string.new_password),
                    error = PasswordError.TOO_SHORT,
                    confirmLabel = "Save",
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithText("Use at least 8 characters.").assertIsDisplayed()
    }
}
