package com.michalmarcin.namelist.ui.people

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import com.michalmarcin.namelist.ui.person.sparkPreview
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.components.Avatar
import com.michalmarcin.namelist.ui.components.hasPhoto
import com.michalmarcin.namelist.ui.components.lineFieldColors
import com.michalmarcin.namelist.ui.components.topHairline
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.michalmarcin.namelist.ui.components.EmptyState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PeopleRoute(
    onAddPerson: (tabId: Long?) -> Unit,
    onOpenPerson: (Long) -> Unit,
    onManageTabs: () -> Unit,
    onSettings: () -> Unit,
    onMap: () -> Unit,
    onArchive: () -> Unit,
    viewModel: PeopleViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PeopleScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onAddPerson = onAddPerson,
        onOpenPerson = onOpenPerson,
        onSelectTab = viewModel::onSelectTab,
        onReorder = viewModel::onReorder,
        onManageTabs = onManageTabs,
        onSettings = onSettings,
        onMap = onMap,
        onArchive = onArchive,
        onArchiveSelected = viewModel::onArchiveSelected,
        onToggleSelect = viewModel::onToggleSelect,
        onSelectAll = viewModel::onSelectAll,
        onClearSelection = viewModel::onClearSelection,
        onDeleteSelected = viewModel::onDeleteSelected,
        onMoveSelected = viewModel::onMoveSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    state: PeopleUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onAddPerson: (tabId: Long?) -> Unit,
    onOpenPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSelectTab: (Long?) -> Unit = {},
    onReorder: (List<Long>) -> Unit = {},
    onManageTabs: () -> Unit = {},
    onSettings: () -> Unit = {},
    onMap: () -> Unit = {},
    onArchive: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    onToggleSelect: (Long) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onMoveSelected: (Long?) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var dialog by remember { mutableStateOf<BulkDialog?>(null) }
    BackHandler(enabled = state.isSelecting) { onClearSelection() }
    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (state.isSelecting) {
                SelectionBar(
                    count = state.selected.size,
                    canMove = state.tabs.isNotEmpty(),
                    onClear = onClearSelection,
                    onSelectAll = onSelectAll,
                    onMove = { dialog = BulkDialog.Move },
                    onArchive = onArchiveSelected,
                    onDelete = { dialog = BulkDialog.Delete },
                )
            } else LargeTopAppBar(
                title = { Text(stringResource(R.string.people_title)) },
                actions = {
                    IconButton(onClick = onMap, modifier = Modifier.testTag("map")) {
                        Icon(painterResource(R.drawable.ic_map), contentDescription = stringResource(R.string.map))
                    }
                    IconButton(onClick = onArchive, modifier = Modifier.testTag("archive")) {
                        Icon(painterResource(R.drawable.ic_archive), contentDescription = stringResource(R.string.archive))
                    }
                    IconButton(onClick = onManageTabs, modifier = Modifier.testTag("manageTabs")) {
                        Icon(painterResource(R.drawable.ic_folder), contentDescription = stringResource(R.string.manage_tabs))
                    }
                    SortMenu(state.sort, onSortChange)
                    IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!state.isSelecting) {
                // F21, Line design (ADR-038): a 64 dp square in the accent color at the bottom right.
                FloatingActionButton(
                    onClick = { onAddPerson(state.selectedTabId) },
                    modifier = Modifier.size(AddButtonSize).testTag("addPerson"),
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_person),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (state.tabs.isNotEmpty() && state.query.isBlank()) {
                TabChips(state.tabs, state.selectedTabId, onSelectTab)
            }
            when {
                state.showEmptyState -> EmptyState(
                    icon = R.drawable.ic_group,
                    title = stringResource(R.string.empty_title),
                    body = stringResource(R.string.empty_body),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                state.showNoMatches -> EmptyState(
                    icon = R.drawable.ic_search,
                    title = stringResource(R.string.no_matches_title),
                    body = stringResource(R.string.no_matches_body, state.query.trim()),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                else -> PeopleList(
                    people = state.people,
                    canReorder = state.canReorder,
                    selected = state.selected,
                    onOpenPerson = onOpenPerson,
                    onToggleSelect = onToggleSelect,
                    onReorder = onReorder,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    when (dialog) {
        BulkDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = {
                Text(pluralStringResource(R.plurals.delete_people_title, state.selected.size, state.selected.size))
            },
            text = { Text(stringResource(R.string.delete_people_body)) },
            confirmButton = {
                TextButton(
                    shape = MaterialTheme.shapes.large,
                    onClick = {
                        dialog = null
                        onDeleteSelected()
                    },
                    modifier = Modifier.testTag("confirmBulkDelete"),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(shape = MaterialTheme.shapes.large, onClick = { dialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
        BulkDialog.Move -> BasicAlertDialog(onDismissRequest = { dialog = null }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                MoveToTabContent(
                    count = state.selected.size,
                    tabs = state.tabs,
                    onPick = { tabId ->
                        dialog = null
                        onMoveSelected(tabId)
                    },
                    onCancel = { dialog = null },
                )
            }
        }
        null -> Unit
    }
}

private enum class BulkDialog { Delete, Move }

/** Top bar while people are selected (M8). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(
    count: Int,
    canMove: Boolean,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onMove: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                pluralStringResource(R.plurals.selected_count, count, count),
                modifier = Modifier.testTag("selectionCount"),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClear, modifier = Modifier.testTag("clearSelection")) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear_selection))
            }
        },
        actions = {
            IconButton(onClick = onSelectAll, modifier = Modifier.testTag("selectAll")) {
                Icon(painterResource(R.drawable.ic_select_all), stringResource(R.string.select_all))
            }
            if (canMove) {
                IconButton(onClick = onMove, modifier = Modifier.testTag("bulkMove")) {
                    Icon(painterResource(R.drawable.ic_folder), stringResource(R.string.move_to_tab))
                }
            }
            IconButton(onClick = onArchive, modifier = Modifier.testTag("bulkArchive")) {
                Icon(painterResource(R.drawable.ic_archive), stringResource(R.string.move_to_archive))
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("bulkDelete")) {
                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    )
}

/** Content of the "Move to tab" dialog. Separate from the dialog window so tests can drive it. */
@Composable
fun MoveToTabContent(count: Int, tabs: List<Tab>, onPick: (Long?) -> Unit, onCancel: () -> Unit) {
    Column(Modifier.padding(vertical = 24.dp)) {
        Text(
            pluralStringResource(R.plurals.move_title, count, count),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        MoveOption(stringResource(R.string.no_tab), Modifier.testTag("moveTo-none")) { onPick(null) }
        tabs.forEach { tab ->
            MoveOption(tab.name, Modifier.testTag("moveTo-${tab.id}")) { onPick(tab.id) }
        }
        TextButton(
            shape = MaterialTheme.shapes.large,
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp, top = 8.dp),
        ) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun MoveOption(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

@Composable
private fun TabChips(tabs: List<Tab>, selectedTabId: Long?, onSelectTab: (Long?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tabChips"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item(key = "all") {
            TabChip(stringResource(R.string.all), selectedTabId == null) { onSelectTab(null) }
        }
        items(tabs, key = { it.id }) { tab ->
            TabChip(tab.name, selectedTabId == tab.id) { onSelectTab(tab.id) }
        }
    }
}

/** Line design (ADR-038): a tag is plain text; the selected one is underlined in the accent color. */
@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            modifier = Modifier
                .drawBehind {
                    if (selected) {
                        val line = 2.dp.toPx()
                        drawRect(colors.primary, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                    }
                }
                .padding(vertical = 6.dp),
        )
    }
}

/**
 * People list.
 * - Tap opens a person; while selecting, tap adds or removes the person (M8).
 * - Long press starts selecting (ADR-024).
 * - In manual order, the handle on the right drags a person to a new place (F5). The new order is kept
 *   locally while dragging and saved once, when the finger lifts.
 */
@Composable
private fun PeopleList(
    people: List<Person>,
    canReorder: Boolean,
    selected: Set<Long>,
    onOpenPerson: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember(people) { mutableStateOf(people) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }
    val haptics = LocalHapticFeedback.current
    val selecting = selected.isNotEmpty()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("peopleList"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp),
    ) {
        items(items, key = { it.id }) { person ->
            ReorderableItem(reorderState, key = person.id, enabled = canReorder) { isDragging ->
                PersonRow(
                    person = person,
                    isSelected = person.id in selected,
                    isSelecting = selecting,
                    onClick = { if (selecting) onToggleSelect(person.id) else onOpenPerson(person.id) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSelect(person.id)
                    },
                    lifted = isDragging,
                    trailing = if (canReorder) {
                        {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(onDragStopped = { onReorder(items.map { it.id }) }),
                            ) {
                                Icon(painterResource(R.drawable.ic_drag_handle), stringResource(R.string.drag_to_reorder))
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
fun PersonRow(
    person: Person,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    isSelecting: Boolean = false,
    lifted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val selectedLabel = stringResource(R.string.selected)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                if (isSelecting) {
                    selected = isSelected
                    if (isSelected) stateDescription = selectedLabel
                }
            }
            .testTag("person-${person.id}"),
        color = when {
            isSelected -> colors.secondaryContainer
            lifted -> colors.surfaceContainerHighest
            else -> colors.background
        },
        contentColor = if (isSelected) colors.onSecondaryContainer else colors.onSurface,
        shadowElevation = if (lifted) 6.dp else 0.dp,
    ) {
        // Line design (ADR-038): no card; a hairline above each row, the photo only when there is one.
        Row(
            modifier = Modifier
                .topHairline(colors.outlineVariant)
                .heightIn(min = 64.dp)
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelecting) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .then(
                            if (isSelected) {
                                Modifier.background(colors.primary, MaterialTheme.shapes.small)
                            } else {
                                Modifier.border(1.5.dp, colors.outline, MaterialTheme.shapes.small)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
            }
            if (hasPhoto(person.photoFile)) {
                Avatar(photoFile = person.photoFile)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // F26: the Conversation Spark comes first, as a short preview under the name.
                val spark = sparkPreview(person.askAbout)
                val subtitle = spark ?: person.metCity ?: person.notes.lineSequence().firstOrNull { it.isNotBlank() }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.testTag(if (spark != null) "spark-${person.id}" else "subtitle-${person.id}"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke() ?: Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun SortMenu(sort: SortMode, onSortChange: (SortMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("sortMenu")) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(if (mode == SortMode.MANUAL) R.string.sort_manual else R.string.sort_a_to_z))
                    },
                    trailingIcon = {
                        if (mode == sort) Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                    },
                    onClick = {
                        open = false
                        onSortChange(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    // Line design (ADR-038): a search line, no box.
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("search"),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        singleLine = true,
        shape = RectangleShape,
        colors = lineFieldColors(),
    )
}

/** The add button on the people list: 64 dp, larger than the Material default of 56 dp (F21, ADR-038). */
internal val AddButtonSize = 64.dp
