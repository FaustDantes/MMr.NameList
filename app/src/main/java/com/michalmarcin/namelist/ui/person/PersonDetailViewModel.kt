package com.michalmarcin.namelist.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.AddResult
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.RelationView
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.ui.nav.PersonRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val person: Person? = null,
    val tabName: String? = null,
    val relations: List<RelationView> = emptyList(),
    /** People this person can be linked to: everyone else, "Me" first. */
    val candidates: List<Person> = emptyList(),
    val relationMessage: AddResult? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class PersonDetailViewModel(
    private val repository: PeopleRepository,
    tabs: TabsRepository,
    private val relationships: RelationshipsRepository,
    handle: SavedStateHandle,
) : ViewModel() {

    private val id = handle.toRoute<PersonRoute>().id
    private val deleted = MutableStateFlow(false)
    private val message = MutableStateFlow<AddResult?>(null)

    private val base = combine(repository.observePerson(id), tabs.observeTabs(), deleted) { person, tabList, isDeleted ->
        PersonDetailUiState(
            person = person,
            tabName = tabList.firstOrNull { it.id == person?.tabId }?.name,
            isLoading = false,
            isDeleted = isDeleted || person == null,
        )
    }

    val uiState: StateFlow<PersonDetailUiState> =
        combine(base, relationships.observeFor(id), repository.observeEveryone(), message) { state, relations, everyone, msg ->
            state.copy(relations = relations, candidates = everyone.filter { it.id != id }, relationMessage = msg)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

    fun addRelationship(otherId: Long, type: RelationType) {
        viewModelScope.launch {
            val result = relationships.add(id, otherId, type)
            message.value = result.takeIf { it != AddResult.ADDED }
        }
    }

    fun changeRelationshipType(relationId: Long, type: RelationType) {
        viewModelScope.launch { relationships.changeType(relationId, viewerId = id, type = type) }
    }

    fun removeRelationship(relationId: Long) {
        viewModelScope.launch { relationships.delete(relationId) }
    }

    fun clearMessage() {
        message.value = null
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(id)
            deleted.value = true
        }
    }
}
