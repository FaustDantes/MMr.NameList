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
    val isLoading: Boolean = true,
) {
    val showEmptyState: Boolean get() = !isLoading && people.isEmpty() && query.isBlank()
    val showNoMatches: Boolean get() = !isLoading && people.isEmpty() && query.isNotBlank()

    /** Drag and drop works only on the manual order of an unfiltered (by search) list. */
    val canReorder: Boolean get() = sort == SortMode.MANUAL && query.isBlank() && people.size > 1
}

class PeopleViewModel(
    private val people: PeopleRepository,
    tabs: TabsRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val query = handle.getStateFlow(KEY_QUERY, "")
    private val sort = handle.getStateFlow(KEY_SORT, SortMode.MANUAL.name).map { SortMode.valueOf(it) }
    private val selectedTab = handle.getStateFlow(KEY_TAB, NO_TAB)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PeopleUiState> =
        combine(query, sort, selectedTab, tabs.observeTabs()) { q, s, selected, tabList ->
            // A deleted tab falls back to "All".
            Selection(q, s, tabList.firstOrNull { it.id == selected }?.id, tabList)
        }.flatMapLatest { sel ->
            people.observePeople(sel.query, sel.sort, sel.tabId).map { list ->
                PeopleUiState(
                    query = sel.query,
                    sort = sel.sort,
                    people = list,
                    tabs = sel.tabs,
                    selectedTabId = sel.tabId,
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

    fun onReorder(idsInNewOrder: List<Long>) {
        viewModelScope.launch { people.reorder(idsInNewOrder) }
    }

    private data class Selection(val query: String, val sort: SortMode, val tabId: Long?, val tabs: List<Tab>)

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_SORT = "sort"
        const val KEY_TAB = "tab"
        const val NO_TAB = 0L
    }
}
