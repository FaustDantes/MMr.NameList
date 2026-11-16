package com.michalmarcin.namelist.ui.relationtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.repo.RelationTypesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageRelationTypesUiState(val types: List<RelationKind> = emptyList(), val isLoading: Boolean = true)

/** Settings list of relationship types (F30). */
class ManageRelationTypesViewModel(private val repository: RelationTypesRepository) : ViewModel() {

    val uiState: StateFlow<ManageRelationTypesUiState> = repository.observeTypes()
        .map { ManageRelationTypesUiState(types = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageRelationTypesUiState())

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.add(name) }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun isTaken(name: String, exceptId: Long?): Boolean = repository.isTaken(name, uiState.value.types, exceptId)
}
