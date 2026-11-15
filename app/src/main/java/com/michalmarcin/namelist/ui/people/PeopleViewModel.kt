package com.michalmarcin.namelist.ui.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.data.repo.TabsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PeopleUiState(
    val query: String = "",
    val sort: SortMode = SortMode.MANUAL,
    val people: List<Person> = emptyList(),
    val tabs: List<Tab> = emptyList(),
    /** null = "All". */
    val selectedTabId: Long? = null,
    /** Selected people (M8). Only people shown now: a filter change drops hidden ones. */
    val selected: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isSelecting: Boolean get() = selected.isNotEmpty()

    val showEmptyState: Boolean get() = !isLoading && people.isEmpty() && query.isBlank()
    val showNoMatches: Boolean get() = !isLoading && people.isEmpty() && query.isNotBlank()

    /** Drag and drop works only on the manual order of an unfiltered (by search) list, outside selection. */
    val canReorder: Boolean get() = sort == SortMode.MANUAL && query.isBlank() && people.size > 1 && !isSelecting
}

class PeopleViewModel(
    private val people: PeopleRepository,
    tabs: TabsRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val query = handle.getStateFlow(KEY_QUERY, "")
    private val sort = handle.getStateFlow(KEY_SORT, SortMode.MANUAL.name).map { SortMode.valueOf(it) }
    private val selectedTab = handle.getStateFlow(KEY_TAB, NO_TAB)
    private val selectedPeople = handle.getStateFlow(KEY_SELECTED, LongArray(0))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PeopleUiState> =
        combine(query, sort, selectedTab, tabs.observeTabs()) { q, s, selected, tabList ->
            // A deleted tab falls back to "All".
            Selection(q, s, tabList.firstOrNull { it.id == selected }?.id, tabList)
        }.flatMapLatest { sel ->
            combine(people.observePeople(sel.query, sel.sort, sel.tabId), selectedPeople) { list, chosen ->
                val visible = list.map { it.id }.toSet()
                PeopleUiState(
                    query = sel.query,
                    sort = sel.sort,
                    people = list,
                    tabs = sel.tabs,
                    selectedTabId = sel.tabId,
                    // Bulk actions must never touch people the user cannot see.
                    selected = chosen.filter { it in visible }.toSet(),
                    isLoading = false,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

    fun onQueryChange(value: String) {
        handle[KEY_QUERY] = value
    }

    fun onSortChange(value: SortMode) {
        handle[KEY_SORT] = value.name
    }

    fun onSelectTab(id: Long?) {
        handle[KEY_TAB] = id ?: NO_TAB
    }

    fun onToggleSelect(id: Long) {
        // Read the saved selection, not uiState: uiState updates asynchronously, and two quick taps
        // would both see the old selection and lose the first tap.
        val current = (handle.get<LongArray>(KEY_SELECTED) ?: LongArray(0)).toSet()
        setSelected(if (id in current) current - id else current + id)
    }

    fun onSelectAll() = setSelected(uiState.value.people.map { it.id }.toSet())

    fun onClearSelection() = setSelected(emptySet())

    fun onDeleteSelected() {
        val ids = uiState.value.selected
        setSelected(emptySet())
        viewModelScope.launch { people.deleteMany(ids) }
    }

    /** Moves the selected people to the archive (F18). They leave the list at once and can be restored. */
    fun onArchiveSelected() {
        val ids = uiState.value.selected
        setSelected(emptySet())
        viewModelScope.launch { people.archive(ids) }
    }

    fun onMoveSelected(tabId: Long?) {
        val ids = uiState.value.selected
        setSelected(emptySet())
        viewModelScope.launch { people.moveToTab(ids, tabId) }
    }

    private fun setSelected(ids: Set<Long>) {
        handle[KEY_SELECTED] = ids.toLongArray()
    }

    fun onReorder(idsInNewOrder: List<Long>) {
        viewModelScope.launch { people.reorder(idsInNewOrder) }
    }

    private data class Selection(val query: String, val sort: SortMode, val tabId: Long?, val tabs: List<Tab>)

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_SORT = "sort"
        const val KEY_TAB = "tab"
        const val KEY_SELECTED = "selected"
        const val NO_TAB = 0L
    }
}
