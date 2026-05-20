package com.ilya.safarshots.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.domain.model.GeoPhoto
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

enum class GalleryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    WITH_LOCATION("With location"),
    WITHOUT_LOCATION("No location"),
}

data class GalleryUiState(
    val photos: List<GeoPhoto> = emptyList(),
    val filter: GalleryFilter = GalleryFilter.ALL,
    val selectedIds: Set<String> = emptySet(),
) {
    val selectionMode: Boolean = selectedIds.isNotEmpty()
}

sealed interface GalleryUiEvent {
    data class FilterChanged(val filter: GalleryFilter) : GalleryUiEvent
    data class ToggleSelection(val photoId: String) : GalleryUiEvent
    data class ToggleFavorite(val photoId: String) : GalleryUiEvent
    data object ClearSelection : GalleryUiEvent
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    getGeoPhotos: GetGeoPhotosUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    init {
        getGeoPhotos()
            .onEach { photos -> _state.update { it.copy(photos = photos) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GalleryUiEvent) {
        when (event) {
            is GalleryUiEvent.FilterChanged -> _state.update { it.copy(filter = event.filter) }
            is GalleryUiEvent.ToggleSelection -> _state.update {
                it.copy(selectedIds = if (event.photoId in it.selectedIds) it.selectedIds - event.photoId else it.selectedIds + event.photoId)
            }
            is GalleryUiEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(event.photoId) }
            GalleryUiEvent.ClearSelection -> _state.update { it.copy(selectedIds = emptySet()) }
        }
    }
}
