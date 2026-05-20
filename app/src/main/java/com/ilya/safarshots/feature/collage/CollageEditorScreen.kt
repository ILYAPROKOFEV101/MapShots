package com.ilya.safarshots.feature.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageBackgroundMode
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.GeoPhoto
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    onBack: () -> Unit,
    viewModel: CollageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.renderPreview()
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.messageShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Collage editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::export,
                        enabled = state.selectedIds.isNotEmpty() && !state.isExporting,
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (state.selectedIds.isEmpty()) {
            EmptyState(
                title = "Выберите фото для коллажа",
                subtitle = "Можно выбрать фото на карте или в галерее.",
                buttonText = null,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            return@Scaffold
        }
        var selectedCardIndex by remember { mutableStateOf<Int?>(null) }
        // Reset the selection whenever the underlying card list shrinks so we don't keep
        // referencing a removed index.
        LaunchedEffect(state.options.cards.size) {
            val idx = selectedCardIndex
            if (idx != null && idx !in state.options.cards.indices) {
                selectedCardIndex = null
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PreviewCard(
                state = state,
                onMapSnapshot = viewModel::onMapSnapshotCaptured,
                selectedCardIndex = selectedCardIndex,
                onSelectCard = { selectedCardIndex = it },
                onMoveCard = viewModel::moveCard,
                onMoveMap = viewModel::moveMap,
                onMoveCaption = viewModel::moveCaption,
            )
            if (state.options.template == CollageTemplate.PHOTO_WITH_MAP) {
                var showAddPhotosSheet by remember { mutableStateOf(false) }
                PhotosInCollageSection(
                    state = state,
                    selectedIndex = selectedCardIndex,
                    onSelectCard = { selectedCardIndex = it },
                    onResize = viewModel::resizeCard,
                    onResizeMap = viewModel::resizeMap,
                    onResizeCaption = viewModel::resizeCaption,
                    onPromote = { index ->
                        viewModel.promoteCardToHero(index)
                        selectedCardIndex = null
                    },
                    onPickHero = viewModel::setBackgroundPhotoId,
                    onRemoveFromCollage = { photoId ->
                        // Deselect from the picker selection; sync logic prunes hero/cards.
                        viewModel.toggleSelection(photoId)
                        selectedCardIndex = null
                    },
                    onRequestAddPhotos = { showAddPhotosSheet = true },
                )
                if (showAddPhotosSheet) {
                    AddPhotosSheet(
                        availablePhotos = state.allPhotos.filter { it.id !in state.selectedIds },
                        atSelectionLimit = state.selectedIds.size >= MAX_COLLAGE_PHOTOS,
                        onPick = { viewModel.toggleSelection(it) },
                        onDismiss = { showAddPhotosSheet = false },
                    )
                }
                BackgroundEditorSection(
                    state = state,
                    onModeChange = viewModel::setBackgroundMode,
                    onColorChange = viewModel::setBackgroundColor,
                )
            }
            EditorSection(title = "Template") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    items(CollageTemplate.entries.size) { index ->
                        val template = CollageTemplate.entries[index]
                        EditorChip(
                            selected = state.options.template == template,
                            label = template.title,
                            onClick = { viewModel.setTemplate(template) },
                        )
                    }
                }
            }
            EditorSection(title = "Aspect ratio") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollageAspectRatio.entries.forEach { ratio ->
                        EditorChip(
                            selected = state.options.aspectRatio == ratio,
                            label = ratio.label,
                            onClick = { viewModel.setAspectRatio(ratio) },
                        )
                    }
                }
            }
            if (state.heroPhoto?.location != null) {
                EditorSection(title = "Location") {
                    OutlinedTextField(
                        value = state.locationLabelDraft,
                        onValueChange = viewModel::setLocationLabelDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Address") },
                        placeholder = { Text("Address") },
                        minLines = 1,
                        maxLines = 3,
                    )
                    Button(
                        onClick = viewModel::saveLocationLabel,
                        enabled = state.isLocationLabelDirty,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isResolvingLocationLabel) "Looking up address…" else "Save address")
                    }
                }
            }
            if (state.options.template == CollageTemplate.PHOTO_WITH_MAP && state.options.showMiniMap) {
                EditorSection(title = "Map position") {
                    MapCornerPicker(
                        selected = state.options.mapCorner,
                        onSelect = viewModel::setMapCorner,
                    )
                }
            }
            EditorSection(title = "Details") {
                ToggleRow("Date", state.options.showDate, viewModel::setShowDate)
                ToggleRow("Coordinates", state.options.showCoordinates, viewModel::setShowCoordinates)
                ToggleRow("Mini-map", state.options.showMiniMap, viewModel::setShowMiniMap)
                ToggleRow("Dark style", state.options.darkStyle, viewModel::setDarkStyle)
            }
            if (state.options.showMiniMap) {
                EditorSection(title = "Map") {
                    MapZoomRow(
                        zoom = state.options.mapZoom,
                        onZoomChange = viewModel::setMapZoom,
                    )
                    ToggleRow("Tourist map", state.options.useTouristMapStyle, viewModel::setUseTouristMapStyle)
                }
            }
            Button(
                onClick = viewModel::export,
                enabled = !state.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        state.isExporting -> "Exporting…"
                        else -> "Export PNG"
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun PreviewCard(
    state: CollageUiState,
    onMapSnapshot: (String, android.graphics.Bitmap) -> Unit,
    selectedCardIndex: Int?,
    onSelectCard: (Int?) -> Unit,
    onMoveCard: (Int, Float, Float) -> Unit,
    onMoveMap: (Float, Float) -> Unit,
    onMoveCaption: (Float, Float) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(state.options.aspectRatio.width / state.options.aspectRatio.height.toFloat())
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val photoNeedingSnapshot = state.photoNeedingMapSnapshot
                if (photoNeedingSnapshot != null) {
                    HiddenMapSnapshotter(
                        photo = photoNeedingSnapshot,
                        zoom = state.options.mapZoom,
                        useTouristMapStyle = state.options.useTouristMapStyle,
                        onSnapshot = { bitmap -> onMapSnapshot(photoNeedingSnapshot.id, bitmap) },
                    )
                }
                if (state.options.template == CollageTemplate.PHOTO_WITH_MAP && state.selectedIds.isNotEmpty()) {
                    InteractivePhotoMapPreview(
                        state = state,
                        selectedCardIndex = selectedCardIndex,
                        onSelectCard = onSelectCard,
                        onMoveCard = onMoveCard,
                        onMoveMap = onMoveMap,
                        onMoveCaption = onMoveCaption,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                    )
                } else {
                    val preview = state.preview
                    if (preview != null) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Collage preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            when {
                                state.isRendering -> "Rendering…"
                                else -> "Preview"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.isRendering || state.isExporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Compose-based live preview for the PHOTO_WITH_MAP template. Renders the same layers as the
 * exported bitmap (background, caption, mini-map, photo cards) but each photo card and the
 * mini-map are draggable. The export bitmap is still produced via [CollageRenderer] when the
 * user taps "Export PNG", so changes here are reflected one-to-one in the saved PNG.
 */
@Composable
private fun InteractivePhotoMapPreview(
    state: CollageUiState,
    selectedCardIndex: Int?,
    onSelectCard: (Int?) -> Unit,
    onMoveCard: (Int, Float, Float) -> Unit,
    onMoveMap: (Float, Float) -> Unit,
    onMoveCaption: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val opts = state.options
    val photosById = remember(state.allPhotos) { state.allPhotos.associateBy { it.id } }
    val heroPhoto = opts.backgroundPhotoId?.let(photosById::get) ?: state.selectedPhotos.firstOrNull()
    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            // Tap on empty area deselects any selected card.
            detectTapGestures(onTap = { onSelectCard(null) })
        },
    ) {
        val canvasW = constraints.maxWidth
        val canvasH = constraints.maxHeight

        // Background layer.
        when (opts.backgroundMode) {
            CollageBackgroundMode.SOLID_COLOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(opts.backgroundColor)),
                )
            }
            CollageBackgroundMode.HERO_PHOTO -> {
                if (heroPhoto != null) {
                    AsyncImage(
                        model = heroPhoto.thumbnailUri ?: heroPhoto.imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
        }

        val captionPhoto = heroPhoto
        val captionLines = remember(captionPhoto, opts.showDate, opts.showCoordinates) {
            buildCaptionLinesForPreview(captionPhoto, opts.showDate, opts.showCoordinates)
        }

        // Mini-map card (if enabled and we have coordinates).
        if (opts.showMiniMap && captionPhoto?.location != null) {
            val placement = opts.mapPlacement
            val cx = placement?.centerX ?: when (opts.mapCorner) {
                CollageMapCorner.TOP_LEFT -> 0.22f
                CollageMapCorner.TOP_RIGHT -> 0.78f
                CollageMapCorner.BOTTOM_LEFT -> 0.22f
                CollageMapCorner.BOTTOM_RIGHT -> 0.78f
            }
            val cy = placement?.centerY ?: when (opts.mapCorner) {
                CollageMapCorner.TOP_LEFT, CollageMapCorner.TOP_RIGHT -> 0.22f
                CollageMapCorner.BOTTOM_LEFT, CollageMapCorner.BOTTOM_RIGHT -> 0.78f
            }
            val widthFrac = placement?.widthFrac ?: 0.4f
            DraggableLayer(
                canvasW = canvasW,
                canvasH = canvasH,
                centerX = cx,
                centerY = cy,
                widthFrac = widthFrac,
                rotationDeg = 0f,
                selected = selectedCardIndex == MAP_LAYER_INDEX,
                onSelect = { onSelectCard(MAP_LAYER_INDEX) },
                onMove = onMoveMap,
            ) {
                MiniMapTile(
                    photo = captionPhoto,
                    mapZoom = opts.mapZoom,
                    useTouristMapStyle = opts.useTouristMapStyle,
                    darkStyle = opts.darkStyle,
                )
            }
        }

        // Photo cards rendered on top of the background.
        opts.cards.forEachIndexed { index, card ->
            val cardPhoto = photosById[card.photoId] ?: return@forEachIndexed
            DraggableLayer(
                canvasW = canvasW,
                canvasH = canvasH,
                centerX = card.centerX,
                centerY = card.centerY,
                widthFrac = card.widthFrac,
                rotationDeg = card.rotationDeg,
                selected = selectedCardIndex == index,
                onSelect = { onSelectCard(index) },
                onMove = { nx, ny -> onMoveCard(index, nx, ny) },
            ) {
                PhotoCardTile(cardPhoto, darkStyle = opts.darkStyle)
            }
        }

        // Caption pill — rendered last so it stays visually on top and the user can always
        // grab it for dragging even if cards happen to overlap its position.
        if (captionPhoto != null && captionLines.isNotEmpty()) {
            val captionPlacement = opts.captionPlacement
            val originX = captionPlacement?.originX ?: DEFAULT_CAPTION_ORIGIN_X
            val originY = captionPlacement?.originY ?: DEFAULT_CAPTION_ORIGIN_Y
            val captionWidthFrac = captionPlacement?.widthFrac ?: DEFAULT_CAPTION_WIDTH_FRAC
            DraggableCaptionLayer(
                canvasW = canvasW,
                canvasH = canvasH,
                originX = originX,
                originY = originY,
                widthFrac = captionWidthFrac,
                selected = selectedCardIndex == CAPTION_LAYER_INDEX,
                onSelect = { onSelectCard(CAPTION_LAYER_INDEX) },
                onMove = onMoveCaption,
            ) {
                CaptionOverlay(lines = captionLines, darkStyle = opts.darkStyle)
            }
        }
    }
}

private const val MAP_LAYER_INDEX = -1
private const val CAPTION_LAYER_INDEX = -2
private const val DEFAULT_CAPTION_ORIGIN_X = 0.04f
private const val DEFAULT_CAPTION_ORIGIN_Y = 0.78f
private const val DEFAULT_CAPTION_WIDTH_FRAC = 0.6f

/**
 * Mirrors [CollageRenderer.buildCaptionLines] so the live Compose preview shows exactly the
 * same caption rows as the exported PNG (including dedup of the coordinate fallback).
 */
private fun buildCaptionLinesForPreview(
    photo: GeoPhoto?,
    showDate: Boolean,
    showCoordinates: Boolean,
): List<String> {
    if (photo == null) return emptyList()
    val address = photo.location?.addressText?.takeIf { it.isNotBlank() }
    val note = photo.note?.takeIf { it.isNotBlank() }
    val coordsLine = photo.location
        ?.takeIf { showCoordinates }
        ?.let { DateTimeFormatters.coordinates(it.latitude, it.longitude) }
    val dateLine = if (showDate) DateTimeFormatters.photoDateTime(photo.capturedAtMillis) else null
    val title = address
        ?: note
        ?: photo.location
            ?.takeIf { coordsLine == null }
            ?.let { "%.5f, %.5f".format(java.util.Locale.US, it.latitude, it.longitude) }
    return buildList {
        if (title != null) add(title)
        if (dateLine != null) add(dateLine)
        if (coordsLine != null && coordsLine != title) add(coordsLine)
    }
}

private val PRESET_BACKGROUND_COLORS = listOf(
    0xFFEFE7DD.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF1B1B1B.toInt(),
    0xFFFDE2E4.toInt(),
    0xFFE0F4FF.toInt(),
    0xFFE7F5E9.toInt(),
    0xFFFFE9C7.toInt(),
    0xFFE3DAFE.toInt(),
)

@Composable
private fun BackgroundEditorSection(
    state: CollageUiState,
    onModeChange: (CollageBackgroundMode) -> Unit,
    onColorChange: (Int) -> Unit,
) {
    EditorSection(title = "Background") {
        // Mode toggle: hero photo vs solid color.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorChip(
                selected = state.options.backgroundMode == CollageBackgroundMode.HERO_PHOTO,
                label = "Photo",
                onClick = { onModeChange(CollageBackgroundMode.HERO_PHOTO) },
            )
            EditorChip(
                selected = state.options.backgroundMode == CollageBackgroundMode.SOLID_COLOR,
                label = "Color",
                onClick = { onModeChange(CollageBackgroundMode.SOLID_COLOR) },
            )
        }

        if (state.options.backgroundMode == CollageBackgroundMode.SOLID_COLOR) {
            // Soft preset palette + the currently-picked color as the first swatch.
            val swatches = remember(state.options.backgroundColor) {
                buildList {
                    if (state.options.backgroundColor !in PRESET_BACKGROUND_COLORS) {
                        add(state.options.backgroundColor)
                    }
                    addAll(PRESET_BACKGROUND_COLORS)
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(swatches) { color ->
                    ColorSwatch(
                        color = color,
                        selected = color == state.options.backgroundColor,
                        onClick = { onColorChange(color) },
                    )
                }
            }
        } else {
            Text(
                "The hero photo (★) fills the background. Pick a different hero in Photos.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

/**
 * Unified picker showing every photo that lives in the current collage. Hero is marked with a
 * star; every chip can be removed from the selection via the X badge so the user no longer
 * has to bounce back to the gallery picker just to drop a photo.
 */
@Composable
private fun PhotosInCollageSection(
    state: CollageUiState,
    selectedIndex: Int?,
    onSelectCard: (Int?) -> Unit,
    onResize: (Int, Float) -> Unit,
    onResizeMap: (Float) -> Unit,
    onResizeCaption: (Float) -> Unit,
    onPromote: (Int) -> Unit,
    onPickHero: (String) -> Unit,
    onRemoveFromCollage: (String) -> Unit,
    onRequestAddPhotos: () -> Unit,
) {
    EditorSection(title = "Photos") {
        Text(
            "Tap a photo to make it the hero. Tap × to drop it from the collage, or + to add more.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Build a hero-first ordered list of the photos that are actually part of the collage.
        val photosById = remember(state.allPhotos) { state.allPhotos.associateBy { it.id } }
        val heroId = state.options.backgroundPhotoId
        val orderedPhotos = remember(state.selectedIds, state.options.cards, heroId, state.allPhotos) {
            buildList {
                if (heroId != null) photosById[heroId]?.let(::add)
                state.options.cards.forEach { card ->
                    photosById[card.photoId]?.let { if (it.id != heroId) add(it) }
                }
                // Fallback: include any selected photo that ended up neither hero nor card
                // (shouldn't normally happen, but keep the list complete just in case).
                state.selectedPhotos.forEach { photo ->
                    if (none { it.id == photo.id }) add(photo)
                }
            }
        }

        val canAddMore = state.selectedIds.size < MAX_COLLAGE_PHOTOS &&
            state.allPhotos.size > state.selectedIds.size
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
        ) {
            items(orderedPhotos) { photo ->
                val isHero = photo.id == heroId
                // Translate a hero-tap into "make this the hero" and a card-tap into
                // "select this card for sizing".
                val cardIndex = state.options.cards.indexOfFirst { it.photoId == photo.id }
                val isSelected = !isHero && cardIndex == selectedIndex
                PhotoSelectionChip(
                    photo = photo,
                    isHero = isHero,
                    isSelected = isSelected,
                    onClick = {
                        if (isHero) return@PhotoSelectionChip
                        if (cardIndex >= 0) {
                            onSelectCard(if (selectedIndex == cardIndex) null else cardIndex)
                        } else {
                            onPickHero(photo.id)
                        }
                    },
                    onRemove = { onRemoveFromCollage(photo.id) },
                )
            }
            if (canAddMore) {
                item("add-photo") {
                    AddPhotoChip(onClick = onRequestAddPhotos)
                }
            }
        }

        val activeIndex = selectedIndex?.takeIf { it >= 0 && it in state.options.cards.indices }
        if (activeIndex != null) {
            val card = state.options.cards[activeIndex]
            Text("Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = card.widthFrac,
                onValueChange = { onResize(activeIndex, it) },
                valueRange = 0.14f..0.7f,
            )
            Button(
                onClick = { onPromote(activeIndex) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Use as background")
            }
        }

        if (selectedIndex == MAP_LAYER_INDEX && state.options.showMiniMap) {
            val mapWidthFrac = state.options.mapPlacement?.widthFrac ?: 0.4f
            Text(
                "Map size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = mapWidthFrac,
                onValueChange = onResizeMap,
                valueRange = 0.18f..0.7f,
            )
        }

        if (selectedIndex == CAPTION_LAYER_INDEX) {
            val captionWidthFrac = state.options.captionPlacement?.widthFrac ?: DEFAULT_CAPTION_WIDTH_FRAC
            Text(
                "Caption width",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = captionWidthFrac,
                onValueChange = onResizeCaption,
                valueRange = 0.3f..0.85f,
            )
        }
    }
}

@Composable
private fun PhotoSelectionChip(
    photo: GeoPhoto,
    isHero: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val borderColor = when {
        isHero -> MaterialTheme.colorScheme.tertiary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val borderWidth = if (isHero || isSelected) 3.dp else 0.dp
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = photo.thumbnailUri ?: photo.imageUri,
            contentDescription = photo.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (isHero) {
            // Star tag in the bottom-left so it doesn't collide with the × badge above.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = "Hero photo",
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        // × removal badge — sized big enough to be a comfortable tap target but visually
        // light so it doesn't drown the thumbnail.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove from collage",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PhotoCardTile(photo: GeoPhoto, darkStyle: Boolean) {
    // Mat colour matches the bitmap renderer's palette.surface so what you see on the live
    // preview is what the exported PNG produces.
    val matColor = if (darkStyle) DARK_SURFACE else Color.White
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .background(matColor, RoundedCornerShape(12.dp))
            .padding(6.dp),
    ) {
        AsyncImage(
            model = photo.thumbnailUri ?: photo.imageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * Live mini-map tile rendered inside the draggable layer. Unlike the previous snapshot-based
 * approach, this composes a real [GoogleMap] so that the "Map camera" zoom slider and the
 * "Tourist map" / "Dark style" toggles update the preview in real time. The persisted
 * `photo.mapSnapshotUri` is still used as a placeholder behind the live map so we never
 * flash an empty tile while tiles are loading.
 */
@Composable
private fun MiniMapTile(
    photo: GeoPhoto,
    mapZoom: Int,
    useTouristMapStyle: Boolean,
    darkStyle: Boolean,
) {
    val location = photo.location
    val pinColor = if (darkStyle) Color(0xFFFFC182) else MaterialTheme.colorScheme.tertiary
    val placeholderBg = if (darkStyle) DARK_MAP_BASE else MaterialTheme.colorScheme.surfaceContainerHigh
    val snapshot = photo.mapSnapshotUri
    val cameraPositionState = if (location != null) {
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(location.latitude, location.longitude),
                mapZoom.toFloat(),
            )
        }
    } else {
        null
    }
    if (location != null && cameraPositionState != null) {
        // Reposition + re-zoom whenever the user moves the slider or the hero photo (with
        // its location) changes underneath us.
        LaunchedEffect(mapZoom, location.latitude, location.longitude) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(location.latitude, location.longitude),
                mapZoom.toFloat(),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(placeholderBg),
    ) {
        // Placeholder = the persisted snapshot, if we have one. Shown behind the live map so
        // tile-loading flashes never leave the user with an empty rectangle.
        if (snapshot != null) {
            AsyncImage(
                model = snapshot,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Map",
                    color = if (darkStyle) Color(0xFFB8C8C1) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        // Live GoogleMap on top of the placeholder. All gestures are disabled so the
        // surrounding DraggableLayer's overlay can own touch input for drag-to-position.
        if (location != null && cameraPositionState != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    compassEnabled = false,
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    scrollGesturesEnabledDuringRotateOrZoom = false,
                ),
            ) {
                MapEffect(useTouristMapStyle, darkStyle) { gmap ->
                    gmap.setMapStyle(
                        if (useTouristMapStyle) MapStyleOptions(MAP_SNAPSHOT_STYLE_JSON) else null,
                    )
                }
            }
        }

        // The native Maps SDK has no built-in dark style and we don't ship a dedicated JSON
        // for it yet, so approximate the exported dark map by dimming the live tile.
        if (darkStyle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
        }
        // Pulse pin marking where the photo was taken. The single-photo map is centred on
        // the photo's location, so dead-centre is the correct spot.
        PulsePin(
            color = pinColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PulsePin(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        // ~4.5% of the shorter side keeps the pin visible without dominating the small
        // mini-map tile.
        val base = min(w, h) * 0.045f
        val center = Offset(cx, cy)
        drawCircle(color = color.copy(alpha = 0.16f), radius = base * 2.4f, center = center)
        drawCircle(color = color.copy(alpha = 0.32f), radius = base * 1.6f, center = center)
        drawCircle(color = Color.White, radius = base * 1.05f, center = center)
        drawCircle(color = color, radius = base * 0.6f, center = center)
        drawCircle(color = Color.White, radius = base * 0.22f, center = center)
    }
}

@Composable
private fun CaptionOverlay(
    lines: List<String>,
    darkStyle: Boolean,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = if (darkStyle) DARK_SURFACE else Color.White
    val titleColor = if (darkStyle) DARK_ON_SURFACE else Color(0xFF17201D)
    val subColor = if (darkStyle) DARK_ON_SURFACE_VARIANT else Color(0xFF50605A)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = surfaceColor,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            lines.forEachIndexed { index, line ->
                if (index == 0) {
                    Text(
                        line,
                        style = MaterialTheme.typography.titleSmall,
                        color = titleColor,
                        maxLines = 1,
                    )
                } else {
                    Text(
                        line,
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Kept in sync with CollageRenderer.Palette so the live Compose preview matches the
// exported bitmap pixel-for-pixel when "Dark style" is on.
private val DARK_SURFACE = Color(0xFF202426)
private val DARK_ON_SURFACE = Color(0xFFE8F0EC)
private val DARK_ON_SURFACE_VARIANT = Color(0xFFB8C8C1)
private val DARK_MAP_BASE = Color(0xFF273132)

/**
 * Caption-specific draggable wrapper. Unlike [DraggableLayer] the caption has a content-sized
 * height (so it can grow with more rows) and is anchored by its top-left corner. The width is
 * a fraction of the full canvas width, matching what the bitmap renderer does.
 */
@Composable
private fun DraggableCaptionLayer(
    canvasW: Int,
    canvasH: Int,
    originX: Float,
    originY: Float,
    widthFrac: Float,
    selected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = widthFrac * canvasW
    val originXPx = originX * canvasW
    val originYPx = originY * canvasH

    var pendingX by remember(originXPx) { mutableStateOf(0f) }
    var pendingY by remember(originYPx) { mutableStateOf(0f) }

    val leftPx = originXPx + pendingX
    val topPx = originYPx + pendingY

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .pointerInput(canvasW, canvasH, originX, originY, widthFrac) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDragEnd = {
                        val newX = ((originXPx + pendingX) / canvasW).coerceIn(0f, 1f)
                        val newY = ((originYPx + pendingY) / canvasH).coerceIn(0f, 1f)
                        onMove(newX, newY)
                    },
                    onDragCancel = {
                        pendingX = 0f
                        pendingY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        pendingX += dragAmount.x
                        pendingY += dragAmount.y
                    },
                )
            }
            .pointerInput(canvasW, canvasH, originX, originY, widthFrac) {
                detectTapGestures(onTap = { onSelect() })
            }
            .width(with(density) { widthPx.toDp() }),
    ) {
        content()
    }
}

/**
 * Generic draggable overlay positioned by normalized coordinates. The draggable element keeps
 * a local pending offset during the gesture and commits the final normalized centre on drag
 * end so we avoid spamming the renderer with intermediate states.
 */
@Composable
private fun DraggableLayer(
    canvasW: Int,
    canvasH: Int,
    centerX: Float,
    centerY: Float,
    widthFrac: Float,
    rotationDeg: Float,
    selected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val shorter = min(canvasW, canvasH)
    val sizePx = widthFrac * shorter
    val centerXPx = centerX * canvasW
    val centerYPx = centerY * canvasH

    // Pending offset accumulated during a drag — committed to the VM on drag end. We reset
    // it via `remember` keys whenever the upstream centre changes (e.g. after a commit, or
    // when the user picks a new hero photo).
    var pendingX by remember(centerXPx) { mutableStateOf(0f) }
    var pendingY by remember(centerYPx) { mutableStateOf(0f) }

    val leftPx = centerXPx - sizePx / 2f + pendingX
    val topPx = centerYPx - sizePx / 2f + pendingY

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .size(with(density) { sizePx.toDp() })
            .rotate(rotationDeg)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        content()
        // Touch-handling sibling drawn on top of `content()`. Putting the gesture detectors
        // here (rather than on the outer Box) is what lets us nest a live GoogleMap inside
        // the draggable layer: the overlay receives Main-pass pointer events first and
        // consumes them via the drag/tap detectors so the AndroidView underneath never sees
        // them.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(canvasW, canvasH, centerX, centerY, widthFrac) {
                    detectDragGestures(
                        onDragStart = { onSelect() },
                        onDragEnd = {
                            // Don't manually reset `pendingX/pendingY` here: the upstream
                            // `remember` keys clear them automatically once the VM-updated
                            // centre propagates back, avoiding a one-frame snap-back to the
                            // pre-drag position.
                            val newCx = ((centerXPx + pendingX) / canvasW).coerceIn(0f, 1f)
                            val newCy = ((centerYPx + pendingY) / canvasH).coerceIn(0f, 1f)
                            onMove(newCx, newCy)
                        },
                        onDragCancel = {
                            pendingX = 0f
                            pendingY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            pendingX += dragAmount.x
                            pendingY += dragAmount.y
                        },
                    )
                }
                .pointerInput(canvasW, canvasH, centerX, centerY, widthFrac) {
                    detectTapGestures(onTap = { onSelect() })
                },
        )
    }
}

@Composable
private fun HiddenMapSnapshotter(
    photo: GeoPhoto,
    zoom: Int,
    useTouristMapStyle: Boolean,
    onSnapshot: (android.graphics.Bitmap) -> Unit,
) {
    val location = photo.location ?: return
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), zoom.toFloat())
    }
    var captured by remember(photo.id, zoom, useTouristMapStyle) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0f),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                compassEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
            ),
        ) {
            MapEffect(photo.id, zoom, useTouristMapStyle) { gmap ->
                gmap.setMapStyle(if (useTouristMapStyle) MapStyleOptions(MAP_SNAPSHOT_STYLE_JSON) else null)
                gmap.setOnMapLoadedCallback {
                    if (captured) return@setOnMapLoadedCallback
                    gmap.snapshot { bitmap ->
                        if (captured) return@snapshot
                        if (bitmap != null) {
                            captured = true
                            onSnapshot(bitmap)
                        }
                    }
                }
            }
        }
    }
}

private const val MAP_SNAPSHOT_STYLE_JSON = """
[
  {"featureType":"poi","elementType":"labels","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.business","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.business","elementType":"labels","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.medical","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.school","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.sports_complex","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.government","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.place_of_worship","stylers":[{"visibility":"off"}]},
  {"featureType":"transit.station","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.attraction","stylers":[{"visibility":"on"}]}
]
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = null,
    )
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun MapCornerPicker(
    selected: CollageMapCorner,
    onSelect: (CollageMapCorner) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CornerCell(corner = CollageMapCorner.TOP_LEFT, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f).fillMaxHeight())
                CornerCell(corner = CollageMapCorner.TOP_RIGHT, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CornerCell(corner = CollageMapCorner.BOTTOM_LEFT, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f).fillMaxHeight())
                CornerCell(corner = CollageMapCorner.BOTTOM_RIGHT, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CornerCell(
    corner: CollageMapCorner,
    selected: CollageMapCorner,
    onSelect: (CollageMapCorner) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selected == corner
    val container = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = container,
        onClick = { onSelect(corner) },
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val dotAlignment = when (corner) {
                CollageMapCorner.TOP_LEFT -> Alignment.TopStart
                CollageMapCorner.TOP_RIGHT -> Alignment.TopEnd
                CollageMapCorner.BOTTOM_LEFT -> Alignment.BottomStart
                CollageMapCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
            }
            Surface(
                modifier = Modifier
                    .align(dotAlignment)
                    .size(width = 26.dp, height = 18.dp),
                shape = RoundedCornerShape(4.dp),
                color = onContainer.copy(alpha = if (isSelected) 1f else 0.55f),
            ) {}
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MapZoomRow(
    zoom: Int,
    onZoomChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Map camera",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (zoom <= 12) "Higher overview" else if (zoom >= 16) "Closer view" else "Balanced view",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onZoomChange(zoom - 1) },
            enabled = zoom > MIN_MAP_ZOOM,
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Higher map camera")
        }
        Text(
            zoom.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = { onZoomChange(zoom + 1) },
            enabled = zoom < MAX_MAP_ZOOM,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Closer map camera")
        }
    }
}

private const val MIN_MAP_ZOOM = 11
private const val MAX_MAP_ZOOM = 17

// Keep in sync with CollageViewModel.MAX_SELECTION — the picker enforces the same cap and the
// editor's "+" chip is hidden once the user reaches the limit.
private const val MAX_COLLAGE_PHOTOS = 12

@Composable
private fun AddPhotoChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = "Add a photo to the collage",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Bottom-sheet picker for adding more photos to the current collage without leaving the
 * editor. Lists every photo from the library that the user hasn't already added so taps map
 * directly to "add this photo" (selection toggling lives in [CollageViewModel.toggleSelection],
 * which auto-places it as a draggable card via the existing sync logic).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPhotosSheet(
    availablePhotos: List<GeoPhoto>,
    atSelectionLimit: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Add photos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subtitle = when {
                atSelectionLimit -> "You've reached the $MAX_COLLAGE_PHOTOS-photo limit. Remove one to add more."
                availablePhotos.isEmpty() -> "All your library photos are already in the collage."
                else -> "Tap a photo to add it. It will appear as a draggable card on the preview."
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (availablePhotos.isNotEmpty() && !atSelectionLimit) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(availablePhotos, key = { it.id }) { photo ->
                        AddSheetPhotoCell(
                            photo = photo,
                            onClick = { onPick(photo.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun AddSheetPhotoCell(photo: GeoPhoto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = photo.thumbnailUri ?: photo.imageUri,
            contentDescription = photo.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
