package com.michalmarcin.namelist.ui.people

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.michalmarcin.namelist.ui.components.EmptyState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PeopleRoute(
    onAddPerson: (tabId: Long?) -> Unit,
    onOpenPerson: (Long) -> Unit,
    onManageTabs: () -> Unit,
    onSettings: () -> Unit,
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
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.people_title)) },
                actions = {
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
            FloatingActionButton(
                onClick = { onAddPerson(state.selectedTabId) },
                modifier = Modifier.testTag("addPerson"),
            ) {
                Icon(painterResource(R.drawable.ic_person_add), contentDescription = stringResource(R.string.add_person))
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
                modifier = Modifier.padding(horizontal = 16.dp),
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
                    onOpenPerson = onOpenPerson,
                    onReorder = onReorder,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabChips(tabs: List<Tab>, selectedTabId: Long?, onSelectTab: (Long?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tabChips"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            TabChip(stringResource(R.string.all), selectedTabId == null) { onSelectTab(null) }
        }
        items(tabs, key = { it.id }) { tab ->
            TabChip(tab.name, selectedTabId == tab.id) { onSelectTab(tab.id) }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = MaterialTheme.shapes.small,
    )
}

/**
 * People list. When [canReorder] is true, a long press starts drag and drop (F5).
 * The new order is kept locally while dragging and saved once, when the finger lifts.
 */
@Composable
private fun PeopleList(
    people: List<Person>,
    canReorder: Boolean,
    onOpenPerson: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember(people) { mutableStateOf(people) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("peopleList"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { person ->
            ReorderableItem(reorderState, key = person.id, enabled = canReorder) { isDragging ->
                PersonRow(
                    person = person,
                    onClick = { onOpenPerson(person.id) },
                    lifted = isDragging,
                    modifier = Modifier.longPressDraggableHandle(
                        enabled = canReorder,
                        onDragStopped = { onReorder(items.map { it.id }) },
                    ),
                )
            }
        }
    }
}

@Composable
fun PersonRow(person: Person, onClick: () -> Unit, modifier: Modifier = Modifier, lifted: Boolean = false) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (lifted) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (lifted) 6.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = person.name, photoFile = person.photoFile)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = person.metCity ?: person.notes.lineSequence().firstOrNull { it.isNotBlank() }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
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
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
