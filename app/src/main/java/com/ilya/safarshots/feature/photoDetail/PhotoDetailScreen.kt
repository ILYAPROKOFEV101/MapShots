package com.ilya.safarshots.feature.photoDetail

import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ilya.safarshots.BuildConfig
import com.ilya.safarshots.R
import com.ilya.safarshots.core.common.DateLabels
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.core.locale.rememberDateLabels
import com.ilya.safarshots.core.permissions.PermissionController
import com.ilya.safarshots.core.storage.ShareUtils
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.GeoPhoto
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    onAddToCollage: (String) -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current
    val dateLabels = rememberDateLabels()
    val shareTitle = stringResource(R.string.share_image_title)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(resources) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PhotoDetailEffect.Deleted -> onBack()
                is PhotoDetailEffect.Message -> snackbarHostState.showSnackbar(resources.getString(effect.textRes))
            }
        }
    }

    val photo = state.photo
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(photo?.friendlyTitle(dateLabels) ?: stringResource(R.string.photo_detail_default_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (photo != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (photo.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(R.string.photo_detail_favorite_cd),
                                tint = if (photo.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (photo == null) {
            EmptyState(
                title = stringResource(R.string.photo_detail_not_found_title),
                subtitle = stringResource(R.string.photo_detail_not_found_subtitle),
                buttonText = null,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = photo.imageUri,
                contentDescription = photo.displayTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )
            InfoCard(
                photo = photo,
                showPreciseCoordinates = state.showPreciseCoordinates,
                onEditNote = viewModel::openNoteDialog,
                onEditLocation = viewModel::openLocationDialog,
            )
            ActionRows(
                onShare = { context.startActivity(ShareUtils.shareImageIntent(context, photo.imageUri, shareTitle)) },
                onAddToCollage = { onAddToCollage(photo.id) },
                onOpenMap = onOpenMap,
                onDelete = { viewModel.showDeleteDialog(true) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (state.showNoteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNoteDialog,
            title = { Text(stringResource(R.string.photo_detail_note_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = state.noteDraft,
                    onValueChange = viewModel::setNoteDraft,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            },
            confirmButton = { TextButton(onClick = viewModel::saveNote) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissNoteDialog) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text(stringResource(R.string.photo_detail_delete_dialog_title)) },
            text = { Text(stringResource(R.string.photo_detail_delete_dialog_text)) },
            confirmButton = { TextButton(onClick = viewModel::deletePhoto) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { viewModel.showDeleteDialog(false) }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (state.showLocationDialog) {
        val mapsEnabled = !BuildConfig.MAPS_API_KEY.isBlank()
        val startPoint = remember(state.showLocationDialog) {
            parseDraftPoint(state.latitudeDraft, state.longitudeDraft)
                ?: state.photo?.location?.let { LatLng(it.latitude, it.longitude) }
        }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                startPoint ?: DEFAULT_MAP_PICKER_POINT,
                if (startPoint == null) 2f else 14f,
            )
        }
        var pickedPoint by remember(state.showLocationDialog) { mutableStateOf(startPoint) }
        val draftPoint = remember(state.latitudeDraft, state.longitudeDraft) {
            parseDraftPoint(state.latitudeDraft, state.longitudeDraft)
        }
        LaunchedEffect(draftPoint) {
            if (draftPoint != null && draftPoint != pickedPoint) {
                pickedPoint = draftPoint
            }
        }
        LaunchedEffect(pickedPoint) {
            pickedPoint?.let { point ->
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(point, 14f))
            }
        }
        val canSave = if (mapsEnabled) {
            pickedPoint != null
        } else {
            parseDraftPoint(state.latitudeDraft, state.longitudeDraft) != null
        }
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { nextValue ->
                // Prevent accidental sheet close while panning/zooming the embedded map.
                nextValue != SheetValue.Hidden
            },
        )
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissLocationDialog,
            sheetState = sheetState,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.photo_detail_location_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.photo_detail_location_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!mapsEnabled) {
                    Text(
                        text = stringResource(R.string.photo_detail_location_no_map),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.latitudeDraft,
                        onValueChange = viewModel::setLatitudeDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.photo_detail_latitude_label)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.longitudeDraft,
                        onValueChange = viewModel::setLongitudeDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.photo_detail_longitude_label)) },
                        singleLine = true,
                    )
                } else {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = PermissionController.hasAnyLocation(context),
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = true,
                        ),
                        // Use a MapView subclass that pre-emptively asks its parent
                        // (Compose's AndroidViewHolder) not to intercept touches as soon as
                        // ACTION_DOWN arrives. Compose's internal PointerInteropFilter listens
                        // to this and starts dispatching subsequent MOVE events during the
                        // Initial pointer pass, so the MapView claims the gesture before the
                        // surrounding ModalBottomSheet's anchoredDraggable (which runs in the
                        // Main pass) can steal vertical pans for sheet drag-to-dismiss.
                        mapViewFactory = ::NonInterceptableMapView,
                        onMapClick = { latLng ->
                            pickedPoint = latLng
                            viewModel.setLatitudeDraft(formatCoordinate(latLng.latitude))
                            viewModel.setLongitudeDraft(formatCoordinate(latLng.longitude))
                        },
                    ) {
                        pickedPoint?.let { point ->
                            Marker(
                                state = remember(point) { MarkerState(position = point) },
                                title = stringResource(R.string.photo_detail_picker_marker_title),
                            )
                        }
                    }
                    val noPointLabel = stringResource(R.string.photo_detail_picker_no_point)
                    val selectedPointLabel = pickedPoint?.let { point ->
                        DateTimeFormatters.coordinates(point.latitude, point.longitude)
                    } ?: noPointLabel
                    Text(
                        text = selectedPointLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = state.addressDraft,
                    onValueChange = viewModel::setAddressDraft,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.photo_detail_address_label)) },
                    minLines = 2,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::dismissLocationDialog,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            if (mapsEnabled && pickedPoint != null) {
                                viewModel.setLatitudeDraft(formatCoordinate(pickedPoint!!.latitude))
                                viewModel.setLongitudeDraft(formatCoordinate(pickedPoint!!.longitude))
                            }
                            viewModel.saveLocation()
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
                if (state.photo?.location != null) {
                    TextButton(onClick = viewModel::clearLocation) { Text(stringResource(R.string.photo_detail_remove_location)) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun parseDraftPoint(latitudeDraft: String, longitudeDraft: String): LatLng? {
    val lat = latitudeDraft.trim().replace(',', '.').toDoubleOrNull() ?: return null
    val lng = longitudeDraft.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return LatLng(lat, lng)
}

private fun formatCoordinate(value: Double): String =
    String.format(Locale.US, "%.6f", value)

private val DEFAULT_MAP_PICKER_POINT = LatLng(20.0, 0.0)

/**
 * MapView that asks its parent not to intercept touch events while a gesture is in
 * progress. Combined with Compose's `AndroidView` interop (which wires
 * `View.requestDisallowInterceptTouchEvent` to the internal `PointerInteropFilter`),
 * this guarantees that vertical drags over the embedded map go to the map and not to
 * an enclosing `ModalBottomSheet`'s drag-to-dismiss gesture.
 */
private class NonInterceptableMapView(context: Context, options: GoogleMapOptions) :
    MapView(context, options) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }
}

