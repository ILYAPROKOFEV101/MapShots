package com.ilya.safarshots.feature.photoDetail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.R
import com.ilya.safarshots.core.location.AddressResolver
import com.ilya.safarshots.core.settings.AppSettingsRepository
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import com.ilya.safarshots.domain.usecase.DeleteGeoPhotoUseCase
import com.ilya.safarshots.domain.usecase.ToggleFavoriteUseCase
import com.ilya.safarshots.domain.usecase.UpdatePhotoLocationUseCase
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
    val showLocationDialog: Boolean = false,
    val latitudeDraft: String = "",
    val longitudeDraft: String = "",
    val addressDraft: String = "",
    val showPreciseCoordinates: Boolean = true,
)

sealed interface PhotoDetailEffect {
    data object Deleted : PhotoDetailEffect
    data class Message(@StringRes val textRes: Int) : PhotoDetailEffect
}

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GeoPhotoRepository,
    settingsRepository: AppSettingsRepository,
    private val addressResolver: AddressResolver,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteGeoPhoto: DeleteGeoPhotoUseCase,
    private val updatePhotoLocation: UpdatePhotoLocationUseCase,
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

    fun openLocationDialog() {
        val location = _state.value.photo?.location
        _state.update {
            it.copy(
                showLocationDialog = true,
                latitudeDraft = location?.latitude?.toString().orEmpty(),
                longitudeDraft = location?.longitude?.toString().orEmpty(),
                addressDraft = location?.addressText.orEmpty(),
            )
        }
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

    fun dismissLocationDialog() {
        _state.update { it.copy(showLocationDialog = false) }
    }

    fun setLatitudeDraft(value: String) {
        _state.update { it.copy(latitudeDraft = value) }
    }

    fun setLongitudeDraft(value: String) {
        _state.update { it.copy(longitudeDraft = value) }
    }

    fun setAddressDraft(value: String) {
        _state.update { it.copy(addressDraft = value) }
    }

    fun saveLocation() {
        viewModelScope.launch {
            val lat = state.value.latitudeDraft.normalizedDoubleOrNull()
            val lng = state.value.longitudeDraft.normalizedDoubleOrNull()
            if (lat == null || lng == null) {
                _effects.emit(PhotoDetailEffect.Message(R.string.photo_detail_invalid_coordinates))
                return@launch
            }
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                _effects.emit(PhotoDetailEffect.Message(R.string.photo_detail_coordinates_out_of_range))
                return@launch
            }
            val typedAddress = state.value.addressDraft.trim().takeIf { it.isNotBlank() }
            val location = PhotoLocation(
                latitude = lat,
                longitude = lng,
                addressText = typedAddress,
                provider = "manual",
                isApproximate = false,
            )
            val resolvedAddress = typedAddress ?: addressResolver.resolve(location)
            updatePhotoLocation(photoId, location.copy(addressText = resolvedAddress))
            _state.update { it.copy(showLocationDialog = false) }
            _effects.emit(PhotoDetailEffect.Message(R.string.photo_detail_location_saved))
        }
    }

    fun clearLocation() {
        viewModelScope.launch {
            updatePhotoLocation(photoId, null)
            _state.update { it.copy(showLocationDialog = false) }
            _effects.emit(PhotoDetailEffect.Message(R.string.photo_detail_location_removed))
        }
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

    private fun String.normalizedDoubleOrNull(): Double? =
        trim().replace(',', '.').toDoubleOrNull()
}
