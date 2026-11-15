package com.michalmarcin.namelist.ui.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.SortMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PeopleUiState(
    val query: String = "",
    val sort: SortMode = SortMode.MANUAL,
    val people: List<Person> = emptyList(),
    val isLoading: Boolean = true,
) {
    val showEmptyState: Boolean get() = !isLoading && people.isEmpty() && query.isBlank()
    val showNoMatches: Boolean get() = !isLoading && people.isEmpty() && query.isNotBlank()
}

class PeopleViewModel(
    private val repository: PeopleRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val query = handle.getStateFlow(KEY_QUERY, "")
    private val sort = handle.getStateFlow(KEY_SORT, SortMode.MANUAL.name).map { SortMode.valueOf(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PeopleUiState> = combine(query, sort) { q, s -> q to s }
        .flatMapLatest { (q, s) ->
            repository.observePeople(q, s).map { PeopleUiState(query = q, sort = s, people = it, isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

    fun onQueryChange(value: String) {
        handle[KEY_QUERY] = value
    }

    fun onSortChange(value: SortMode) {
        handle[KEY_SORT] = value.name
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_SORT = "sort"
    }
}
