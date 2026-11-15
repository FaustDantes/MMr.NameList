package com.michalmarcin.namelist.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.repo.PeopleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val people: List<Person> = emptyList(),
    val isLoading: Boolean = true,
)

/** People in the archive (F18), the newest first. */
class ArchiveViewModel(private val people: PeopleRepository) : ViewModel() {

    val uiState: StateFlow<ArchiveUiState> = people.observeArchived()
        .map { ArchiveUiState(people = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState())

    fun restore(id: Long) {
        viewModelScope.launch { people.restore(listOf(id)) }
    }

    fun restoreAll() {
        val ids = uiState.value.people.map { it.id }
        viewModelScope.launch { people.restore(ids) }
    }
}
