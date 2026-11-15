package com.michalmarcin.namelist.ui.person

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.remember
import com.michalmarcin.namelist.data.db.Tab
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.michalmarcin.namelist.ui.components.Avatar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.ui.AppViewModelFactory

@Composable
fun PersonEditRoute(
    onDone: () -> Unit,
    viewModel: PersonEditViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }

    var pendingCapture by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) viewModel.onPhotoPicked(pendingCapture)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onPhotoPicked(uri)
    }

    PersonEditScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::save,
        onClose = onDone,
        onCamera = {
            val uri = viewModel.newCaptureUri()
            pendingCapture = uri
            try {
                camera.launch(uri)
            } catch (_: ActivityNotFoundException) {
                viewModel.onNoCamera()
            }
        },
        onGallery = {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhoto = viewModel::removePhoto,
        onTabChange = viewModel::onTabChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditScreen(
    state: PersonEditUiState,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onCamera: () -> Unit = {},
    onGallery: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onTabChange: (Long?) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.new_person else R.string.edit_person)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close))
                    }
                },
                actions = {
                    TextButton(shape = MaterialTheme.shapes.large, onClick = onSave, enabled = !state.isLoading, modifier = Modifier.testTag("save")) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PhotoSection(state, onCamera, onGallery, onRemovePhoto)
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nameField"),
                label = { Text(stringResource(R.string.name)) },
                isError = state.showNameError,
                supportingText = if (state.showNameError) {
                    { Text(stringResource(R.string.name_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            TabField(state.tabs, state.tabId, onTabChange)
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .testTag("notesField"),
                label = { Text(stringResource(R.string.notes)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
    }
}

@Composable
private fun PhotoSection(
    state: PersonEditUiState,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Avatar(name = state.name.ifBlank { "?" }, size = 120.dp, photoFile = state.photoFile)
            if (state.isImportingPhoto) CircularProgressIndicator()
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(shape = MaterialTheme.shapes.large, onClick = onCamera, modifier = Modifier.testTag("camera")) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.camera))
            }
            FilledTonalButton(shape = MaterialTheme.shapes.large, onClick = onGallery, modifier = Modifier.testTag("gallery")) {
                Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.gallery))
            }
            if (state.photoFile != null) {
                TextButton(shape = MaterialTheme.shapes.large, onClick = onRemovePhoto, modifier = Modifier.testTag("removePhoto")) {
                    Text(stringResource(R.string.remove_photo))
                }
            }
        }
        state.photoMessage?.let { message ->
            Text(
                text = stringResource(
                    when (message) {
                        PhotoMessage.IMPORT_FAILED -> R.string.photo_import_failed
                        PhotoMessage.NO_CAMERA -> R.string.no_camera
                    },
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabField(tabs: List<Tab>, selectedId: Long?, onChange: (Long?) -> Unit) {
    if (tabs.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val label = tabs.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.no_tab)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.tab)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("tabField"),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.no_tab)) },
                onClick = {
                    onChange(null)
                    expanded = false
                },
            )
            tabs.forEach { tab ->
                DropdownMenuItem(
                    text = { Text(tab.name) },
                    onClick = {
                        onChange(tab.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
