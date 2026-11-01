package com.ilya.safarshots.feature.collage

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.R
import com.ilya.safarshots.core.location.AddressResolver
import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageBackgroundMode
import com.ilya.safarshots.domain.model.CollageCaptionPlacement
import com.ilya.safarshots.domain.model.CollageCard
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageMapPlacement
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.CollageTextLayer
import com.ilya.safarshots.domain.model.CollageTextShape
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import com.ilya.safarshots.domain.usecase.CreateCollageUseCase
import com.ilya.safarshots.domain.usecase.ExportCollageUseCase
import com.ilya.safarshots.domain.usecase.GetGeoPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollageMessage(
    @StringRes val resId: Int,
    val intArg: Int? = null,
)

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
    val message: CollageMessage? = null,
) {
    val selectedPhotos: List<GeoPhoto>
        get() {
            val photosById = allPhotos.associateBy { it.id }
            return selectedIds.mapNotNull { photosById[it] }
        }
    val mainPhoto: GeoPhoto? get() = selectedPhotos.firstOrNull()

    /**
     * The photo currently shown as the collage hero. For PHOTO_WITH_MAP this is the photo
     * picked via [CollageOptions.backgroundPhotoId]; for every other template (and as a
     * fallback) it's the first selected photo. Caption and address editing derive from it.
     */
    val heroPhoto: GeoPhoto?
        get() {
            val photosById = allPhotos.associateBy { it.id }
            return options.backgroundPhotoId?.let(photosById::get) ?: mainPhoto
        }
    val mapTargetPhoto: GeoPhoto?
        get() {
            val photosById = allPhotos.associateBy { it.id }
            return options.mapPhotoId
                ?.let(photosById::get)
                ?.takeIf { it.id in selectedIds }
                ?: heroPhoto?.takeIf { it.location != null }
                ?: selectedPhotos.firstOrNull { it.location != null }
                ?: heroPhoto
                ?: selectedPhotos.firstOrNull()
        }
    val mapPhoto: GeoPhoto? get() = mapTargetPhoto?.takeIf { it.location != null }
    val requiredMapSnapshotPhoto: GeoPhoto?
        get() = mapPhoto?.takeIf { options.showMiniMap }
    val photoNeedingMapSnapshot: GeoPhoto?
        get() = requiredMapSnapshotPhoto?.takeIf { it.mapSnapshotUri == null }
}

