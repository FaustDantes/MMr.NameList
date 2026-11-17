package com.michalmarcin.namelist.ui.person

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import com.michalmarcin.namelist.data.repo.AddResult
import com.michalmarcin.namelist.data.repo.RelationView
import com.michalmarcin.namelist.platform.Coordinates
import com.michalmarcin.namelist.ui.map.WorldMap
import com.michalmarcin.namelist.ui.map.toMarker
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.ui.archive.ArchiveAge
import com.michalmarcin.namelist.ui.archive.text
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.components.Avatar
import com.michalmarcin.namelist.ui.components.hasPhoto
import com.michalmarcin.namelist.ui.components.topHairline

@Composable
fun PersonDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenPerson: (Long) -> Unit,
    onShowOnMap: (Long) -> Unit,
    viewModel: PersonDetailViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    PersonDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = viewModel::delete,
        onOpenMap = { person -> openMap(context, person) },
        onOpenPerson = onOpenPerson,
        onShowOnMap = onShowOnMap,
        onAddRelationship = viewModel::addRelationship,
        onChangeRelationshipType = viewModel::changeRelationshipType,
        onRemoveRelationship = viewModel::removeRelationship,
        onMessageShown = viewModel::clearMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    state: PersonDetailUiState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMap: (Person) -> Unit = {},
    onOpenPerson: (Long) -> Unit = {},
    onShowOnMap: (Long) -> Unit = {},
    onAddRelationship: (Long, Long) -> Unit = { _, _ -> },
    onChangeRelationshipType: (Long, Long) -> Unit = { _, _ -> },
    onRemoveRelationship: (Long) -> Unit = {},
    onMessageShown: () -> Unit = {},
) {
    val person = state.person
    var confirmDelete by remember { mutableStateOf(false) }
    var addingRelation by rememberSaveable { mutableStateOf(false) }
    var changing by remember { mutableStateOf<RelationView?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val duplicateText = stringResource(R.string.relationship_exists)
    LaunchedEffect(state.relationMessage) {
        if (state.relationMessage == AddResult.DUPLICATE) {
            snackbar.showSnackbar(duplicateText)
            onMessageShown()
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    if (person != null) {
                        IconButton(onClick = { onEdit(person.id) }, modifier = Modifier.testTag("edit")) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.edit))
                        }
                        if (!person.isSelf) {
                            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete")) {
                                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (person != null) {
            PersonDetailContent(
                person = person,
                tabName = state.tabNames.joinToString(", ").ifEmpty { null },
                onOpenMap = onOpenMap,
                onShowOnMap = onShowOnMap,
                modifier = Modifier.padding(padding),
            ) {
                RelationshipsSection(
                    relations = state.relations,
                    onOpenPerson = onOpenPerson,
                    onAdd = { addingRelation = true },
                    onChangeType = { changing = it },
                    onRemove = { onRemoveRelationship(it.id) },
                )
            }
        }
    }

    if (addingRelation && person != null) {
        ModalBottomSheet(onDismissRequest = { addingRelation = false }) {
            AddRelationshipContent(
                personName = person.name,
                candidates = state.candidates,
                types = state.relationTypes,
                onAdd = { otherId, type ->
                    addingRelation = false
                    onAddRelationship(otherId, type)
                },
            )
        }
    }

    changing?.let { relation ->
        BasicAlertDialog(onDismissRequest = { changing = null }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                ChangeTypeContent(
                    name = relation.other.name,
                    types = state.relationTypes,
                    current = relation.type,
                    onSelect = { type ->
                        changing = null
                        onChangeRelationshipType(relation.id, type.id)
                    },
                    onCancel = { changing = null },
                )
            }
        }
    }

    if (confirmDelete && person != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title, person.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirmDelete"),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.large, onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PersonDetailContent(
    person: Person,
    tabName: String?,
    onOpenMap: (Person) -> Unit,
    onShowOnMap: (Long) -> Unit,
    modifier: Modifier = Modifier,
    relationships: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // Line design (ADR-038): the photo only when there is one, the name beside it, left aligned.
        Row(verticalAlignment = Alignment.Bottom) {
            if (hasPhoto(person.photoFile)) {
                Avatar(photoFile = person.photoFile, size = 104.dp)
                Spacer(Modifier.width(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = person.name, style = MaterialTheme.typography.headlineMedium)
                if (tabName != null) {
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("tabLabel"),
                    )
                }
            }
        }
        person.askAbout?.let { ask ->
            // F23: what to ask next time, right below the name, under a line in the accent color.
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .topHairline(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 14.dp)
                    .testTag("askAboutBlock"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.ask_about),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(ask, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        // People saved before 1.1 have no stored date; they count from the day they were added.
        (person.firstMetOn ?: person.createdAt.takeIf { it > 0 }?.let { it / 86_400_000L })?.let { day ->
            Text(
                text = stringResource(R.string.first_met_on, formatEpochDay(day)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("firstMetLabel"),
            )
            Spacer(Modifier.height(16.dp))
        }
        person.archivedAt?.let { archivedAt ->
            Text(
                text = stringResource(
                    R.string.archive_status,
                    formatEpochDay(millisToEpochDay(archivedAt)),
                    ArchiveAge.of(archivedAt, System.currentTimeMillis()).text(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("archiveStatus"),
            )
            Spacer(Modifier.height(16.dp))
        }
        val facts = listOfNotNull(
            person.birthDate?.let { R.string.birth_date to formatEpochDay(it) },
            person.nativeLanguage?.let { R.string.native_language to it },
            person.education?.let { R.string.education to it },
            person.currentWork?.let { R.string.current_work to it },
        )
        if (facts.isNotEmpty()) {
            DetailSection(title = stringResource(R.string.more_details), modifier = Modifier.testTag("factsSection")) {
                facts.forEach { (label, value) ->
                    Column {
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        val coordinates = person.coordinates()
        if (person.metCity != null || coordinates != null) {
            DetailSection(title = stringResource(R.string.where_met), modifier = Modifier.testTag("placeSection")) {
                person.metCity?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                if (coordinates != null) {
                    person.toMarker()?.let { marker ->
                        // Small map at the person's place; tap opens the full map there (M9).
                        WorldMap(
                            markers = listOf(marker),
                            focus = marker,
                            focusZoom = 5f,
                            interactive = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onShowOnMap(person.id) }
                                .testTag("miniMap"),
                        )
                    }
                    Text(
                        text = stringResource(R.string.gps_coordinates, coordinates.format()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        shape = MaterialTheme.shapes.large,
                        onClick = { onOpenMap(person) },
                        modifier = Modifier.testTag("openMap"),
                    ) {
                        Icon(painterResource(R.drawable.ic_place), contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.open_in_maps))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        relationships()
        Spacer(Modifier.height(12.dp))
        DetailSection(title = stringResource(R.string.notes)) {
            Text(
                text = person.notes.ifBlank { stringResource(R.string.no_notes) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (person.notes.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        // F19: the time of the last save; people from before 1.1 show the day they were added.
        (person.lastUpdatedAt ?: person.createdAt.takeIf { it > 0 })?.let { millis ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.last_updated, formatDateTime(millis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("lastUpdated"),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A titled block under a thin line (Line design, ADR-038). Used on the detail screen and in Settings. */
@Composable
fun DetailSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .topHairline(MaterialTheme.colorScheme.outlineVariant)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

private fun Person.coordinates(): Coordinates? {
    val lat = metLat
    val lon = metLon
    return if (lat != null && lon != null) Coordinates(lat, lon) else null
}

/** Opens the user's map app at the saved point. The map app, not this app, decides what goes online. */
private fun openMap(context: Context, person: Person) {
    val coordinates = person.coordinates() ?: return
    val point = coordinates.format().replace(" ", "")
    val uri = Uri.parse("geo:$point?q=$point(${Uri.encode(person.name)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_map_app, Toast.LENGTH_SHORT).show()
    }
}
