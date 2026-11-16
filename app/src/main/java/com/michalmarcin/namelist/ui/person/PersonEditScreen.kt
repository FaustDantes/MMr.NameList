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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.remember
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.platform.LocationProvider
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
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
    onOpenExisting: (Long) -> Unit = {},
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
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) viewModel.fetchLocation() else viewModel.onLocationDenied()
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
        onTabChange = viewModel::onTabToggle,
        onPlaceChange = viewModel::onPlaceChange,
        onFirstMetChange = viewModel::onFirstMetChange,
        onNativeLanguageChange = viewModel::onNativeLanguageChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onEducationChange = viewModel::onEducationChange,
        onCurrentWorkChange = viewModel::onCurrentWorkChange,
        onAskAboutChange = viewModel::onAskAboutChange,
        onSaveAnyway = viewModel::saveAnyway,
        onDismissDuplicate = viewModel::dismissDuplicate,
        onOpenExisting = onOpenExisting,
        onUseLocation = {
            if (viewModel.hasLocationPermission()) {
                viewModel.fetchLocation()
            } else {
                locationPermission.launch(LocationProvider.PERMISSIONS)
            }
        },
        onClearCoordinates = viewModel::clearCoordinates,
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
    onTabChange: (Long) -> Unit = {},
    onPlaceChange: (String) -> Unit = {},
    onUseLocation: () -> Unit = {},
    onClearCoordinates: () -> Unit = {},
    onFirstMetChange: (Long?) -> Unit = {},
    onNativeLanguageChange: (String) -> Unit = {},
    onBirthDateChange: (Long?) -> Unit = {},
    onEducationChange: (String) -> Unit = {},
    onCurrentWorkChange: (String) -> Unit = {},
    onAskAboutChange: (String) -> Unit = {},
    onSaveAnyway: () -> Unit = {},
    onDismissDuplicate: () -> Unit = {},
    onOpenExisting: (Long) -> Unit = {},
) {
    // F19: only icon, name and tab are on top. The rest waits in a collapsed section.
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    // F16: a new person opens with the cursor in the Name field; Name is the only required field.
    val nameFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    state.duplicate?.let { DuplicateDialog(it, onOpenExisting, onSaveAnyway, onDismissDuplicate) }
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
            // Inside the content: the Scaffold composes it later, so the field exists when the request runs.
            LaunchedEffect(state.isNew) {
                if (state.isNew) {
                    nameFocus.requestFocus()
                    keyboard?.show()
                }
            }
            PhotoSection(state, onCamera, onGallery, onRemovePhoto)
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus)
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
            // F23: the question for the next talk sits right below the name.
            TextDetail(state.askAbout, onAskAboutChange, R.string.ask_about, "askAboutField")
            TabField(state.tabs, state.tabIds, onTabChange)
            DetailsToggle(open = detailsOpen, onToggle = { detailsOpen = !detailsOpen })
            if (detailsOpen) {
                PlaceField(state, onPlaceChange, onUseLocation, onClearCoordinates)
                DateField(
                    epochDay = state.firstMetOn,
                    onChange = onFirstMetChange,
                    label = R.string.first_met,
                    emptyHint = R.string.first_met_today,
                    tag = "firstMet",
                )
                DateField(
                    epochDay = state.birthDate,
                    onChange = onBirthDateChange,
                    label = R.string.birth_date,
                    emptyHint = null,
                    tag = "birth",
                )
                TextDetail(state.nativeLanguage, onNativeLanguageChange, R.string.native_language, "nativeLanguageField")
                TextDetail(state.education, onEducationChange, R.string.education, "educationField")
                TextDetail(state.currentWork, onCurrentWorkChange, R.string.current_work, "currentWorkField")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabField(tabs: List<Tab>, selectedIds: Set<Long>, onToggle: (Long) -> Unit) {
    if (tabs.isEmpty()) return
    // F28: a person can be in many tabs, so the tabs are chips that switch on and off.
    Column(Modifier.fillMaxWidth().testTag("tabField"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.manage_tabs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { tab ->
                FilterChip(
                    selected = tab.id in selectedIds,
                    onClick = { onToggle(tab.id) },
                    label = { Text(tab.name) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("tabChip-${tab.id}"),
                )
            }
        }
    }
}

/** F10: one "City, state" field and a GPS button that stores the current coordinates. */
@Composable
private fun PlaceField(
    state: PersonEditUiState,
    onPlaceChange: (String) -> Unit,
    onUseLocation: () -> Unit,
    onClearCoordinates: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = state.metPlace,
            onValueChange = onPlaceChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("placeField"),
            label = { Text(stringResource(R.string.where_met)) },
            placeholder = { Text(stringResource(R.string.city_state)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            trailingIcon = {
                if (state.isLocating) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onUseLocation, modifier = Modifier.testTag("useLocation")) {
                        Icon(painterResource(R.drawable.ic_my_location), stringResource(R.string.use_current_location))
                    }
                }
            },
        )
        state.coordinates?.let { coordinates ->
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("coordinates"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_place),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.gps_coordinates, coordinates.format()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = onClearCoordinates,
                    modifier = Modifier.testTag("clearCoordinates"),
                ) { Text(stringResource(R.string.clear)) }
            }
        }
        state.locationMessage?.let { message ->
            Text(
                text = stringResource(
                    when (message) {
                        LocationMessage.DENIED -> R.string.location_denied
                        LocationMessage.UNAVAILABLE -> R.string.location_unavailable
                    },
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The header row that opens and closes the "More details" section (F19). */
@Composable
private fun DetailsToggle(open: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .testTag("detailsToggle"),
    ) {
        Text(
            text = stringResource(R.string.more_details),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(if (open) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
            contentDescription = stringResource(if (open) R.string.collapse else R.string.expand),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TextDetail(value: String, onChange: (String) -> Unit, label: Int, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

/**
 * A date with a picker that allows no future days (F16, F19). Empty is allowed; [emptyHint] explains what it means.
 * Test tags: "[tag]Field", "clear[Tag]" and "pick[Tag]" (first letter of [tag] in upper case).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(epochDay: Long?, onChange: (Long?) -> Unit, label: Int, emptyHint: Int?, tag: String) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val suffix = tag.replaceFirstChar { it.uppercase() }
    // A read-only field takes the tap itself, so the picker opens on the release of a press.
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) showPicker = true }
    }
    OutlinedTextField(
        value = epochDay?.let { formatEpochDay(it) }.orEmpty(),
        onValueChange = {},
        readOnly = true,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${tag}Field"),
        label = { Text(stringResource(label)) },
        supportingText = if (epochDay == null && emptyHint != null) {
            { Text(stringResource(emptyHint)) }
        } else {
            null
        },
        trailingIcon = {
            if (epochDay != null) {
                IconButton(onClick = { onChange(null) }, modifier = Modifier.testTag("clear$suffix")) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear))
                }
            } else {
                IconButton(onClick = { showPicker = true }, modifier = Modifier.testTag("pick$suffix")) {
                    Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.pick_date))
                }
            }
        },
    )
    if (showPicker) {
        val todayMillis = epochDayToPickerMillis(todayEpochDay())
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = epochDayToPickerMillis(epochDay ?: todayEpochDay()),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = {
                        pickerState.selectedDateMillis?.let { onChange(pickerMillisToEpochDay(it)) }
                        showPicker = false
                    },
                    modifier = Modifier.testTag("confirmDate"),
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.large, onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}

/** F22: the name exists already. Open that person, save a second one, or go back and change the name. */
@Composable
private fun DuplicateDialog(
    duplicate: DuplicatePerson,
    onOpen: (Long) -> Unit,
    onSaveAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.duplicate_title, duplicate.name)) },
        text = {
            Text(stringResource(if (duplicate.archived) R.string.duplicate_body_archived else R.string.duplicate_body))
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = { onOpen(duplicate.id) },
                    modifier = Modifier.testTag("openExisting"),
                ) { Text(stringResource(R.string.duplicate_open)) }
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = onSaveAnyway,
                    modifier = Modifier.testTag("saveAnyway"),
                ) { Text(stringResource(R.string.duplicate_save_anyway)) }
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = onDismiss,
                    modifier = Modifier.testTag("changeName"),
                ) { Text(stringResource(R.string.duplicate_change_name)) }
            }
        },
    )
}
