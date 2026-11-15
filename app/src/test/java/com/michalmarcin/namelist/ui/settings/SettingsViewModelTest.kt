package com.michalmarcin.namelist.ui.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.platform.BackupManager
import com.michalmarcin.namelist.platform.BackupPasswordStore
import com.michalmarcin.namelist.platform.EncryptChoice
import com.michalmarcin.namelist.platform.PhotoStore
import com.michalmarcin.namelist.platform.SettingsStore
import com.michalmarcin.namelist.testutil.MainDispatcherRule
import com.michalmarcin.namelist.testutil.await
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

/** F17: the export question, the password in settings and the import password. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SettingsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsStore
    private lateinit var passwords: BackupPasswordStore
    private lateinit var backup: BackupManager
    private lateinit var vm: SettingsViewModel

    @Before
    fun setUp() {
        db = AppDatabase.build(context, inMemory = true)
        settings = SettingsStore(context)
        passwords = BackupPasswordStore(context, iterations = 1_000)
        backup = BackupManager(context, db, PhotoStore(context))
        vm = SettingsViewModel(settings, backup, passwords)
    }

    @After
    fun tearDown() = db.close()

    private fun state(check: (SettingsUiState) -> Boolean = { true }) = vm.uiState.await(condition = check)

    private fun setPassword(text: String) = runBlocking { passwords.set(text.toCharArray()) }

    // ---- The export question ----

    @Test
    fun askingIsTheDefaultAndShowsTheQuestion() {
        assertEquals(EncryptChoice.ASK, state().encryptChoice)
        vm.onExportClicked()
        assertEquals(SettingsDialog.AskEncrypt, state { it.dialog != null }.dialog)
        assertNull(state().saveRequest)
    }

    @Test
    fun answeringNoGoesStraightToAPlainFile() {
        vm.onExportClicked()
        vm.onEncryptAnswer(encrypt = false, dontAskAgain = false)
        val s = state { it.saveRequest != null }
        assertEquals(SaveRequest(encrypted = false), s.saveRequest)
        assertNull(s.dialog)
        assertEquals("not remembered", EncryptChoice.ASK, s.encryptChoice)
    }

    @Test
    fun doNotShowAgainWithNoMakesNeverTheDefault() {
        vm.onExportClicked()
        vm.onEncryptAnswer(encrypt = false, dontAskAgain = true)
        assertEquals(EncryptChoice.NEVER, state { it.encryptChoice == EncryptChoice.NEVER }.encryptChoice)

        vm.onSaveRequestHandled()
        vm.onExportClicked()
        val s = state { it.saveRequest != null }
        assertNull("no question any more", s.dialog)
        assertEquals(SaveRequest(encrypted = false), s.saveRequest)
        assertEquals(EncryptChoice.NEVER, SettingsStore(context).encryptChoice.value)
    }

    @Test
    fun doNotShowAgainWithYesMakesAlwaysTheDefault() {
        setPassword("my secret 1")
        vm.onExportClicked()
        vm.onEncryptAnswer(encrypt = true, dontAskAgain = true)
        assertEquals(EncryptChoice.ALWAYS, state { it.encryptChoice == EncryptChoice.ALWAYS }.encryptChoice)
        assertEquals(SettingsDialog.EnterPassword, state { it.dialog != null }.dialog)
    }

    @Test
    fun theDefaultCanBeChangedInSettings() {
        vm.setEncryptChoice(EncryptChoice.ALWAYS)
        assertEquals(EncryptChoice.ALWAYS, state { it.encryptChoice == EncryptChoice.ALWAYS }.encryptChoice)
        vm.setEncryptChoice(EncryptChoice.ASK)
        vm.onExportClicked()
        assertEquals(SettingsDialog.AskEncrypt, state { it.dialog != null }.dialog)
    }

    // ---- Encrypting an export ----

    @Test
    fun encryptingWithoutAPasswordFirstAsksToSetOne() {
        vm.onExportClicked()
        vm.onEncryptAnswer(encrypt = true, dontAskAgain = false)
        assertEquals(SettingsDialog.SetPassword(forExport = true), state { it.dialog != null }.dialog)
    }

    @Test
    fun theNewPasswordMustBeLongEnoughAndRepeatedRight() {
        vm.onSetPasswordClicked()
        vm.onSetPassword("short", "short", forExport = false)
        assertEquals(PasswordError.TOO_SHORT, state { it.passwordError != null }.passwordError)
        vm.onSetPassword("long enough 1", "long enough 2", forExport = false)
        assertEquals(PasswordError.MISMATCH, state { it.passwordError == PasswordError.MISMATCH }.passwordError)
        assertFalse(passwords.isSet.value)
        assertTrue("dialog stays open", state().dialog is SettingsDialog.SetPassword)
    }

    @Test
    fun settingAPasswordInSettingsSavesItAndConfirms() {
        vm.onSetPasswordClicked()
        vm.onSetPassword("long enough 1", "long enough 1", forExport = false)
        val s = state { it.hasPassword }
        assertEquals(BackupMessage.PasswordSet, s.message)
        assertNull(s.dialog)
        assertNull("no export was asked for", s.saveRequest)
    }

    @Test
    fun settingAPasswordForAnExportContinuesToTheFilePicker() {
        vm.onExportClicked()
        vm.onEncryptAnswer(encrypt = true, dontAskAgain = false)
        vm.onSetPassword("long enough 1", "long enough 1", forExport = true)
        val s = state { it.saveRequest != null }
        assertEquals(SaveRequest(encrypted = true), s.saveRequest)
        assertTrue(s.hasPassword)
    }

    @Test
    fun theSavedPasswordMustBeTypedToEncryptAnExport() {
        setPassword("my secret 1")
        vm.setEncryptChoice(EncryptChoice.ALWAYS)
        vm.onExportClicked()
        assertEquals(SettingsDialog.EnterPassword, state { it.dialog != null }.dialog)

        vm.onEnterPassword("my secret 2")
        assertEquals(PasswordError.WRONG_PASSWORD, state { it.passwordError != null }.passwordError)
        assertNull(state().saveRequest)

        vm.onEnterPassword("my secret 1")
        val s = state { it.saveRequest != null }
        assertEquals(SaveRequest(encrypted = true), s.saveRequest)
        assertNull(s.dialog)
    }

    @Test
    fun cancellingTheDialogEndsTheExport() {
        setPassword("my secret 1")
        vm.setEncryptChoice(EncryptChoice.ALWAYS)
        vm.onExportClicked()
        vm.onDismissDialog()
        val s = state { it.dialog == null }
        assertNull(s.saveRequest)
        assertNull(s.passwordError)
    }

    // ---- Change, remove, forgot ----

    @Test
    fun changingNeedsTheOldPassword() {
        setPassword("first password")
        vm.onChangePasswordClicked()
        vm.onChangePassword("not the old one", "second password", "second password")
        assertEquals(PasswordError.WRONG_PASSWORD, state { it.passwordError != null }.passwordError)
        assertTrue(runBlocking { passwords.verify("first password".toCharArray()) })

        vm.onChangePassword("first password", "second password", "second password")
        val s = state { it.message == BackupMessage.PasswordChanged }
        assertNull(s.dialog)
        assertTrue(runBlocking { passwords.verify("second password".toCharArray()) })
    }

    @Test
    fun theNewPasswordIsCheckedBeforeTheOldOneIsTried() {
        setPassword("first password")
        vm.onChangePasswordClicked()
        vm.onChangePassword("first password", "short", "short")
        assertEquals(PasswordError.TOO_SHORT, state { it.passwordError != null }.passwordError)
        assertTrue("old password unchanged", runBlocking { passwords.verify("first password".toCharArray()) })
    }

    @Test
    fun removingNeedsTheOldPassword() {
        setPassword("first password")
        vm.onRemovePasswordClicked()
        vm.onRemovePassword("wrong wrong")
        assertEquals(PasswordError.WRONG_PASSWORD, state { it.passwordError != null }.passwordError)
        assertTrue(passwords.isSet.value)

        vm.onRemovePassword("first password")
        val s = state { !it.hasPassword }
        assertEquals(BackupMessage.PasswordRemoved, s.message)
    }

    @Test
    fun aForgottenPasswordCanBeResetAfterConfirmation() {
        setPassword("first password")
        vm.onForgotPasswordClicked()
        assertEquals(SettingsDialog.ResetPassword, state { it.dialog != null }.dialog)
        vm.onConfirmReset()
        val s = state { !it.hasPassword }
        assertEquals(BackupMessage.PasswordRemoved, s.message)
        assertNull(s.dialog)
    }

    // ---- Import ----

    private fun encryptedFile(password: String): Uri {
        val out = ByteArrayOutputStream()
        runBlocking { backup.export(out, password.toCharArray()) }
        val file = File(context.cacheDir, "enc-${System.nanoTime()}.nlbak").apply { writeBytes(out.toByteArray()) }
        return Uri.fromFile(file)
    }

    private fun plainFile(): Uri {
        val out = ByteArrayOutputStream()
        runBlocking { backup.export(out) }
        val file = File(context.cacheDir, "plain-${System.nanoTime()}.zip").apply { writeBytes(out.toByteArray()) }
        return Uri.fromFile(file)
    }

    @Test
    fun anEncryptedFileAsksForItsPasswordAndImportsWithIt() {
        val uri = encryptedFile("file password")
        vm.import(uri)
        assertEquals(SettingsDialog.ImportPassword(uri), state { it.dialog != null }.dialog)

        vm.onImportPassword(uri, "file password")
        val s = state { it.message is BackupMessage.Imported }
        assertNull(s.dialog)
    }

    @Test
    fun aWrongImportPasswordShowsAMessageAndChangesNothing() {
        val uri = encryptedFile("file password")
        vm.import(uri)
        vm.onImportPassword(uri, "other password")
        assertEquals(BackupMessage.WrongBackupPassword, state { it.message != null }.message)
    }

    @Test
    fun aPlainFileImportsWithoutAQuestion() {
        vm.import(plainFile())
        val s = state { it.message != null }
        assertTrue(s.message is BackupMessage.Imported)
        assertNull(s.dialog)
    }

    @Test
    fun cancellingTheFilePickerDoesNothing() {
        vm.import(null)
        vm.export(null)
        val s = state()
        assertNull(s.message)
        assertNull(s.dialog)
    }
}
