package com.michalmarcin.namelist.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.platform.BackupException
import com.michalmarcin.namelist.platform.BackupManager
import com.michalmarcin.namelist.platform.BackupPasswordException
import com.michalmarcin.namelist.platform.BackupPasswordStore
import com.michalmarcin.namelist.platform.EncryptChoice
import com.michalmarcin.namelist.platform.SettingsStore
import com.michalmarcin.namelist.platform.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface BackupMessage {
    data class Exported(val people: Int, val photos: Int, val encrypted: Boolean = false) : BackupMessage
    data class Imported(val people: Int, val photos: Int) : BackupMessage
    data object NotABackup : BackupMessage
    data object Failed : BackupMessage

    /** The password for an encrypted backup was wrong, or the file is damaged. */
    data object WrongBackupPassword : BackupMessage
    data object PasswordSet : BackupMessage
    data object PasswordChanged : BackupMessage
    data object PasswordRemoved : BackupMessage
}

/** What the open dialog asks for (F17). */
sealed interface SettingsDialog {
    /** "Encrypt this backup?" with "Do not show this again". */
    data object AskEncrypt : SettingsDialog

    /** New password and repeat. [forExport] = the export goes on after it is saved. */
    data class SetPassword(val forExport: Boolean) : SettingsDialog

    /** The saved password, typed to encrypt this export. */
    data object EnterPassword : SettingsDialog

    data object ChangePassword : SettingsDialog

    data object RemovePassword : SettingsDialog

    /** "Forgot the password": confirmation only. */
    data object ResetPassword : SettingsDialog

    /** The password of the encrypted backup file at [uri]. */
    data class ImportPassword(val uri: Uri) : SettingsDialog
}

enum class PasswordError { TOO_SHORT, MISMATCH, WRONG_PASSWORD }

/** The screen opens the system file picker for a new backup file. [encrypted] picks the file name. */
data class SaveRequest(val encrypted: Boolean)

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val isBusy: Boolean = false,
    val message: BackupMessage? = null,
    val encryptChoice: EncryptChoice = EncryptChoice.ASK,
    val hasPassword: Boolean = false,
    val dialog: SettingsDialog? = null,
    val passwordError: PasswordError? = null,
    val saveRequest: SaveRequest? = null,
)

