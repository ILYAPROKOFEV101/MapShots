package com.ilya.safarshots.feature.gallery

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.R
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.usecase.GetGeoPhotosUseCase
import com.ilya.safarshots.domain.usecase.ImportPhotoUseCase
import com.ilya.safarshots.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GalleryFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    FAVORITES(R.string.filter_favorites),
    WITH_LOCATION(R.string.filter_with_location),
    WITHOUT_LOCATION(R.string.filter_no_location),
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

sealed interface GalleryEffect {
    data class Message(@StringRes val textRes: Int) : GalleryEffect
    data class PhotoImported(val photoId: String) : GalleryEffect
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    getGeoPhotos: GetGeoPhotosUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val importPhotoUseCase: ImportPhotoUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()
    private val _effects = MutableSharedFlow<GalleryEffect>()
    val effects: SharedFlow<GalleryEffect> = _effects.asSharedFlow()

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

    fun importPhoto(sourceUri: String) {
        viewModelScope.launch {
            runCatching {
                importPhotoUseCase(sourceUri, location = null)
            }.onSuccess { photo ->
                _effects.emit(GalleryEffect.PhotoImported(photo.id))
            }.onFailure {
                _effects.emit(GalleryEffect.Message(R.string.gallery_import_failed))
            }
        }
    }
}
