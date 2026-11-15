package com.michalmarcin.namelist.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.platform.BackupException
import com.michalmarcin.namelist.platform.BackupManager
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
    data class Exported(val people: Int, val photos: Int) : BackupMessage
    data class Imported(val people: Int, val photos: Int) : BackupMessage
    data object NotABackup : BackupMessage
    data object Failed : BackupMessage
}

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val isBusy: Boolean = false,
    val message: BackupMessage? = null,
)

class SettingsViewModel(
    private val settings: SettingsStore,
    private val backup: BackupManager,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<BackupMessage?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(settings.theme, busy, message) { theme, isBusy, msg ->
        SettingsUiState(theme, isBusy, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.theme.value))

    fun setTheme(mode: ThemeMode) = settings.setTheme(mode)

    fun export(uri: Uri?) = run(uri) {
        val summary = backup.export(it)
        BackupMessage.Exported(summary.people, summary.photos)
    }

    fun import(uri: Uri?) = run(uri) {
        val summary = backup.import(it)
        BackupMessage.Imported(summary.people, summary.photos)
    }

    private fun run(uri: Uri?, action: suspend (Uri) -> BackupMessage) {
        if (uri == null) return
        busy.value = true
        message.value = null
        viewModelScope.launch {
            message.value = try {
                action(uri)
            } catch (_: BackupException) {
                BackupMessage.NotABackup
            } catch (_: IOException) {
                BackupMessage.Failed
            }
            busy.value = false
        }
    }
}
