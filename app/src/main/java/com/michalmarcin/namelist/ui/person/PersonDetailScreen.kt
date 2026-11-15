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
import com.michalmarcin.namelist.platform.Coordinates
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
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.components.Avatar

@Composable
fun PersonDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
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
) {
    val person = state.person
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
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
            PersonDetailContent(person, state.tabName, onOpenMap, Modifier.padding(padding))
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Avatar(name = person.name, size = 144.dp, photoFile = person.photoFile)
        Spacer(Modifier.height(16.dp))
        Text(
            text = person.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        if (tabName != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("tabLabel"),
            ) {
                Text(
                    text = tabName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        val coordinates = person.coordinates()
        if (person.metCity != null || coordinates != null) {
            DetailSection(title = stringResource(R.string.where_met), modifier = Modifier.testTag("placeSection")) {
                person.metCity?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                if (coordinates != null) {
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DetailSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
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
