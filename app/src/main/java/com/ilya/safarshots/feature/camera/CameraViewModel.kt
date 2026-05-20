package com.ilya.safarshots.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.core.location.LocationClient
import com.ilya.safarshots.data.storage.PhotoStorage
import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.usecase.CapturePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class LocationStatus(val label: String) {
    SEARCHING("Searching location..."),
    READY("Location ready"),
    APPROXIMATE("Approximate location"),
    NONE("No location"),
}

data class CameraUiState(
    val isSaving: Boolean = false,
    val locationStatus: LocationStatus = LocationStatus.SEARCHING,
    val lastLocation: PhotoLocation? = null,
)

sealed interface CameraEffect {
    data class PhotoSaved(val photoId: String) : CameraEffect
    data class Message(val text: String) : CameraEffect
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val photoStorage: PhotoStorage,
    private val locationClient: LocationClient,
    private val capturePhoto: CapturePhotoUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CameraEffect>()
    val effects: SharedFlow<CameraEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            locationClient.observeLastKnownLocation().collect { location ->
                _state.update {
                    it.copy(
                        lastLocation = location,
                        locationStatus = when {
                            location == null -> LocationStatus.NONE
                            location.isApproximate -> LocationStatus.APPROXIMATE
                            else -> LocationStatus.READY
                        },
                    )
                }
            }
        }
    }

    fun createOutputFile(): File = photoStorage.createOriginalFile()

    fun onImageSaved(file: File, cameraFacing: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching {
                // Use the location already cached by observeLastKnownLocation() so we don't
                // block the capture flow waiting for a fresh GPS fix.
                val location = _state.value.lastLocation
                val photo = capturePhoto(file, location, cameraFacing)
                _effects.emit(CameraEffect.PhotoSaved(photo.id))
            }.onFailure { throwable ->
                file.delete()
                _effects.emit(CameraEffect.Message(throwable.message ?: "Не удалось сохранить фото"))
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun onCaptureError(message: String) {
        viewModelScope.launch {
            _effects.emit(CameraEffect.Message(message))
        }
    }
}
