package com.ilya.safarshots.feature.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilya.safarshots.R
import com.ilya.safarshots.core.locale.rememberDateLabels
import com.ilya.safarshots.design.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenCamera: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onCreateCollage: (List<String>) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = state.filteredPhotos()
    val dateLabels = rememberDateLabels()
    val groups = GalleryDateGrouper.group(filtered, dateLabels)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current
    val importPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importPhoto(it.toString()) }
    }

    LaunchedEffect(resources) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GalleryEffect.Message -> snackbarHostState.showSnackbar(resources.getString(effect.textRes))
                is GalleryEffect.PhotoImported -> onOpenPhoto(effect.photoId)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                actions = {
                    IconButton(onClick = { importPhotoPicker.launch("image/*") }) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = stringResource(R.string.gallery_import_cd))
                    }
                    IconButton(onClick = onOpenCamera) {
                        Icon(Icons.Rounded.AddAPhoto, contentDescription = stringResource(R.string.gallery_open_camera_cd))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (state.selectedIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = false),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalIconButton(onClick = { viewModel.onEvent(GalleryUiEvent.ClearSelection) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                        Text(
                            stringResource(R.string.photo_count, state.selectedIds.size),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FilledTonalButton(onClick = { onCreateCollage(state.selectedIds.toList()) }) {
                            Icon(Icons.Rounded.Collections, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.gallery_collage))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GalleryFilterRow(state.filter) { viewModel.onEvent(GalleryUiEvent.FilterChanged(it)) }
            if (state.photos.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.gallery_empty_title),
                    subtitle = stringResource(R.string.gallery_empty_subtitle),
                    buttonText = stringResource(R.string.gallery_empty_button),
                    modifier = Modifier.fillMaxSize(),
                    onButtonClick = onOpenCamera,
                )
            } else {
                PhotoGrid(
                    groups = groups,
                    selectedIds = state.selectedIds,
                    selectionMode = state.selectionMode,
                    onPhotoClick = { photo ->
                        if (state.selectionMode) {
                            viewModel.onEvent(GalleryUiEvent.ToggleSelection(photo.id))
                        } else {
                            onOpenPhoto(photo.id)
                        }
                    },
                    onPhotoLongClick = { photo -> viewModel.onEvent(GalleryUiEvent.ToggleSelection(photo.id)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryFilterRow(selected: GalleryFilter, onFilter: (GalleryFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(GalleryFilter.entries.size) { index ->
            val filter = GalleryFilter.entries[index]
            FilterChip(
                selected = selected == filter,
                onClick = { onFilter(filter) },
                label = { Text(stringResource(filter.labelRes)) },
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
    }
}

private fun GalleryUiState.filteredPhotos() = photos.filter { photo ->
    when (filter) {
        GalleryFilter.ALL -> true
        GalleryFilter.FAVORITES -> photo.isFavorite
        GalleryFilter.WITH_LOCATION -> photo.hasLocation
        GalleryFilter.WITHOUT_LOCATION -> !photo.hasLocation
    }
}
