package com.michalmarcin.namelist.ui.settings

import com.michalmarcin.namelist.ui.components.lineFieldColors
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.TextField
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.platform.BackupPasswordStore

/** Everything the settings dialogs can ask the ViewModel to do (F17). All no-ops by default for tests. */
data class SettingsActions(
    val onEncryptChoice: (com.michalmarcin.namelist.platform.EncryptChoice) -> Unit = {},
    val onSetPasswordClicked: () -> Unit = {},
    val onChangePasswordClicked: () -> Unit = {},
    val onRemovePasswordClicked: () -> Unit = {},
    val onForgotPasswordClicked: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onEncryptAnswer: (encrypt: Boolean, dontAskAgain: Boolean) -> Unit = { _, _ -> },
    val onEnterPassword: (String) -> Unit = {},
    val onSetPassword: (password: String, repeat: String, forExport: Boolean) -> Unit = { _, _, _ -> },
    val onChangePassword: (old: String, new: String, repeat: String) -> Unit = { _, _, _ -> },
    val onRemovePassword: (String) -> Unit = {},
    val onConfirmReset: () -> Unit = {},
    val onImportPassword: (Uri, String) -> Unit = { _, _ -> },
)

/** Shows the dialog that [SettingsUiState.dialog] asks for. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialogs(state: SettingsUiState, actions: SettingsActions) {
    val dialog = state.dialog ?: return
    BasicAlertDialog(onDismissRequest = actions.onDismissDialog) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            when (dialog) {
                SettingsDialog.AskEncrypt -> EncryptQuestionContent(actions.onEncryptAnswer, actions.onDismissDialog)
                is SettingsDialog.SetPassword -> PasswordFormContent(
                    title = stringResource(R.string.set_password),
                    fields = listOf(R.string.new_password, R.string.repeat_password),
                    error = state.passwordError,
                    confirmLabel = stringResource(R.string.save),
                    onConfirm = { actions.onSetPassword(it[0], it[1], dialog.forExport) },
                    onCancel = actions.onDismissDialog,
                    hint = stringResource(R.string.password_lost_warning),
                )
                SettingsDialog.EnterPassword -> PasswordFormContent(
                    title = stringResource(R.string.enter_backup_password),
                    fields = listOf(R.string.password),
                    error = state.passwordError,
                    confirmLabel = stringResource(R.string.encrypt),
                    onConfirm = { actions.onEnterPassword(it[0]) },
                    onCancel = actions.onDismissDialog,
                )
                SettingsDialog.ChangePassword -> PasswordFormContent(
                    title = stringResource(R.string.change_password),
                    fields = listOf(R.string.old_password, R.string.new_password, R.string.repeat_password),
                    error = state.passwordError,
                    confirmLabel = stringResource(R.string.save),
                    onConfirm = { actions.onChangePassword(it[0], it[1], it[2]) },
                    onCancel = actions.onDismissDialog,
                    hint = stringResource(R.string.change_password_note),
                )
                SettingsDialog.RemovePassword -> PasswordFormContent(
                    title = stringResource(R.string.remove_password),
                    fields = listOf(R.string.old_password),
                    error = state.passwordError,
                    confirmLabel = stringResource(R.string.remove),
                    onConfirm = { actions.onRemovePassword(it[0]) },
                    onCancel = actions.onDismissDialog,
                )
                SettingsDialog.ResetPassword -> ConfirmContent(
                    title = stringResource(R.string.reset_password_title),
                    body = stringResource(R.string.reset_password_body),
                    confirmLabel = stringResource(R.string.remove),
                    onConfirm = actions.onConfirmReset,
                    onCancel = actions.onDismissDialog,
                )
                is SettingsDialog.ImportPassword -> PasswordFormContent(
                    title = stringResource(R.string.import_password_title),
                    fields = listOf(R.string.password),
                    error = state.passwordError,
                    confirmLabel = stringResource(R.string.import_backup),
                    onConfirm = { actions.onImportPassword(dialog.uri, it[0]) },
                    onCancel = actions.onDismissDialog,
                    hint = stringResource(R.string.import_password_body),
                )
            }
        }
    }
}

/** "Encrypt this backup?" with the "Do not show this again" checkbox (F17). Separate from the dialog window for tests. */
@Composable
fun EncryptQuestionContent(onAnswer: (encrypt: Boolean, dontAskAgain: Boolean) -> Unit, onCancel: () -> Unit) {
    var dontAskAgain by remember { mutableStateOf(false) }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.encrypt_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.encrypt_body), style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = dontAskAgain, role = Role.Checkbox, onValueChange = { dontAskAgain = it })
                .testTag("dontAskAgain"),
        ) {
            Checkbox(checked = dontAskAgain, onCheckedChange = null)
            Text(stringResource(R.string.dont_show_again), modifier = Modifier.padding(start = 8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            TextButton(shape = MaterialTheme.shapes.large, onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            TextButton(
                shape = MaterialTheme.shapes.large,
                onClick = { onAnswer(false, dontAskAgain) },
                modifier = Modifier.testTag("encryptNo"),
            ) { Text(stringResource(R.string.encrypt_no)) }
            FilledTonalButton(
                shape = MaterialTheme.shapes.large,
                onClick = { onAnswer(true, dontAskAgain) },
                modifier = Modifier.testTag("encryptYes"),
            ) { Text(stringResource(R.string.encrypt)) }
        }
    }
}

/** One to three password fields and a confirm button. Field i has the test tag "password-i". */
@Composable
fun PasswordFormContent(
    title: String,
    fields: List<Int>,
    error: PasswordError?,
    confirmLabel: String,
    onConfirm: (List<String>) -> Unit,
    onCancel: () -> Unit,
    hint: String? = null,
) {
    val values = remember(fields) { mutableStateOf(List(fields.size) { "" }) }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        fields.forEachIndexed { index, label ->
            TextField(
                colors = lineFieldColors(),
                shape = RectangleShape,
                value = values.value[index],
                onValueChange = { text -> values.value = values.value.toMutableList().also { it[index] = text } },
                label = { Text(stringResource(label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password-$index"),
            )
        }
        error?.let {
            Text(
                text = stringResource(
                    when (it) {
                        PasswordError.TOO_SHORT -> R.string.password_too_short
                        PasswordError.MISMATCH -> R.string.password_mismatch
                        PasswordError.WRONG_PASSWORD -> R.string.password_wrong
                    },
                    BackupPasswordStore.MIN_LENGTH,
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("passwordError"),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            TextButton(shape = MaterialTheme.shapes.large, onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            FilledTonalButton(
                shape = MaterialTheme.shapes.large,
                onClick = { onConfirm(values.value) },
                modifier = Modifier.testTag("confirmPassword"),
            ) { Text(confirmLabel) }
        }
    }
}

@Composable
private fun ConfirmContent(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            TextButton(shape = MaterialTheme.shapes.large, onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            FilledTonalButton(
                shape = MaterialTheme.shapes.large,
                onClick = onConfirm,
                modifier = Modifier.testTag("confirmReset"),
            ) { Text(confirmLabel) }
        }
    }
}
