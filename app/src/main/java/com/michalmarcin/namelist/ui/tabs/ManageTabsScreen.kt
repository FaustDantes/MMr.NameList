package com.michalmarcin.namelist.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.components.EmptyState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ManageTabsRoute(
    onBack: () -> Unit,
    viewModel: ManageTabsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManageTabsScreen(
        state = state,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        onReorder = viewModel::reorder,
    )
}

/** Dialog shown on the screen. Kept as one value so only one dialog can be open. */
private sealed interface TabDialog {
    data object Add : TabDialog
    data class Rename(val tab: Tab) : TabDialog
    data class Delete(val tab: Tab) : TabDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTabsScreen(
    state: ManageTabsUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<TabDialog?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_tabs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = TabDialog.Add },
                modifier = Modifier.testTag("addTab"),
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.add_tab)) },
            )
        },
    ) { padding ->
        if (!state.isLoading && state.tabs.isEmpty()) {
            EmptyState(
                icon = R.drawable.ic_folder,
                title = stringResource(R.string.tabs_empty_title),
                body = stringResource(R.string.tabs_empty_body),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
        } else {
            TabList(
                tabs = state.tabs,
                onRename = { dialog = TabDialog.Rename(it) },
                onDelete = { dialog = TabDialog.Delete(it) },
                onReorder = onReorder,
                modifier = Modifier.padding(padding),
            )
        }
    }

    when (val current = dialog) {
        TabDialog.Add -> NameDialog(
            title = stringResource(R.string.add_tab),
            initial = "",
            onConfirm = { onAdd(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is TabDialog.Rename -> NameDialog(
            title = stringResource(R.string.rename_tab),
            initial = current.tab.name,
            onConfirm = { onRename(current.tab.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is TabDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.delete_title, current.tab.name)) },
            text = { Text(stringResource(R.string.delete_tab_body)) },
            confirmButton = {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = { onDelete(current.tab.id); dialog = null },
                    modifier = Modifier.testTag("confirmDeleteTab"),
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

@Composable
private fun TabList(
    tabs: List<Tab>,
    onRename: (Tab) -> Unit,
    onDelete: (Tab) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember(tabs) { mutableStateOf(tabs) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { tab ->
            ReorderableItem(reorderState, key = tab.id) { isDragging ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shadowElevation = if (isDragging) 6.dp else 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 64.dp)
                            .padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(onDragStopped = { onReorder(items.map { it.id }) }),
                        ) {
                            Icon(painterResource(R.drawable.ic_drag_handle), stringResource(R.string.drag_to_reorder))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = tab.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRename(tab) }, modifier = Modifier.testTag("rename-${tab.id}")) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.rename_tab))
                        }
                        IconButton(onClick = { onDelete(tab) }, modifier = Modifier.testTag("delete-${tab.id}")) {
                            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.tab_name)) },
                singleLine = true,
                modifier = Modifier.testTag("tabNameField"),
            )
        },
        confirmButton = {
            TextButton(
                shape = MaterialTheme.shapes.large,
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirmTabName"),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(shape = MaterialTheme.shapes.large, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
