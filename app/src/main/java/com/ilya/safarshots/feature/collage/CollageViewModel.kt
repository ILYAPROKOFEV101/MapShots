package com.ilya.safarshots.feature.collage

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.core.location.AddressResolver
import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import com.ilya.safarshots.domain.usecase.CreateCollageUseCase
import com.ilya.safarshots.domain.usecase.ExportCollageUseCase
import com.ilya.safarshots.domain.usecase.GetGeoPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollageUiState(
    val allPhotos: List<GeoPhoto> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val options: CollageOptions = CollageOptions(),
    val preview: Bitmap? = null,
    val locationLabelDraft: String = "",
    val locationLabelPhotoId: String? = null,
    val isLocationLabelDirty: Boolean = false,
    val isResolvingLocationLabel: Boolean = false,
    val isRendering: Boolean = false,
    val isExporting: Boolean = false,
    val message: String? = null,
) {
    val selectedPhotos: List<GeoPhoto>
        get() {
            val photosById = allPhotos.associateBy { it.id }
            return selectedIds.mapNotNull { photosById[it] }
        }
    val mainPhoto: GeoPhoto? get() = selectedPhotos.firstOrNull()
    val requiredMapSnapshotPhoto: GeoPhoto?
        get() = mainPhoto?.takeIf { photo ->
            options.showMiniMap &&
                when (options.template) {
                    CollageTemplate.PHOTO_WITH_MAP, CollageTemplate.POSTCARD -> true
                    else -> false
                } &&
                photo.location != null
        }
    val photoNeedingMapSnapshot: GeoPhoto?
        get() = requiredMapSnapshotPhoto?.takeIf { it.mapSnapshotUri == null }
}