class SettingsViewModel(
    private val settings: SettingsStore,
    private val backup: BackupManager,
    private val passwords: BackupPasswordStore,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<BackupMessage?>(null)
    private val dialog = MutableStateFlow<SettingsDialog?>(null)
    private val error = MutableStateFlow<PasswordError?>(null)
    private val saveRequest = MutableStateFlow<SaveRequest?>(null)

    /** Password for the export that waits for the file picker. Kept in memory only, cleared after use. */
    private var pendingPassword: CharArray? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        listOf(settings.theme, busy, message, settings.encryptChoice, passwords.isSet, dialog, error, saveRequest),
    ) { v ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            theme = v[0] as ThemeMode,
            isBusy = v[1] as Boolean,
            message = v[2] as BackupMessage?,
            encryptChoice = v[3] as EncryptChoice,
            hasPassword = v[4] as Boolean,
            dialog = v[5] as SettingsDialog?,
            passwordError = v[6] as PasswordError?,
            saveRequest = v[7] as SaveRequest?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.theme.value))

    fun setTheme(mode: ThemeMode) = settings.setTheme(mode)

    fun setEncryptChoice(choice: EncryptChoice) = settings.setEncryptChoice(choice)

    // ---- Export ----

    /** The Export button. Asks, encrypts or not, by the saved choice. */
    fun onExportClicked() {
        message.value = null
        when (settings.encryptChoice.value) {
            EncryptChoice.ASK -> openDialog(SettingsDialog.AskEncrypt)
            EncryptChoice.ALWAYS -> beginEncryptedExport()
            EncryptChoice.NEVER -> saveRequest.value = SaveRequest(encrypted = false)
        }
    }

    /** The answer to "Encrypt this backup?". [dontAskAgain] makes the answer the new default. */
    fun onEncryptAnswer(encrypt: Boolean, dontAskAgain: Boolean) {
        closeDialog()
        if (dontAskAgain) settings.setEncryptChoice(if (encrypt) EncryptChoice.ALWAYS else EncryptChoice.NEVER)
        if (encrypt) beginEncryptedExport() else saveRequest.value = SaveRequest(encrypted = false)
    }

    private fun beginEncryptedExport() {
        openDialog(if (passwords.isSet.value) SettingsDialog.EnterPassword else SettingsDialog.SetPassword(forExport = true))
    }

    /** The password typed to encrypt this export. Must match the saved password. */
    fun onEnterPassword(password: String) {
        viewModelScope.launch {
            val chars = password.toCharArray()
            if (passwords.verify(chars)) {
                closeDialog()
                pendingPassword = chars
                saveRequest.value = SaveRequest(encrypted = true)
            } else {
                error.value = PasswordError.WRONG_PASSWORD
            }
        }
    }

    /** The screen has opened the file picker. */
    fun onSaveRequestHandled() {
        saveRequest.value = null
    }

    /** The user picked the new file ([uri] null = cancelled). */
    fun export(uri: Uri?) {
        val password = pendingPassword
        pendingPassword = null
        if (uri == null) return
        runBackup {
            val summary = backup.export(uri, password)
            BackupMessage.Exported(summary.people, summary.photos, encrypted = password != null)
        }
    }

    // ---- Import ----

    /** The user picked a file to import. An encrypted file asks for its password first. */
    fun import(uri: Uri?) {
        if (uri == null) return
        message.value = null
        viewModelScope.launch {
            val encrypted = try {
                backup.isEncrypted(uri)
            } catch (_: IOException) {
                message.value = BackupMessage.Failed
                return@launch
            } catch (_: BackupException) {
                message.value = BackupMessage.Failed
                return@launch
            }
            if (encrypted) openDialog(SettingsDialog.ImportPassword(uri)) else importNow(uri, null)
        }
    }

    fun onImportPassword(uri: Uri, password: String) {
        closeDialog()
        viewModelScope.launch { importNow(uri, password.toCharArray()) }
    }

    private suspend fun importNow(uri: Uri, password: CharArray?) = runBackupNow {
        val summary = backup.import(uri, password)
        BackupMessage.Imported(summary.people, summary.photos)
    }

    // ---- Password in settings ----

    fun onSetPasswordClicked() = openDialog(SettingsDialog.SetPassword(forExport = false))

    fun onChangePasswordClicked() = openDialog(SettingsDialog.ChangePassword)

    fun onRemovePasswordClicked() = openDialog(SettingsDialog.RemovePassword)

    fun onForgotPasswordClicked() = openDialog(SettingsDialog.ResetPassword)

    /** Saves a first password. When [forExport] the export goes on with it. */
    fun onSetPassword(password: String, repeat: String, forExport: Boolean) {
        if (!checkNewPassword(password, repeat)) return
        viewModelScope.launch {
            val chars = password.toCharArray()
            passwords.set(chars)
            closeDialog()
            if (forExport) {
                pendingPassword = chars
                saveRequest.value = SaveRequest(encrypted = true)
            } else {
                message.value = BackupMessage.PasswordSet
            }
        }
    }

    fun onChangePassword(old: String, new: String, repeat: String) {
        if (!checkNewPassword(new, repeat)) return
        viewModelScope.launch {
            if (passwords.change(old.toCharArray(), new.toCharArray())) {
                closeDialog()
                message.value = BackupMessage.PasswordChanged
            } else {
                error.value = PasswordError.WRONG_PASSWORD
            }
        }
    }

    fun onRemovePassword(old: String) {
        viewModelScope.launch {
            if (passwords.remove(old.toCharArray())) {
                closeDialog()
                message.value = BackupMessage.PasswordRemoved
            } else {
                error.value = PasswordError.WRONG_PASSWORD
            }
        }
    }

    /** Forgot the password: removes the check value. Backups made with the old password stay locked with it. */
    fun onConfirmReset() {
        passwords.reset()
        closeDialog()
        message.value = BackupMessage.PasswordRemoved
    }

    fun onDismissDialog() {
        closeDialog()
        pendingPassword = null
    }

    private fun checkNewPassword(password: String, repeat: String): Boolean {
        error.value = when {
            password.length < BackupPasswordStore.MIN_LENGTH -> PasswordError.TOO_SHORT
            password != repeat -> PasswordError.MISMATCH
            else -> null
        }
        return error.value == null
    }

    private fun openDialog(value: SettingsDialog) {
        error.value = null
        dialog.value = value
    }

    private fun closeDialog() {
        error.value = null
        dialog.value = null
    }

    private fun runBackup(action: suspend () -> BackupMessage) {
        viewModelScope.launch { runBackupNow(action) }
    }

    private suspend fun runBackupNow(action: suspend () -> BackupMessage) {
        busy.value = true
        message.value = null
        message.value = try {
            action()
        } catch (e: BackupPasswordException) {
            BackupMessage.WrongBackupPassword
        } catch (_: BackupException) {
            BackupMessage.NotABackup
        } catch (_: IOException) {
            BackupMessage.Failed
        }
        busy.value = false
    }
}
