package com.michalmarcin.namelist.ui.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.TabsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageTabsUiState(val tabs: List<Tab> = emptyList(), val isLoading: Boolean = true)

class ManageTabsViewModel(private val repository: TabsRepository) : ViewModel() {

    val uiState: StateFlow<ManageTabsUiState> = repository.observeTabs()
        .map { ManageTabsUiState(tabs = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageTabsUiState())

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.add(name) }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.rename(id, name) }
    }

    /** F29: a tag name may exist only once, in any case. The form uses this to block a taken name. */
    fun isTaken(name: String, exceptId: Long?): Boolean = TabsRepository.isTaken(name, uiState.value.tabs, exceptId)

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun reorder(idsInNewOrder: List<Long>) {
        viewModelScope.launch { repository.reorder(idsInNewOrder) }
    }
}