internal fun shouldShowMiniMapByDefault(
    hasGeotaggedPhoto: Boolean,
    template: CollageTemplate,
): Boolean = hasGeotaggedPhoto ||
    template == CollageTemplate.PHOTO_WITH_MAP ||
    template == CollageTemplate.POSTCARD

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
    private var mapVisibilityExplicitlySet = false

    private val _state = MutableStateFlow(
        CollageUiState(
            selectedIds = initialIds.toSet(),
            options = CollageOptions(
                template = initialTemplate,
                showMiniMap = initialTemplate == CollageTemplate.PHOTO_WITH_MAP ||
                    initialTemplate == CollageTemplate.POSTCARD,
            ),
        ),
    )
    val state: StateFlow<CollageUiState> = _state.asStateFlow()

    private var lastSelectionSignature: String? = null

    init {
        getGeoPhotos()
            .onEach { photos ->
                _state.update { it.copy(allPhotos = photos) }
                syncDefaultMapVisibility()
                syncLocationLabelEditor()
                resolveMainAddressIfNeeded()
                syncPhotoWithMapLayout()
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

    /**
     * Keeps the Photo+Map layout in sync with the current selection. When a new photo is added,
     * we auto-place it as a draggable card. When the hero photo or any card photo gets
     * deselected, we prune the layout so the renderer never references missing photos.
     */
    private fun syncPhotoWithMapLayout() {
        val current = _state.value
        if (current.options.template != CollageTemplate.PHOTO_WITH_MAP) return
        val selectedIds = current.selectedIds
        if (selectedIds.isEmpty()) return

        val existingOptions = current.options
        val heroPhotoId = existingOptions.backgroundPhotoId
            ?.takeIf { it in selectedIds }
            ?: selectedIds.first()

        // Preserve existing card positions, but drop entries referencing missing photos
        // (e.g. the user removed a photo from the selection) and entries that match the
        // current hero — the hero photo should never appear as a card.
        val existingCards = existingOptions.cards
            .filter { it.photoId in selectedIds && it.photoId != heroPhotoId }

        val coveredIds = existingCards.map { it.photoId }.toSet() + heroPhotoId
        val newCardIds = selectedIds.filterNot { it in coveredIds }
        val newCards = newCardIds.mapIndexed { addedIndex, photoId ->
            defaultCardLayout(existingCards.size + addedIndex).copy(photoId = photoId)
        }
        val updatedCards = existingCards + newCards

        // Preserve any user-customised map placement; otherwise leave it null so the
        // renderer can fall back to the legacy corner placement.
        val mapPlacement = existingOptions.mapPlacement

        val nextOptions = existingOptions.copy(
            backgroundPhotoId = heroPhotoId,
            cards = updatedCards,
            mapPlacement = mapPlacement,
        )
        if (nextOptions != existingOptions) {
            _state.update { it.copy(options = nextOptions) }
            renderPreview()
        }
    }

    private fun defaultCardLayout(index: Int): CollageCard {
        // Scatter additional cards in a tasteful zigzag so multiple photos look like an
        // Instagram-style stack instead of overlapping in the same spot.
        val positions = listOf(
            Triple(0.78f, 0.50f, 4f),
            Triple(0.22f, 0.28f, -4f),
            Triple(0.78f, 0.22f, 3f),
            Triple(0.28f, 0.62f, -3f),
            Triple(0.55f, 0.38f, 2f),
        )
        val slot = positions[index % positions.size]
        return CollageCard(
            photoId = "",
            centerX = slot.first,
            centerY = slot.second,
            widthFrac = DEFAULT_CARD_FRAC,
            rotationDeg = slot.third,
        )
    }

    private fun backfillMapSnapshots(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                runCatching { geoPhotoRepository.ensureMapSnapshot(id) }
            }
        }
    }

    fun toggleSelection(photoId: String) {
        val current = _state.value
        val wasSelected = photoId in current.selectedIds
        val nextIds: Set<String>
        var hitLimitMessage: CollageMessage? = null
        if (wasSelected) {
            nextIds = current.selectedIds - photoId
        } else if (current.selectedIds.size >= MAX_SELECTION) {
            // Keep the existing selection intact instead of silently dropping one of the
            // previously chosen photos via take(MAX_SELECTION).
            nextIds = current.selectedIds
            hitLimitMessage = CollageMessage(R.string.collage_editor_select_limit, MAX_SELECTION)
        } else {
            nextIds = current.selectedIds + photoId
        }
        _state.update {
            it.copy(
                selectedIds = nextIds,
                options = if (wasSelected && it.options.mapPhotoId == photoId) {
                    it.options.copy(mapPhotoId = null)
                } else {
                    it.options
                },
                message = hitLimitMessage ?: it.message,
            )
        }
        if (!wasSelected && photoId in nextIds) {
            // Pre-warm the on-disk map snapshot for the newly added photo so subsequent
            // preview renders can take the fast path instead of waiting on the network.
            backfillMapSnapshots(listOf(photoId))
        }
        syncPhotoWithMapLayout()
        syncDefaultMapVisibility()
        maybeRenderForSelection()
    }

    fun clearSelection() {
        if (_state.value.selectedIds.isEmpty()) return
        _state.update {
            it.copy(
                selectedIds = emptySet(),
                preview = null,
                options = it.options.copy(
                    backgroundPhotoId = null,
                    mapPhotoId = null,
                    cards = emptyList(),
                    mapPlacement = null,
                ),
            )
        }
        lastSelectionSignature = null
    }

    fun setTemplate(template: CollageTemplate) {
        if (_state.value.selectedIds.size < template.minimumPhotos) return
        updateOptions { it.copy(template = template) }
        syncDefaultMapVisibility()
        if (template == CollageTemplate.PHOTO_WITH_MAP) {
            syncPhotoWithMapLayout()
        }
    }

    fun moveCard(index: Int, centerX: Float, centerY: Float) {
        updateOptions { current ->
            val cards = current.cards.toMutableList()
            val card = cards.getOrNull(index) ?: return@updateOptions current
            cards[index] = card.copy(
                centerX = centerX.coerceIn(0f, 1f),
                centerY = centerY.coerceIn(0f, 1f),
            )
            current.copy(cards = cards)
        }
    }

    fun resizeCard(index: Int, widthFrac: Float) {
        updateOptions { current ->
            val cards = current.cards.toMutableList()
            val card = cards.getOrNull(index) ?: return@updateOptions current
            cards[index] = card.copy(widthFrac = widthFrac.coerceIn(MIN_CARD_FRAC, MAX_CARD_FRAC))
            current.copy(cards = cards)
        }
    }

    fun transformCard(
        index: Int,
        centerX: Float,
        centerY: Float,
        widthFrac: Float,
        rotationDeg: Float,
    ) {
        updateOptions { current ->
            val cards = current.cards.toMutableList()
            val card = cards.getOrNull(index) ?: return@updateOptions current
            cards[index] = card.copy(
                centerX = centerX.coerceIn(0f, 1f),
                centerY = centerY.coerceIn(0f, 1f),
                widthFrac = widthFrac.coerceIn(MIN_CARD_FRAC, MAX_CARD_FRAC),
                rotationDeg = rotationDeg.normalizedDegrees(),
            )
            current.copy(cards = cards)
        }
    }

    fun removeCard(index: Int) {
        updateOptions { current ->
            val cards = current.cards.toMutableList()
            if (index !in cards.indices) return@updateOptions current
            cards.removeAt(index)
            current.copy(cards = cards)
        }
    }

    fun promoteCardToHero(index: Int) {
        // Swap a card photo with the current background photo so the user can quickly
        // try a different hero shot without rebuilding the selection.
        val current = _state.value.options
        val card = current.cards.getOrNull(index) ?: return
        val previousHero = current.backgroundPhotoId
        val updatedCards = current.cards.toMutableList()
        if (previousHero != null && previousHero != card.photoId) {
            updatedCards[index] = card.copy(photoId = previousHero)
        } else {
            updatedCards.removeAt(index)
        }
        _state.update {
            it.copy(options = it.options.copy(backgroundPhotoId = card.photoId, cards = updatedCards))
        }
        renderPreview()
    }

    fun moveMap(centerX: Float, centerY: Float) {
        updateOptions { current ->
            val base = current.mapPlacement ?: defaultMapPlacement(current.mapCorner)
            current.copy(
                mapPlacement = base.copy(
                    centerX = centerX.coerceIn(0f, 1f),
                    centerY = centerY.coerceIn(0f, 1f),
                ),
            )
        }
    }

    fun resizeMap(widthFrac: Float) {
        updateOptions { current ->
            val base = current.mapPlacement ?: defaultMapPlacement(current.mapCorner)
            current.copy(mapPlacement = base.copy(widthFrac = widthFrac.coerceIn(MIN_CARD_FRAC, MAX_CARD_FRAC)))
        }
    }

    fun transformMap(centerX: Float, centerY: Float, widthFrac: Float, rotationDeg: Float) {
        updateOptions { current ->
            val base = current.mapPlacement ?: defaultMapPlacement(current.mapCorner)
            current.copy(
                mapPlacement = base.copy(
                    centerX = centerX.coerceIn(0f, 1f),
                    centerY = centerY.coerceIn(0f, 1f),
                    widthFrac = widthFrac.coerceIn(MIN_CARD_FRAC, MAX_CARD_FRAC),
                    rotationDeg = rotationDeg.normalizedDegrees(),
                ),
            )
        }
    }

    /**
     * Moves the caption pill. [originX]/[originY] are normalized canvas coords of the
     * caption's top-left corner so the drag matches the finger position exactly.
     */
    fun moveCaption(originX: Float, originY: Float) {
        updateOptions { current ->
            val base = current.captionPlacement ?: defaultCaptionPlacement()
            current.copy(
                captionPlacement = base.copy(
                    originX = originX.coerceIn(0f, 1f),
                    originY = originY.coerceIn(0f, 1f),
                ),
            )
        }
    }

    fun resizeCaption(widthFrac: Float) {
        updateOptions { current ->
            val base = current.captionPlacement ?: defaultCaptionPlacement()
            current.copy(captionPlacement = base.copy(widthFrac = widthFrac.coerceIn(MIN_CAPTION_FRAC, MAX_CAPTION_FRAC)))
        }
    }

    fun transformCaption(originX: Float, originY: Float, widthFrac: Float, rotationDeg: Float) {
        updateOptions { current ->
            val base = current.captionPlacement ?: defaultCaptionPlacement()
            current.copy(
                captionPlacement = base.copy(
                    originX = originX.coerceIn(0f, 1f),
                    originY = originY.coerceIn(0f, 1f),
                    widthFrac = widthFrac.coerceIn(MIN_CAPTION_FRAC, MAX_CAPTION_FRAC),
                    rotationDeg = rotationDeg.normalizedDegrees(),
                ),
            )
        }
    }

    fun setCaptionTextSizeScale(scale: Float) {
        updateOptions { current ->
            current.copy(captionStyle = current.captionStyle.copy(textSizeScale = scale.coerceIn(0.65f, 1.8f)))
        }
    }

    fun setCaptionTextColor(color: Int) {
        updateOptions { current -> current.copy(captionStyle = current.captionStyle.copy(textColor = color)) }
    }

    fun setCaptionBackgroundColor(color: Int) {
        updateOptions { current -> current.copy(captionStyle = current.captionStyle.copy(backgroundColor = color)) }
    }

    fun setCaptionBackgroundOpacity(opacity: Float) {
        updateOptions { current ->
            current.copy(captionStyle = current.captionStyle.copy(backgroundOpacity = opacity.coerceIn(0f, 1f)))
        }
    }

    fun setCaptionBorderColor(color: Int) {
        updateOptions { current -> current.copy(captionStyle = current.captionStyle.copy(borderColor = color)) }
    }

    fun setCaptionBorderEnabled(enabled: Boolean) {
        updateOptions { current ->
            current.copy(captionStyle = current.captionStyle.copy(borderWidthFrac = if (enabled) 0.004f else 0f))
        }
    }

    fun setCaptionCornerRadius(radiusFrac: Float) {
        updateOptions { current ->
            current.copy(captionStyle = current.captionStyle.copy(cornerRadiusFrac = radiusFrac.coerceIn(0f, 0.12f)))
        }
    }

    fun setCaptionShape(shape: CollageTextShape) {
        updateOptions { current -> current.copy(captionStyle = current.captionStyle.copy(shape = shape)) }
    }

    fun setBackgroundMode(mode: CollageBackgroundMode) {
        updateOptions { it.copy(backgroundMode = mode) }
    }

    /**
     * Picks the photo shown as the hero background. The previous hero is pushed back into
     * the cards stack so the user never loses a previously chosen photo.
     */
    fun setBackgroundPhotoId(photoId: String) {
        val state = _state.value
        if (photoId !in state.selectedIds) return
        val opts = state.options
        if (opts.backgroundPhotoId == photoId) return
        val previousHero = opts.backgroundPhotoId
        val cards = opts.cards.toMutableList()
        val matchingCardIndex = cards.indexOfFirst { it.photoId == photoId }
        if (matchingCardIndex >= 0) {
            cards.removeAt(matchingCardIndex)
        }
        if (previousHero != null && previousHero != photoId && cards.none { it.photoId == previousHero }) {
            cards.add(defaultCardLayout(cards.size).copy(photoId = previousHero))
        }
        _state.update {
            it.copy(
                options = it.options.copy(
                    backgroundPhotoId = photoId,
                    backgroundMode = CollageBackgroundMode.HERO_PHOTO,
                    cards = cards,
                ),
            )
        }
        // The new hero may not have a cached map snapshot yet — kick off backfill so the
        // mini-map shows the right location immediately instead of disappearing while we wait
        // for the hidden Google Map snapshotter to capture one.
        backfillMapSnapshots(listOf(photoId))
        // Re-sync the address editor and re-trigger geocoding for the new hero so the
        // caption picks up its label rather than stale text from the previous hero.
        syncLocationLabelEditor()
        resolveMainAddressIfNeeded()
        renderPreview()
    }

    fun setBackgroundColor(@androidx.annotation.ColorInt color: Int) {
        updateOptions {
            it.copy(
                backgroundColor = color,
                backgroundMode = CollageBackgroundMode.SOLID_COLOR,
            )
        }
    }

    private fun defaultMapPlacement(corner: CollageMapCorner): CollageMapPlacement {
        val centerX = when (corner) {
            CollageMapCorner.TOP_LEFT, CollageMapCorner.BOTTOM_LEFT -> 0.22f
            CollageMapCorner.TOP_RIGHT, CollageMapCorner.BOTTOM_RIGHT -> 0.78f
        }
        val centerY = when (corner) {
            CollageMapCorner.TOP_LEFT, CollageMapCorner.TOP_RIGHT -> 0.22f
            CollageMapCorner.BOTTOM_LEFT, CollageMapCorner.BOTTOM_RIGHT -> 0.78f
        }
        return CollageMapPlacement(centerX = centerX, centerY = centerY, widthFrac = 0.4f)
    }

    private fun defaultCaptionPlacement(): CollageCaptionPlacement =
        CollageCaptionPlacement(originX = 0.04f, originY = 0.78f, widthFrac = 0.6f)

    fun setAspectRatio(aspectRatio: CollageAspectRatio) {
        updateOptions { it.copy(aspectRatio = aspectRatio) }
    }

    fun setShowCaption(value: Boolean) {
        updateOptions { it.copy(showCaption = value) }
    }

    fun setCustomCaption(value: String) {
        _state.update { current ->
            current.copy(
                options = current.options.copy(
                    customCaption = value.take(MAX_CAPTION_LENGTH),
                    // Typing text is an explicit request to put it on the collage. This also
                    // makes adding text a one-step action after the caption was previously hidden.
                    showCaption = current.options.showCaption || value.isNotBlank(),
                ),
            )
        }
        // Text fields can emit an update for every keystroke. Debounce the bitmap renderer so
        // photo-only layouts stay live without repeatedly decoding every source image.
        schedulePreviewRender(CAPTION_RENDER_DEBOUNCE_MS)
    }

    fun setShowDate(value: Boolean) {
        updateOptions { it.copy(showDate = value) }
    }

    fun setShowCoordinates(value: Boolean) {
        updateOptions { it.copy(showCoordinates = value) }
    }

    fun setShowMiniMap(value: Boolean) {
        mapVisibilityExplicitlySet = true
        val sourceId = state.value.mapPhoto?.id
        updateOptions {
            it.copy(
                showMiniMap = value,
                mapPhotoId = if (value) it.mapPhotoId ?: sourceId else it.mapPhotoId,
            )
        }
        if (value && sourceId != null) backfillMapSnapshots(listOf(sourceId))
    }

    fun setMapPhotoId(photoId: String) {
        val photo = state.value.selectedPhotos.firstOrNull { it.id == photoId } ?: return
        updateOptions { it.copy(mapPhotoId = photo.id) }
        if (photo.location != null) backfillMapSnapshots(listOf(photo.id))
    }

    fun saveManualMapLocation(photoId: String, latitude: Double, longitude: Double, address: String?) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            _state.update { it.copy(message = CollageMessage(R.string.collage_editor_invalid_map_location)) }
            return
        }
        if (photoId !in state.value.selectedIds) return
        mapVisibilityExplicitlySet = true
        viewModelScope.launch {
            val typedAddress = address?.trim()?.takeIf { it.isNotBlank() }
            val location = PhotoLocation(
                latitude = latitude,
                longitude = longitude,
                addressText = typedAddress,
                provider = "manual",
                isApproximate = false,
            )
            val resolvedAddress = typedAddress ?: addressResolver.resolve(location)
            runCatching {
                geoPhotoRepository.updatePhotoLocation(
                    photoId,
                    location.copy(addressText = resolvedAddress),
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        options = it.options.copy(mapPhotoId = photoId, showMiniMap = true),
                        message = CollageMessage(R.string.collage_editor_map_location_saved),
                    )
                }
                renderPreview()
            }.onFailure {
                _state.update { it.copy(message = CollageMessage(R.string.collage_editor_map_location_failed)) }
            }
        }
    }

    fun addTextLayer(text: String): Int {
        val current = state.value.options.textLayers
        if (current.size >= MAX_TEXT_LAYERS) return current.lastIndex.coerceAtLeast(0)
        val index = current.size
        val column = index % 3
        val row = index / 3
        val layer = CollageTextLayer(
            text = text.take(MAX_TEXT_LENGTH),
            originX = (0.08f + column * 0.08f).coerceAtMost(0.4f),
            originY = (0.14f + row * 0.12f).coerceAtMost(0.72f),
        )
        updateOptions { it.copy(textLayers = it.textLayers + layer) }
        return index
    }

    fun removeTextLayer(index: Int) {
        updateOptions { current ->
            if (index !in current.textLayers.indices) return@updateOptions current
            current.copy(textLayers = current.textLayers.toMutableList().also { it.removeAt(index) })
        }
    }

    fun setTextLayerText(index: Int, value: String) {
        updateTextLayer(index) { it.copy(text = value.take(MAX_TEXT_LENGTH)) }
    }

    fun moveTextLayer(index: Int, originX: Float, originY: Float) {
        updateTextLayer(index) {
            it.copy(originX = originX.coerceIn(0f, 1f), originY = originY.coerceIn(0f, 1f))
        }
    }

    fun resizeTextLayer(index: Int, widthFrac: Float) {
        updateTextLayer(index) { it.copy(widthFrac = widthFrac.coerceIn(MIN_TEXT_WIDTH, MAX_TEXT_WIDTH)) }
    }

    fun transformTextLayer(
        index: Int,
        originX: Float,
        originY: Float,
        widthFrac: Float,
        rotationDeg: Float,
    ) {
        updateTextLayer(index) {
            it.copy(
                originX = originX.coerceIn(0f, 1f),
                originY = originY.coerceIn(0f, 1f),
                widthFrac = widthFrac.coerceIn(MIN_TEXT_WIDTH, MAX_TEXT_WIDTH),
                rotationDeg = rotationDeg.normalizedDegrees(),
            )
        }
    }

    fun setTextLayerSize(index: Int, textSizeFrac: Float) {
        updateTextLayer(index) { it.copy(textSizeFrac = textSizeFrac.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)) }
    }

    fun setTextLayerTextColor(index: Int, color: Int) {
        updateTextLayer(index) { it.copy(textColor = color) }
    }

    fun setTextLayerBackgroundColor(index: Int, color: Int) {
        updateTextLayer(index) { it.copy(backgroundColor = color) }
    }

    fun setTextLayerBackgroundOpacity(index: Int, opacity: Float) {
        updateTextLayer(index) { it.copy(backgroundOpacity = opacity.coerceIn(0f, 1f)) }
    }

    fun setTextLayerBorderColor(index: Int, color: Int) {
        updateTextLayer(index) { it.copy(borderColor = color) }
    }

    fun setTextLayerBorderEnabled(index: Int, enabled: Boolean) {
        updateTextLayer(index) { it.copy(borderWidthFrac = if (enabled) 0.004f else 0f) }
    }

    fun setTextLayerCornerRadius(index: Int, radiusFrac: Float) {
        updateTextLayer(index) { it.copy(cornerRadiusFrac = radiusFrac.coerceIn(0f, MAX_TEXT_RADIUS)) }
    }

    fun setTextLayerShape(index: Int, shape: CollageTextShape) {
        updateTextLayer(index) { it.copy(shape = shape) }
    }

    private fun updateTextLayer(index: Int, transform: (CollageTextLayer) -> CollageTextLayer) {
        updateOptions { current ->
            val layers = current.textLayers.toMutableList()
            val layer = layers.getOrNull(index) ?: return@updateOptions current
            layers[index] = transform(layer)
            current.copy(textLayers = layers)
        }
    }

    /**
     * A geotagged photo should open with its map already visible in every collage template.
     * Once the user explicitly adds or removes the map, that preference wins for the rest of
     * this editor session.
     */
    private fun syncDefaultMapVisibility() {
        if (mapVisibilityExplicitlySet) return
        val current = _state.value
        val shouldShow = shouldShowMiniMapByDefault(
            hasGeotaggedPhoto = current.selectedPhotos.any { it.location != null },
            template = current.options.template,
        )
        if (current.options.showMiniMap != shouldShow) {
            _state.update { state ->
                state.copy(options = state.options.copy(showMiniMap = shouldShow))
            }
        }
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
        val photo = current.heroPhoto ?: return
        if (photo.location == null) return
        viewModelScope.launch {
            val label = current.locationLabelDraft.trim().takeIf { it.isNotBlank() }
            addressLookupRequestedIds.add(photo.id)
            runCatching { geoPhotoRepository.updateAddressText(photo.id, label) }
                .onSuccess {
                    _state.update { it.copy(isLocationLabelDirty = false) }
                }
                .onFailure {
                    _state.update { it.copy(message = CollageMessage(R.string.collage_editor_save_address_failed)) }
                }
        }
    }

    fun setMapCorner(corner: CollageMapCorner) {
        updateOptions { current ->
            val widthFrac = current.mapPlacement?.widthFrac ?: 0.4f
            current.copy(
                mapCorner = corner,
                mapPlacement = defaultMapPlacement(corner).copy(widthFrac = widthFrac),
            )
        }
    }

    fun renderPreview() {
        schedulePreviewRender()
    }

    private fun schedulePreviewRender(delayMillis: Long = 0L) {
        if (state.value.selectedIds.isEmpty()) return
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            val current = state.value
            _state.update { it.copy(isRendering = true) }
            try {
                val previewOptions = if (current.options.template == CollageTemplate.PHOTO_WITH_MAP) {
                    current.options.copy(renderTextLayers = false)
                } else {
                    // Compose owns the map in these previews so it can be dragged directly.
                    // Export still receives the untouched options and draws the map into the PNG.
                    current.options.copy(renderMiniMap = false, renderTextLayers = false)
                }
                val bitmap = createCollage(current.selectedIds.toList(), previewOptions)
                _state.update { it.copy(preview = bitmap, isRendering = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _state.update { it.copy(isRendering = false, message = CollageMessage(R.string.collage_editor_render_failed)) }
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
                _state.update { it.copy(isExporting = false, message = CollageMessage(R.string.collage_editor_saved)) }
            }.onFailure {
                _state.update { it.copy(isExporting = false, message = CollageMessage(R.string.collage_editor_export_failed)) }
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
        val photo = current.heroPhoto
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
        val photo = state.value.heroPhoto ?: return
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

    private fun Float.normalizedDegrees(): Float = ((this % 360f) + 540f) % 360f - 180f

    private companion object {
        const val MIN_MAP_ZOOM = 11
        const val MAX_MAP_ZOOM = 17
        const val MAX_SELECTION = 12
        const val MIN_CARD_FRAC = 0.14f
        const val MAX_CARD_FRAC = 0.7f
        const val DEFAULT_CARD_FRAC = 0.28f
        const val MIN_CAPTION_FRAC = 0.3f
        const val MAX_CAPTION_FRAC = 0.9f
        const val MAX_CAPTION_LENGTH = 160
        const val CAPTION_RENDER_DEBOUNCE_MS = 180L
        const val MAX_TEXT_LAYERS = 8
        const val MAX_TEXT_LENGTH = 240
        const val MIN_TEXT_WIDTH = 0.2f
        const val MAX_TEXT_WIDTH = 0.9f
        const val MIN_TEXT_SIZE = 0.018f
        const val MAX_TEXT_SIZE = 0.1f
        const val MAX_TEXT_RADIUS = 0.12f
    }
}
