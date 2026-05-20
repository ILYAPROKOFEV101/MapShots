package com.ilya.safarshots.feature.photoDetail

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ilya.safarshots.BuildConfig
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.core.storage.ShareUtils
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.GeoPhoto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    onAddToCollage: (String) -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PhotoDetailEffect.Deleted -> onBack()
                is PhotoDetailEffect.Message -> Unit
            }
        }
    }

    val photo = state.photo
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text(photo?.friendlyTitle() ?: "Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (photo != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (photo.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
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
                title = "Фото не найдено",
                subtitle = "Возможно, оно уже удалено.",
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
            )
            MiniMapCard(photo)
            ActionRows(
                onShare = { context.startActivity(ShareUtils.shareImageIntent(context, photo.imageUri)) },
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
            title = { Text("Заметка") },
            text = {
                OutlinedTextField(
                    value = state.noteDraft,
                    onValueChange = viewModel::setNoteDraft,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            },
            confirmButton = { TextButton(onClick = viewModel::saveNote) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = viewModel::dismissNoteDialog) { Text("Отмена") } },
        )
    }
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Удалить фото?") },
            text = { Text("Запись и локальные файлы будут удалены безвозвратно.") },
            confirmButton = { TextButton(onClick = viewModel::deletePhoto) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { viewModel.showDeleteDialog(false) }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun InfoCard(
    photo: GeoPhoto,
    showPreciseCoordinates: Boolean,
    onEditNote: () -> Unit,
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
                    "Accuracy ${it.toInt()} m",
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
                Text(if (note == null) "Add note" else "Edit note")
            }
        }
    }
}

private fun GeoPhoto.locationLabel(showPreciseCoordinates: Boolean): String {
    val location = location ?: return "No location"
    location.addressText?.let { return it }
    return if (showPreciseCoordinates) {
        DateTimeFormatters.coordinates(location.latitude, location.longitude)
    } else {
        "Location saved"
    }
}

private fun GeoPhoto.friendlyTitle(): String {
    note?.takeIf { it.isNotBlank() }?.let { return it }
    location?.addressText?.takeIf { it.isNotBlank() }?.let { return it }
    return DateTimeFormatters.galleryGroup(capturedAtMillis)
}

@Composable
private fun MiniMapCard(photo: GeoPhoto) {
    val location = photo.location ?: return
    if (BuildConfig.MAPS_API_KEY.isBlank()) return
    val latLng = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, 14f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                Marker(
                    state = remember(latLng) { MarkerState(position = latLng) },
                    title = photo.displayTitle,
                )
            }
        }
    }
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
                Text("Share")
            }
            FilledTonalButton(onClick = onAddToCollage, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Collage")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenMap, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Map")
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
