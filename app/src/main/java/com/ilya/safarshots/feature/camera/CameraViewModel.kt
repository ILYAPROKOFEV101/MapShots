package com.ilya.safarshots.feature.camera

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.R
import com.ilya.safarshots.core.location.LocationClient
import com.ilya.safarshots.data.storage.PhotoStorage
import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.usecase.CapturePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

enum class LocationStatus(@StringRes val labelRes: Int, val isAvailable: Boolean) {
    SEARCHING(R.string.location_searching, isAvailable = false),
    READY(R.string.location_ready, isAvailable = true),
    APPROXIMATE(R.string.location_approximate, isAvailable = true),
    NONE(R.string.location_none, isAvailable = false),
}

data class CameraUiState(
    val isSaving: Boolean = false,
    val locationStatus: LocationStatus = LocationStatus.SEARCHING,
    val lastLocation: PhotoLocation? = null,
)

sealed interface CameraEffect {
    data class PhotoSaved(val photoId: String) : CameraEffect
    data class Message(@StringRes val textRes: Int) : CameraEffect
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
    private var locationRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            locationClient.observeLastKnownLocation().collect { location ->
                updateLocation(location)
            }
        }
    }

    fun createOutputFile(): File = photoStorage.createOriginalFile()

    fun onImageSaved(file: File, cameraFacing: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching {
                // Permission may have been granted only a moment before the shutter press,
                // before the periodic location observer has emitted its next value. In that
                // case wait briefly for a fix so the photo is never saved without an available
                // device location. Once persisted, the collage can enable its map immediately.
                val location = _state.value.lastLocation
                    ?: locationClient.getCurrentLocationOrNull(CAPTURE_LOCATION_TIMEOUT_MS)
                if (location != null) updateLocation(location)
                val photo = capturePhoto(file, location, cameraFacing)
                _effects.emit(CameraEffect.PhotoSaved(photo.id))
            }.onFailure {
                file.delete()
                _effects.emit(CameraEffect.Message(R.string.camera_save_failed))
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun onCaptureError() {
        viewModelScope.launch {
            _effects.emit(CameraEffect.Message(R.string.camera_capture_failed))
        }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        if (!granted) {
            locationRefreshJob?.cancel()
            updateLocation(null)
            return
        }
        locationRefreshJob?.cancel()
        locationRefreshJob = viewModelScope.launch {
            _state.update { it.copy(locationStatus = LocationStatus.SEARCHING) }
            updateLocation(locationClient.getCurrentLocationOrNull(PERMISSION_LOCATION_TIMEOUT_MS))
        }
    }

    private fun updateLocation(location: PhotoLocation?) {
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

    private companion object {
        const val CAPTURE_LOCATION_TIMEOUT_MS = 3_500L
        const val PERMISSION_LOCATION_TIMEOUT_MS = 5_000L
    }
}
