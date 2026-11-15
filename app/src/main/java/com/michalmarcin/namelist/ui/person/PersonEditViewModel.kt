package com.michalmarcin.namelist.ui.person

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.PhotoStore
import com.michalmarcin.namelist.ui.nav.EditPersonRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PhotoMessage { IMPORT_FAILED, NO_CAMERA }

data class PersonEditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val notes: String = "",
    val photoFile: String? = null,
    val tabId: Long? = null,
    val tabs: List<Tab> = emptyList(),
    val isImportingPhoto: Boolean = false,
    val photoMessage: PhotoMessage? = null,
    val showNameError: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
)

class PersonEditViewModel(
    private val repository: PeopleRepository,
    tabs: TabsRepository,
    private val photos: PhotoStore,
    handle: SavedStateHandle,
) : ViewModel() {

    private val route = handle.toRoute<EditPersonRoute>()
    private val id = route.id
    private val state = MutableStateFlow(
        PersonEditUiState(isNew = id == 0L, isLoading = id != 0L, tabId = route.tabId.takeIf { it != 0L }),
    )
    val uiState: StateFlow<PersonEditUiState> = state.asStateFlow()

    /** Photos imported in this edit session. Deleted again if the user does not save. */
    private val sessionPhotos = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            tabs.observeTabs().collect { list ->
                state.update { current ->
                    // A tab deleted meanwhile is cleared.
                    current.copy(tabs = list, tabId = current.tabId?.takeIf { id -> list.any { it.id == id } })
                }
            }
        }
        if (id != 0L) {
            viewModelScope.launch {
                val person = repository.getPerson(id)
                state.update {
                    it.copy(
                        name = person?.name.orEmpty(),
                        notes = person?.notes.orEmpty(),
                        photoFile = person?.photoFile,
                        tabId = person?.tabId,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value, showNameError = false) }

    fun onNotesChange(value: String) = state.update { it.copy(notes = value) }

    fun onTabChange(value: Long?) = state.update { it.copy(tabId = value) }

    fun newCaptureUri(): Uri = photos.newCaptureUri()

    fun onNoCamera() = state.update { it.copy(photoMessage = PhotoMessage.NO_CAMERA) }

    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        state.update { it.copy(isImportingPhoto = true, photoMessage = null) }
        viewModelScope.launch {
            val imported = runCatching { photos.import(uri) }.getOrNull()
            if (imported == null) {
                state.update { it.copy(isImportingPhoto = false, photoMessage = PhotoMessage.IMPORT_FAILED) }
                return@launch
            }
            dropSessionPhoto(state.value.photoFile)
            sessionPhotos += imported
            state.update { it.copy(photoFile = imported, isImportingPhoto = false) }
        }
    }

    fun removePhoto() {
        dropSessionPhoto(state.value.photoFile)
        state.update { it.copy(photoFile = null, photoMessage = null) }
    }

    fun save() {
        val current = state.value
        if (current.name.isBlank()) {
            state.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            repository.save(id, current.name, current.notes, current.photoFile, current.tabId)
            sessionPhotos.clear()
            state.update { it.copy(isSaved = true) }
        }
    }

    override fun onCleared() {
        // The user left without saving: remove photos that nothing points to.
        sessionPhotos.forEach(photos::delete)
        sessionPhotos.clear()
    }

    private fun dropSessionPhoto(name: String?) {
        if (name != null && sessionPhotos.remove(name)) photos.delete(name)
    }
}
