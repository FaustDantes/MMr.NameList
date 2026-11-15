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
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
        viewModel.export(it)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.import(it)
    }
    SettingsScreen(
        state = state,
        versionName = rememberVersionName(),
        onBack = onBack,
        onThemeChange = viewModel::setTheme,
        onExport = { exportLauncher.launch("namelist-backup-${LocalDate.now()}.zip") },
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
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
                            is BackupMessage.Exported, is BackupMessage.Imported -> MaterialTheme.colorScheme.primary
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

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun BackupMessage.text(): String = when (this) {
    is BackupMessage.Exported ->
        pluralStringResource(R.plurals.exported, people, people) + " " + pluralStringResource(R.plurals.photos, photos, photos)
    is BackupMessage.Imported ->
        pluralStringResource(R.plurals.imported, people, people) + " " + pluralStringResource(R.plurals.photos, photos, photos)
    BackupMessage.NotABackup -> stringResource(R.string.not_a_backup)
    BackupMessage.Failed -> stringResource(R.string.backup_failed)
}
