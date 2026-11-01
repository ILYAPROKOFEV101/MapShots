package com.ilya.safarshots.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ilya.safarshots.R
import com.ilya.safarshots.core.permissions.PermissionController
import com.ilya.safarshots.core.storage.ShareUtils
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.GeoPhoto
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onCreateCollage: (List<String>) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current

    LaunchedEffect(state.messageRes, resources) {
        val resId = state.messageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(resources.getString(resId))
        viewModel.onEvent(MapUiEvent.MessageShown)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCamera,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Rounded.AddAPhoto, contentDescription = null) },
                text = { Text(stringResource(R.string.map_capture)) },
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            when {
                !state.mapsConfigured -> NoMapsKeyState(modifier = Modifier.fillMaxSize())
                state.photos.isEmpty() -> EmptyState(
                    title = stringResource(R.string.map_empty_title),
                    subtitle = stringResource(R.string.map_empty_subtitle),
                    buttonText = stringResource(R.string.map_empty_button),
                    modifier = Modifier.align(Alignment.Center),
                    onButtonClick = onOpenCamera,
                )
                else -> PhotoMap(
                    photos = state.filteredPhotos(),
                    selectedIds = state.selectedPhotoIds,
                    onMarkerClick = { viewModel.onEvent(MapUiEvent.MarkerClicked(it)) },
                    onMarkerSelect = { viewModel.onEvent(MapUiEvent.AddToSelection(it)) },
                )
            }

            FloatingSearchBar(
                title = stringResource(R.string.app_name),
                onSettings = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FilterRow(state.filter) { viewModel.onEvent(MapUiEvent.FilterChanged(it)) }
            }

            if (state.selectedPhotoIds.size >= 2) {
                SelectionBar(
                    count = state.selectedPhotoIds.size,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    onClear = { viewModel.onEvent(MapUiEvent.ClearSelection) },
                    onCreate = { onCreateCollage(state.selectedPhotoIds.toList()) },
                )
            }
        }
    }

    val selectedPhoto = state.photos.firstOrNull { it.id == state.selectedPhotoId }
    if (selectedPhoto != null) {
        PhotoBottomSheet(
            photo = selectedPhoto,
            onDismiss = { viewModel.onEvent(MapUiEvent.DismissPhoto) },
            onOpen = { onOpenPhoto(selectedPhoto.id) },
            onAddToCollage = { viewModel.onEvent(MapUiEvent.AddToSelection(selectedPhoto.id)) },
            onFavorite = { viewModel.onEvent(MapUiEvent.ToggleFavorite(selectedPhoto.id)) },
            onShare = { context.startActivity(ShareUtils.shareImageIntent(context, selectedPhoto.imageUri)) },
            onDelete = { viewModel.onEvent(MapUiEvent.DeletePhoto(selectedPhoto.id)) },
        )
    }
}

@Composable
private fun FloatingSearchBar(
    title: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp), clip = false),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.map_settings_cd))
            }
        }
    }
}

@Composable
private fun FilterRow(selected: MapFilter, onFilter: (MapFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(MapFilter.entries.size) { index ->
            val filter = MapFilter.entries[index]
            ElevatedChip(
                selected = selected == filter,
                label = stringResource(filter.labelRes),
                onClick = { onFilter(filter) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElevatedChip(selected: Boolean, label: String, onClick: () -> Unit) {
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
private fun PhotoMap(
    photos: List<GeoPhoto>,
    selectedIds: Set<String>,
    onMarkerClick: (String) -> Unit,
    onMarkerSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    val noLocationLabel = stringResource(R.string.common_no_location)
    val photosWithLocation = photos.filter { it.location != null }
    val start = photosWithLocation.firstOrNull()?.location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            start?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(20.0, 0.0),
            if (start == null) 2f else 11.5f,
        )
    }
    LaunchedEffect(photosWithLocation.firstOrNull()?.id) {
        val location = photosWithLocation.firstOrNull()?.location ?: return@LaunchedEffect
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = PermissionController.hasAnyLocation(context),
        ),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        contentPadding = PaddingValues(top = 168.dp, bottom = 120.dp),
    ) {
        photosWithLocation.forEach { photo ->
            val location = photo.location ?: return@forEach
            val icon by rememberPhotoMarkerIcon(photo, photo.id in selectedIds)
            Marker(
                state = remember(photo.id) { MarkerState(position = LatLng(location.latitude, location.longitude)) },
                title = photo.displayTitle,
                snippet = photo.locationSubtitle ?: noLocationLabel,
                icon = icon,
                onClick = {
                    onMarkerClick(photo.id)
                    true
                },
                onInfoWindowClick = {
                    onMarkerSelect(photo.id)
                },
            )
        }
    }
}

@Composable
private fun NoMapsKeyState(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.map_no_api_title),
        subtitle = stringResource(R.string.map_no_api_subtitle),
        buttonText = null,
        icon = Icons.Rounded.Settings,
        modifier = modifier,
    )
}

@Composable
private fun SelectionBar(
    count: Int,
    modifier: Modifier,
    onClear: () -> Unit,
    onCreate: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.map_clear_cd))
            }
            Text(
                text = stringResource(R.string.photo_count, count),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onCreate) {
                Icon(Icons.Rounded.Collections, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.map_create_collage))
            }
        }
    }
}

private fun MapUiState.filteredPhotos(): List<GeoPhoto> {
    val now = Calendar.getInstance()
    val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    return photos.filter { photo ->
        when (filter) {
            MapFilter.ALL -> true
            MapFilter.TODAY -> {
                val captured = Calendar.getInstance().apply { timeInMillis = photo.capturedAtMillis }
                captured.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    captured.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            }
            MapFilter.WEEK -> photo.capturedAtMillis >= weekAgo.timeInMillis
            MapFilter.FAVORITES -> photo.isFavorite
            MapFilter.WITH_NOTES -> !photo.note.isNullOrBlank()
            MapFilter.NO_LOCATION -> photo.location == null
        }
    }
}
