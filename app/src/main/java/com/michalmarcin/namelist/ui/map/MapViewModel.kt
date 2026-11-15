package com.michalmarcin.namelist.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.ui.nav.MapRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MapUiState(
    val markers: List<MapMarker> = emptyList(),
    val focus: MapMarker? = null,
    val isLoading: Boolean = true,
)

/** People with GPS coordinates (F10) on the world map (M9). */
class MapViewModel(people: PeopleRepository, handle: SavedStateHandle) : ViewModel() {

    private val focusId = handle.toRoute<MapRoute>().focusId

    val uiState: StateFlow<MapUiState> = people.observeEveryone()
        .map { everyone ->
            val markers = everyone.mapNotNull { it.toMarker() }
            MapUiState(markers = markers, focus = markers.firstOrNull { it.id == focusId }, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())
}

fun Person.toMarker(): MapMarker? {
    val lat = metLat ?: return null
    val lon = metLon ?: return null
    return if (isSelf) null else MapMarker.of(id, name, lat, lon)
}
