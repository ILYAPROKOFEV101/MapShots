package com.ilya.safarshots.feature.photoDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.core.settings.AppSettingsRepository
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import com.ilya.safarshots.domain.usecase.DeleteGeoPhotoUseCase
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

data class PhotoDetailUiState(
    val photo: GeoPhoto? = null,
    val noteDraft: String = "",
    val showNoteDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showPreciseCoordinates: Boolean = true,
)

sealed interface PhotoDetailEffect {
    data object Deleted : PhotoDetailEffect
    data class Message(val text: String) : PhotoDetailEffect
}

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GeoPhotoRepository,
    settingsRepository: AppSettingsRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteGeoPhoto: DeleteGeoPhotoUseCase,
) : ViewModel() {
    private val photoId: String = checkNotNull(savedStateHandle["photoId"])
    private val _state = MutableStateFlow(PhotoDetailUiState())
    val state: StateFlow<PhotoDetailUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PhotoDetailEffect>()
    val effects: SharedFlow<PhotoDetailEffect> = _effects.asSharedFlow()

    init {
        repository.observePhoto(photoId)
            .onEach { photo ->
                _state.update { it.copy(photo = photo, noteDraft = photo?.note.orEmpty()) }
            }
            .launchIn(viewModelScope)
        settingsRepository.observe()
            .onEach { settings ->
                _state.update { it.copy(showPreciseCoordinates = settings.showPreciseCoordinates) }
            }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite() {
        viewModelScope.launch { toggleFavorite(photoId) }
    }

    fun openNoteDialog() {
        _state.update { it.copy(showNoteDialog = true, noteDraft = it.photo?.note.orEmpty()) }
    }

    fun setNoteDraft(value: String) {
        _state.update { it.copy(noteDraft = value) }
    }

    fun saveNote() {
        viewModelScope.launch {
            repository.updateNote(photoId, state.value.noteDraft.takeIf { it.isNotBlank() })
            _state.update { it.copy(showNoteDialog = false) }
        }
    }

    fun dismissNoteDialog() {
        _state.update { it.copy(showNoteDialog = false) }
    }

    fun showDeleteDialog(show: Boolean) {
        _state.update { it.copy(showDeleteDialog = show) }
    }

    fun deletePhoto() {
        viewModelScope.launch {
            deleteGeoPhoto(photoId)
            _effects.emit(PhotoDetailEffect.Deleted)
        }
    }
}
