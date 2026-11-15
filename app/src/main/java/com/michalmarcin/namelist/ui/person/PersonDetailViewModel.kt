package com.michalmarcin.namelist.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.ui.nav.PersonRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val person: Person? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class PersonDetailViewModel(
    private val repository: PeopleRepository,
    handle: SavedStateHandle,
) : ViewModel() {

    private val id = handle.toRoute<PersonRoute>().id
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<PersonDetailUiState> =
        combine(repository.observePerson(id), deleted) { person, isDeleted ->
            PersonDetailUiState(person = person, isLoading = false, isDeleted = isDeleted || person == null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

    fun delete() {
        viewModelScope.launch {
            repository.delete(id)
            deleted.value = true
        }
    }
}
