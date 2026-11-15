package com.michalmarcin.namelist.ui.person

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.PersonDraft
import com.michalmarcin.namelist.platform.Coordinates
import com.michalmarcin.namelist.platform.LocationProvider
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.PhotoStore
import com.michalmarcin.namelist.ui.nav.EditPersonRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A person who already has the typed name (F22). */
data class DuplicatePerson(val id: Long, val name: String, val archived: Boolean)

enum class PhotoMessage { IMPORT_FAILED, NO_CAMERA }

enum class LocationMessage { DENIED, UNAVAILABLE }

data class PersonEditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val notes: String = "",
    val photoFile: String? = null,
    val tabId: Long? = null,
    val tabs: List<Tab> = emptyList(),
    val metPlace: String = "",
    val coordinates: Coordinates? = null,
    /** Epoch day of the first meeting; null = not filled, the repository then uses today (F16). */
    val firstMetOn: Long? = null,
    /** More details (F19). */
    val nativeLanguage: String = "",
    /** Epoch day; null = not filled. */
    val birthDate: Long? = null,
    val education: String = "",
    val currentWork: String = "",
    val askAbout: String = "",
    val isLocating: Boolean = false,
    val locationMessage: LocationMessage? = null,
    val isImportingPhoto: Boolean = false,
    val photoMessage: PhotoMessage? = null,
    val showNameError: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    /** Set when the name already exists; the screen asks what to do. */
    val duplicate: DuplicatePerson? = null,
)

class PersonEditViewModel(
    private val repository: PeopleRepository,
    tabs: TabsRepository,
    private val photos: PhotoStore,
    private val location: LocationProvider,
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

    /** Name as loaded; an unchanged name of an existing person is not checked again. */
    private var loadedName: String? = null

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
                loadedName = person?.name
                state.update {
                    it.copy(
                        name = person?.name.orEmpty(),
                        notes = person?.notes.orEmpty(),
                        photoFile = person?.photoFile,
                        tabId = person?.tabId,
                        metPlace = person?.metCity.orEmpty(),
                        firstMetOn = person?.let(repository::firstMetDay),
                        nativeLanguage = person?.nativeLanguage.orEmpty(),
                        birthDate = person?.birthDate,
                        education = person?.education.orEmpty(),
                        currentWork = person?.currentWork.orEmpty(),
                        askAbout = person?.askAbout.orEmpty(),
                        coordinates = person?.let { p ->
                            if (p.metLat != null && p.metLon != null) Coordinates(p.metLat, p.metLon) else null
                        },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value, showNameError = false) }

    fun onNotesChange(value: String) = state.update { it.copy(notes = value) }

    fun onTabChange(value: Long?) = state.update { it.copy(tabId = value) }

    fun onFirstMetChange(epochDay: Long?) = state.update { it.copy(firstMetOn = epochDay) }

    fun onNativeLanguageChange(value: String) = state.update { it.copy(nativeLanguage = value) }

    fun onBirthDateChange(epochDay: Long?) = state.update { it.copy(birthDate = epochDay) }

    fun onEducationChange(value: String) = state.update { it.copy(education = value) }

    fun onCurrentWorkChange(value: String) = state.update { it.copy(currentWork = value) }

    fun onAskAboutChange(value: String) = state.update { it.copy(askAbout = value) }

    fun onPlaceChange(value: String) = state.update { it.copy(metPlace = value) }

    fun hasLocationPermission(): Boolean = location.hasPermission()

    fun onLocationDenied() = state.update { it.copy(locationMessage = LocationMessage.DENIED, isLocating = false) }

    /** Call after the permission is granted. */
    fun fetchLocation() {
        state.update { it.copy(isLocating = true, locationMessage = null) }
        viewModelScope.launch {
            val found = location.current()
            state.update {
                it.copy(
                    coordinates = found ?: it.coordinates,
                    isLocating = false,
                    locationMessage = if (found == null) LocationMessage.UNAVAILABLE else null,
                )
            }
        }
    }

    fun clearCoordinates() = state.update { it.copy(coordinates = null, locationMessage = null) }

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

    fun save() = saveChecked(checkName = true)

    /** The user chose "Save anyway" in the duplicate dialog. */
    fun saveAnyway() {
        state.update { it.copy(duplicate = null) }
        saveChecked(checkName = false)
    }

    fun dismissDuplicate() = state.update { it.copy(duplicate = null) }

    private fun saveChecked(checkName: Boolean) {
        val current = state.value
        if (current.name.isBlank()) {
            state.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            val nameChanged = loadedName?.let { PeopleRepository.nameKey(it) != PeopleRepository.nameKey(current.name) } ?: true
            if (checkName && nameChanged) {
                val same = repository.findSameName(current.name, exceptId = id)
                if (same != null) {
                    state.update { it.copy(duplicate = DuplicatePerson(same.id, same.name, same.archivedAt != null)) }
                    return@launch
                }
            }
            repository.save(
                id,
                PersonDraft(
                    name = current.name,
                    notes = current.notes,
                    photoFile = current.photoFile,
                    tabId = current.tabId,
                    metPlace = current.metPlace,
                    metLat = current.coordinates?.latitude,
                    metLon = current.coordinates?.longitude,
                    firstMetOn = current.firstMetOn,
                    nativeLanguage = current.nativeLanguage,
                    birthDate = current.birthDate,
                    education = current.education,
                    currentWork = current.currentWork,
                    askAbout = current.askAbout,
                ),
            )
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
