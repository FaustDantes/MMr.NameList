package com.michalmarcin.namelist.ui.relationtypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.person.label
import com.michalmarcin.namelist.ui.tabs.NameDialog

@Composable
fun ManageRelationTypesRoute(
    onBack: () -> Unit,
    viewModel: ManageRelationTypesViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManageRelationTypesScreen(
        state = state,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        isTaken = viewModel::isTaken,
    )
}

private sealed interface TypeDialog {
    data object Add : TypeDialog
    data class Rename(val type: RelationKind, val label: String) : TypeDialog
    data class Delete(val type: RelationKind, val label: String) : TypeDialog
}

/** F30: the relationship types the person picks from. Add, rename and delete; "Other", parent and child stay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRelationTypesScreen(
    state: ManageRelationTypesUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isTaken: (name: String, exceptId: Long?) -> Boolean = { _, _ -> false },
) {
    var dialog by remember { mutableStateOf<TypeDialog?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.relation_types)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = TypeDialog.Add }, modifier = Modifier.testTag("addRelationType")) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add_relation_type))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.types, key = { it.id }) { type ->
                val label = type.label()
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("relationType-${type.id}"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { dialog = TypeDialog.Rename(type, label) },
                            modifier = Modifier.testTag("renameType-${type.id}"),
                        ) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.rename))
                        }
                        if (type.canDelete) {
                            IconButton(
                                onClick = { dialog = TypeDialog.Delete(type, label) },
                                modifier = Modifier.testTag("deleteType-${type.id}"),
                            ) {
                                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete))
                            }
                        } else {
                            // Keeps the rename buttons in one column.
                            Spacer(Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        TypeDialog.Add -> NameDialog(
            title = stringResource(R.string.add_relation_type),
            initial = "",
            fieldLabel = stringResource(R.string.relation_type_name),
            onConfirm = { onAdd(it); dialog = null },
            onDismiss = { dialog = null },
            isTaken = { isTaken(it, null) },
        )
        is TypeDialog.Rename -> NameDialog(
            title = stringResource(R.string.rename),
            initial = current.label,
            fieldLabel = stringResource(R.string.relation_type_name),
            onConfirm = { onRename(current.type.id, it); dialog = null },
            onDismiss = { dialog = null },
            isTaken = { isTaken(it, current.type.id) },
        )
        is TypeDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.delete_title, current.label)) },
            text = { Text(stringResource(R.string.delete_relation_type_body)) },
            confirmButton = {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = { onDelete(current.type.id); dialog = null },
                    modifier = Modifier.testTag("confirmDeleteType"),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.large, onClick = { dialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
        null -> Unit
    }
}
