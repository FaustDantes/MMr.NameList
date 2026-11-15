package com.michalmarcin.namelist.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.ui.nav.EditPersonRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonEditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val notes: String = "",
    val showNameError: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
)

class PersonEditViewModel(
    private val repository: PeopleRepository,
    handle: SavedStateHandle,
) : ViewModel() {

    private val id = handle.toRoute<EditPersonRoute>().id
    private val state = MutableStateFlow(PersonEditUiState(isNew = id == 0L, isLoading = id != 0L))
    val uiState: StateFlow<PersonEditUiState> = state.asStateFlow()

    init {
        if (id != 0L) {
            viewModelScope.launch {
                val person = repository.getPerson(id)
                state.update {
                    it.copy(name = person?.name.orEmpty(), notes = person?.notes.orEmpty(), isLoading = false)
                }
            }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value, showNameError = false) }

    fun onNotesChange(value: String) = state.update { it.copy(notes = value) }

    fun save() {
        val current = state.value
        if (current.name.isBlank()) {
            state.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            repository.save(id, current.name, current.notes)
            state.update { it.copy(isSaved = true) }
        }
    }
}