@HiltViewModel
class CollageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getGeoPhotos: GetGeoPhotosUseCase,
    private val createCollage: CreateCollageUseCase,
    private val exportCollage: ExportCollageUseCase,
    private val geoPhotoRepository: GeoPhotoRepository,
    private val addressResolver: AddressResolver,
) : ViewModel() {
    private val initialIds = savedStateHandle.get<String>("photoIds").parsePhotoIds()
    private val initialTemplate = savedStateHandle.get<String>("template")
        ?.let { runCatching { CollageTemplate.valueOf(it) }.getOrNull() }
        ?: CollageTemplate.PHOTO_WITH_MAP
    private var renderJob: Job? = null
    private val addressLookupRequestedIds = mutableSetOf<String>()

    private val _state = MutableStateFlow(
        CollageUiState(
            selectedIds = initialIds.toSet(),
            options = CollageOptions(template = initialTemplate),
        ),
    )
    val state: StateFlow<CollageUiState> = _state.asStateFlow()

    private var lastSelectionSignature: String? = null

    init {
        getGeoPhotos()
            .onEach { photos ->
                _state.update { it.copy(allPhotos = photos) }
                syncLocationLabelEditor()
                resolveMainAddressIfNeeded()
                maybeRenderForSelection()
            }
            .launchIn(viewModelScope)
        if (initialIds.isNotEmpty()) {
            backfillMapSnapshots(initialIds)
        }
    }

    private fun maybeRenderForSelection() {
        val current = _state.value
        if (current.selectedIds.isEmpty()) return
        val signature = current.selectedPhotos
            .joinToString("|") { "${it.id}=${it.mapSnapshotUri.orEmpty()}=${it.location?.addressText.orEmpty()}" }
            .ifEmpty { current.selectedIds.sorted().joinToString(",") }
        if (signature == lastSelectionSignature) return
        lastSelectionSignature = signature
        renderPreview()
    }

    private fun backfillMapSnapshots(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                runCatching { geoPhotoRepository.ensureMapSnapshot(id) }
            }
        }
    }

    fun toggleSelection(photoId: String) {
        val wasSelected = photoId in _state.value.selectedIds
        _state.update {
            val next = if (photoId in it.selectedIds) it.selectedIds - photoId else it.selectedIds + photoId
            it.copy(selectedIds = next.take(12).toSet())
        }
        if (!wasSelected && photoId in _state.value.selectedIds) {
            // Pre-warm the on-disk map snapshot for the newly added photo so subsequent
            // preview renders can take the fast path instead of waiting on the network.
            backfillMapSnapshots(listOf(photoId))
        }
        maybeRenderForSelection()
    }

    fun setTemplate(template: CollageTemplate) {
        updateOptions { it.copy(template = template) }
    }

    fun setAspectRatio(aspectRatio: CollageAspectRatio) {
        updateOptions { it.copy(aspectRatio = aspectRatio) }
    }

    fun setShowDate(value: Boolean) {
        updateOptions { it.copy(showDate = value) }
    }

    fun setShowCoordinates(value: Boolean) {
        updateOptions { it.copy(showCoordinates = value) }
    }

    fun setShowMiniMap(value: Boolean) {
        updateOptions { it.copy(showMiniMap = value) }
    }

    fun setDarkStyle(value: Boolean) {
        updateOptions { it.copy(darkStyle = value) }
    }

    fun setMapZoom(value: Int) {
        updateOptions { it.copy(mapZoom = value.coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)) }
    }

    fun setUseTouristMapStyle(value: Boolean) {
        updateOptions { it.copy(useTouristMapStyle = value) }
    }

    fun setLocationLabelDraft(value: String) {
        _state.update { it.copy(locationLabelDraft = value, isLocationLabelDirty = true) }
    }

    fun saveLocationLabel() {
        val current = state.value
        val photo = current.mainPhoto ?: return
        if (photo.location == null) return
        viewModelScope.launch {
            val label = current.locationLabelDraft.trim().takeIf { it.isNotBlank() }
            addressLookupRequestedIds.add(photo.id)
            runCatching { geoPhotoRepository.updateAddressText(photo.id, label) }
                .onSuccess {
                    _state.update { it.copy(isLocationLabelDirty = false) }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(message = throwable.message ?: "Не удалось сохранить адрес") }
                }
        }
    }

    fun setMapCorner(corner: CollageMapCorner) {
        updateOptions { it.copy(mapCorner = corner) }
    }

    fun renderPreview() {
        val current = state.value
        if (current.selectedIds.isEmpty()) return
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _state.update { it.copy(isRendering = true) }
            runCatching {
                createCollage(current.selectedIds.toList(), current.options)
            }.onSuccess { bitmap ->
                _state.update { it.copy(preview = bitmap, isRendering = false) }
            }.onFailure { throwable ->
                _state.update { it.copy(isRendering = false, message = throwable.message ?: "Не удалось создать preview") }
            }
        }
    }

    fun export() {
        val current = state.value
        if (current.selectedIds.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            runCatching {
                exportCollage(current.selectedIds.toList(), current.options)
            }.onSuccess {
                _state.update { it.copy(isExporting = false, message = "Коллаж сохранен") }
            }.onFailure { throwable ->
                _state.update { it.copy(isExporting = false, message = throwable.message ?: "Не удалось экспортировать коллаж") }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun onMapSnapshotCaptured(photoId: String, bitmap: Bitmap) {
        viewModelScope.launch {
            runCatching { geoPhotoRepository.saveMapSnapshot(photoId, bitmap) }
        }
    }

    private fun updateOptions(block: (CollageOptions) -> CollageOptions) {
        // Keep the previously rendered preview visible while we re-render so the user
        // sees a smooth update instead of the empty "Rendering…" placeholder.
        _state.update { it.copy(options = block(it.options)) }
        renderPreview()
    }

    private fun syncLocationLabelEditor() {
        val current = _state.value
        val photo = current.mainPhoto
        if (photo?.location == null) {
            if (current.locationLabelPhotoId != null || current.locationLabelDraft.isNotBlank()) {
                _state.update {
                    it.copy(
                        locationLabelDraft = "",
                        locationLabelPhotoId = null,
                        isLocationLabelDirty = false,
                    )
                }
            }
            return
        }
        val addressText = photo.location.addressText.orEmpty()
        if (current.locationLabelPhotoId != photo.id || !current.isLocationLabelDirty) {
            _state.update {
                it.copy(
                    locationLabelDraft = addressText,
                    locationLabelPhotoId = photo.id,
                    isLocationLabelDirty = false,
                )
            }
        }
    }

    private fun resolveMainAddressIfNeeded() {
        val photo = state.value.mainPhoto ?: return
        val location = photo.location ?: return
        if (!location.addressText.isNullOrBlank()) return
        if (!addressLookupRequestedIds.add(photo.id)) return
        viewModelScope.launch {
            _state.update { it.copy(isResolvingLocationLabel = true) }
            val resolved = addressResolver.resolve(location)
            if (!resolved.isNullOrBlank()) {
                runCatching { geoPhotoRepository.updateAddressText(photo.id, resolved) }
            }
            _state.update { it.copy(isResolvingLocationLabel = false) }
        }
    }

    private fun String?.parsePhotoIds(): List<String> =
        this.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }

    private companion object {
        const val MIN_MAP_ZOOM = 11
        const val MAX_MAP_ZOOM = 17
    }
}
