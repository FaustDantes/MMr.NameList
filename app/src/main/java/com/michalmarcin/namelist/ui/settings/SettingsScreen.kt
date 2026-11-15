package com.michalmarcin.namelist.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.platform.EncryptChoice
import com.michalmarcin.namelist.platform.ThemeMode
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.person.DetailSection
import java.time.LocalDate

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        viewModel.export(it)
    }
    // The ViewModel asks for the file picker once the encryption question is settled (F17).
    LaunchedEffect(state.saveRequest) {
        state.saveRequest?.let { request ->
            val extension = if (request.encrypted) "nlbak" else "zip"
            viewModel.onSaveRequestHandled()
            exportLauncher.launch("namelist-backup-${LocalDate.now()}.$extension")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.import(it)
    }
    SettingsScreen(
        state = state,
        versionName = rememberVersionName(),
        onBack = onBack,
        onThemeChange = viewModel::setTheme,
        onExport = viewModel::onExportClicked,
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        actions = SettingsActions(
            onEncryptChoice = viewModel::setEncryptChoice,
            onSetPasswordClicked = viewModel::onSetPasswordClicked,
            onChangePasswordClicked = viewModel::onChangePasswordClicked,
            onRemovePasswordClicked = viewModel::onRemovePasswordClicked,
            onForgotPasswordClicked = viewModel::onForgotPasswordClicked,
            onDismissDialog = viewModel::onDismissDialog,
            onEncryptAnswer = viewModel::onEncryptAnswer,
            onEnterPassword = viewModel::onEnterPassword,
            onSetPassword = viewModel::onSetPassword,
            onChangePassword = viewModel::onChangePassword,
            onRemovePassword = viewModel::onRemovePassword,
            onConfirmReset = viewModel::onConfirmReset,
            onImportPassword = viewModel::onImportPassword,
        ),
    )
}

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    versionName: String,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    actions: SettingsActions = SettingsActions(),
) {
    var confirmImport by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailSection(title = stringResource(R.string.theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.theme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(stringResource(mode.labelRes())) },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("theme-${mode.name}"),
                        )
                    }
                }
            }
            DetailSection(title = stringResource(R.string.backup_password)) {
                Text(
                    stringResource(R.string.backup_password_explained),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(if (state.hasPassword) R.string.password_is_set else R.string.password_not_set),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("passwordStatus"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.hasPassword) {
                        FilledTonalButton(
                            shape = MaterialTheme.shapes.large,
                            onClick = actions.onChangePasswordClicked,
                            modifier = Modifier.testTag("changePassword"),
                        ) { Text(stringResource(R.string.change_password)) }
                        TextButton(
                            shape = MaterialTheme.shapes.large,
                            onClick = actions.onRemovePasswordClicked,
                            modifier = Modifier.testTag("removePassword"),
                        ) { Text(stringResource(R.string.remove_password)) }
                    } else {
                        FilledTonalButton(
                            shape = MaterialTheme.shapes.large,
                            onClick = actions.onSetPasswordClicked,
                            modifier = Modifier.testTag("setPassword"),
                        ) { Text(stringResource(R.string.set_password)) }
                    }
                }
                if (state.hasPassword) {
                    TextButton(
                        shape = MaterialTheme.shapes.large,
                        onClick = actions.onForgotPasswordClicked,
                        modifier = Modifier.testTag("forgotPassword"),
                    ) { Text(stringResource(R.string.forgot_password)) }
                }
                Text(
                    stringResource(R.string.export_encryption),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EncryptChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = state.encryptChoice == choice,
                            onClick = { actions.onEncryptChoice(choice) },
                            label = { Text(stringResource(choice.labelRes())) },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("encrypt-${choice.name}"),
                        )
                    }
                }
            }
            DetailSection(title = stringResource(R.string.backup)) {
                Text(
                    stringResource(R.string.backup_explained),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    FilledTonalButton(
                        shape = MaterialTheme.shapes.large,
                        onClick = onExport,
                        enabled = !state.isBusy,
                        modifier = Modifier.testTag("export"),
                    ) {
                        Icon(painterResource(R.drawable.ic_upload), contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_backup))
                    }
                    FilledTonalButton(
                        shape = MaterialTheme.shapes.large,
                        onClick = { confirmImport = true },
                        enabled = !state.isBusy,
                        modifier = Modifier.testTag("import"),
                    ) {
                        Icon(painterResource(R.drawable.ic_download), contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.import_backup))
                    }
                }
                if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                state.message?.let { message ->
                    Text(
                        text = message.text(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (message) {
                            is BackupMessage.Exported, is BackupMessage.Imported, BackupMessage.PasswordSet,
                            BackupMessage.PasswordChanged, BackupMessage.PasswordRemoved,
                            -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("backupMessage"),
                    )
                }
            }
            DetailSection(title = stringResource(R.string.about)) {
                Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.about_privacy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    SettingsDialogs(state, actions)

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.import_title)) },
            text = { Text(stringResource(R.string.import_warning)) },
            confirmButton = {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = {
                        confirmImport = false
                        onImport()
                    },
                    modifier = Modifier.testTag("confirmImport"),
                ) { Text(stringResource(R.string.import_backup)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.large, onClick = { confirmImport = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun EncryptChoice.labelRes(): Int = when (this) {
    EncryptChoice.ASK -> R.string.encrypt_ask
    EncryptChoice.ALWAYS -> R.string.encrypt_always
    EncryptChoice.NEVER -> R.string.encrypt_never
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun BackupMessage.text(): String = when (this) {
    is BackupMessage.Exported ->
        pluralStringResource(R.plurals.exported, people, people) + " " + pluralStringResource(R.plurals.photos, photos, photos) +
            if (encrypted) " " + stringResource(R.string.backup_encrypted_suffix) else ""
    is BackupMessage.Imported ->
        pluralStringResource(R.plurals.imported, people, people) + " " + pluralStringResource(R.plurals.photos, photos, photos)
    BackupMessage.NotABackup -> stringResource(R.string.not_a_backup)
    BackupMessage.Failed -> stringResource(R.string.backup_failed)
    BackupMessage.WrongBackupPassword -> stringResource(R.string.wrong_backup_password)
    BackupMessage.PasswordSet -> stringResource(R.string.password_saved)
    BackupMessage.PasswordChanged -> stringResource(R.string.password_changed)
    BackupMessage.PasswordRemoved -> stringResource(R.string.password_removed)
}
