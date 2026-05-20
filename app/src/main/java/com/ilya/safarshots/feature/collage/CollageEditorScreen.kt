package com.ilya.safarshots.feature.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.GeoPhoto

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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PreviewCard(state, onMapSnapshot = viewModel::onMapSnapshotCaptured)
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
            if (state.mainPhoto?.location != null) {
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
            if (state.isRendering || state.isExporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
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