@Composable
private fun InfoCard(
    photo: GeoPhoto,
    showPreciseCoordinates: Boolean,
    onEditNote: () -> Unit,
    onEditLocation: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                DateTimeFormatters.photoDateTime(photo.capturedAtMillis),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    photo.locationLabel(showPreciseCoordinates),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            photo.location?.accuracyMeters?.let {
                Text(
                    stringResource(R.string.photo_detail_accuracy, it.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val note = photo.note?.takeIf { it.isNotBlank() }
            if (note != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        note,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            OutlinedButton(onClick = onEditNote) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (note == null) R.string.photo_detail_add_note else R.string.photo_detail_edit_note))
            }
            OutlinedButton(onClick = onEditLocation) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (photo.location == null) R.string.photo_detail_set_location else R.string.photo_detail_edit_location))
            }
        }
    }
}

@Composable
private fun GeoPhoto.locationLabel(showPreciseCoordinates: Boolean): String {
    val location = location ?: return stringResource(R.string.photo_detail_no_location)
    location.addressText?.let { return it }
    return if (showPreciseCoordinates) {
        DateTimeFormatters.coordinates(location.latitude, location.longitude)
    } else {
        stringResource(R.string.photo_detail_location_saved_label)
    }
}

private fun GeoPhoto.friendlyTitle(labels: DateLabels): String {
    note?.takeIf { it.isNotBlank() }?.let { return it }
    location?.addressText?.takeIf { it.isNotBlank() }?.let { return it }
    return DateTimeFormatters.galleryGroup(capturedAtMillis, labels)
}

@Composable
private fun ActionRows(
    onShare: () -> Unit,
    onAddToCollage: () -> Unit,
    onOpenMap: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.IosShare, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.photo_detail_share))
            }
            FilledTonalButton(onClick = onAddToCollage, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.photo_detail_collage))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenMap, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.photo_detail_map))
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.photo_detail_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
