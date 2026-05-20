package com.ilya.safarshots.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.BuildConfig
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.usecase.DeleteGeoPhotoUseCase
import com.ilya.safarshots.domain.usecase.GetGeoPhotosUseCase
import com.ilya.safarshots.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MapFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    WEEK("This week"),
    FAVORITES("Favorites"),
    WITH_NOTES("With notes"),
    NO_LOCATION("No location"),
}

data class MapUiState(
    val photos: List<GeoPhoto> = emptyList(),
    val filter: MapFilter = MapFilter.ALL,
    val selectedPhotoId: String? = null,
    val selectedPhotoIds: Set<String> = emptySet(),
    val mapsConfigured: Boolean = BuildConfig.MAPS_API_KEY.isNotBlank(),
    val message: String? = null,
)

sealed interface MapUiEvent {
    data class FilterChanged(val filter: MapFilter) : MapUiEvent
    data class MarkerClicked(val photoId: String) : MapUiEvent
    data class AddToSelection(val photoId: String) : MapUiEvent
    data class ToggleFavorite(val photoId: String) : MapUiEvent
    data class DeletePhoto(val photoId: String) : MapUiEvent
    data object ClearSelection : MapUiEvent
    data object DismissPhoto : MapUiEvent
    data object MessageShown : MapUiEvent
}

@HiltViewModel
class MapViewModel @Inject constructor(
    getGeoPhotos: GetGeoPhotosUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteGeoPhoto: DeleteGeoPhotoUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        getGeoPhotos()
            .onEach { photos -> _state.update { it.copy(photos = photos) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MapUiEvent) {
        when (event) {
            is MapUiEvent.FilterChanged -> _state.update { it.copy(filter = event.filter) }
            is MapUiEvent.MarkerClicked -> _state.update { it.copy(selectedPhotoId = event.photoId) }
            is MapUiEvent.AddToSelection -> _state.update {
                it.copy(selectedPhotoIds = it.selectedPhotoIds.toggle(event.photoId), selectedPhotoId = null)
            }
            is MapUiEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(event.photoId) }
            is MapUiEvent.DeletePhoto -> viewModelScope.launch {
                deleteGeoPhoto(event.photoId)
                _state.update { it.copy(selectedPhotoId = null, message = "Фото удалено") }
            }
            MapUiEvent.ClearSelection -> _state.update { it.copy(selectedPhotoIds = emptySet()) }
            MapUiEvent.DismissPhoto -> _state.update { it.copy(selectedPhotoId = null) }
            MapUiEvent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun Set<String>.toggle(id: String): Set<String> =
        if (id in this) this - id else this + id
}
