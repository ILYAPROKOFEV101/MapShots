package com.ilya.safarshots.feature.collage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilya.safarshots.R
import com.ilya.safarshots.core.locale.rememberDateLabels
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.feature.gallery.GalleryDateGrouper
import com.ilya.safarshots.feature.gallery.PhotoGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollagePickerScreen(
    onOpenGallery: () -> Unit,
    onEdit: (List<String>, CollageTemplate) -> Unit,
    viewModel: CollageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedPhotos = state.selectedPhotos
    val dateLabels = rememberDateLabels()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Two-step flow: pick photos (multi-select) → optionally choose a template.
    // Tracked locally so it's restored across config changes but doesn't pollute the VM.
    var showTemplateStep by rememberSaveable { mutableStateOf(false) }

    // Auto-return to the photo grid if the user clears the selection while on the template step.
    if (showTemplateStep && state.selectedIds.isEmpty()) {
        showTemplateStep = false
    }
    BackHandler(enabled = showTemplateStep) { showTemplateStep = false }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(if (showTemplateStep) R.string.collage_template_step_title else R.string.collage_title)) },
                navigationIcon = {
                    if (showTemplateStep) {
                        IconButton(onClick = { showTemplateStep = false }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.collage_back_to_photos_cd))
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
        bottomBar = {
            if (!showTemplateStep && state.selectedIds.isNotEmpty() && selectedPhotos.isNotEmpty()) {
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
                        FilledTonalIconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.collage_clear_selection_cd))
                        }
                        Text(
                            stringResource(R.string.photo_count, state.selectedIds.size),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FilledTonalButton(onClick = { showTemplateStep = true }) {
                            Text(stringResource(R.string.action_continue))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.allPhotos.isEmpty() -> EmptyState(
                title = stringResource(R.string.collage_picker_empty_title),
                subtitle = stringResource(R.string.collage_picker_empty_subtitle),
                buttonText = stringResource(R.string.collage_picker_empty_button),
                icon = Icons.Rounded.Collections,
                modifier = Modifier.padding(padding).fillMaxSize(),
                onButtonClick = onOpenGallery,
            )
            showTemplateStep -> TemplatePickerContent(
                selectedCount = state.selectedIds.size,
                selected = state.options.template,
                onTemplate = {
                    viewModel.setTemplate(it)
                    onEdit(state.selectedIds.toList(), it)
                },
                modifier = Modifier.padding(padding),
            )
            else -> Column(Modifier.padding(padding).fillMaxSize()) {
                Text(
                    if (state.selectedIds.isEmpty()) {
                        stringResource(R.string.collage_pick_hint)
                    } else {
                        stringResource(R.string.collage_pick_progress, state.selectedIds.size)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PhotoGrid(
                    groups = GalleryDateGrouper.group(state.allPhotos, dateLabels),
                    selectedIds = state.selectedIds,
                    selectionMode = true,
                    onPhotoClick = { viewModel.toggleSelection(it.id) },
                    onPhotoLongClick = { viewModel.toggleSelection(it.id) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TemplatePickerContent(
    selectedCount: Int,
    selected: CollageTemplate,
    onTemplate: (CollageTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.collage_template_ready, selectedCount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.collage_template_pick_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(CollageTemplate.entries) { template ->
            TemplateCard(
                template = template,
                selected = template == selected,
                enabled = selectedCount >= template.minimumPhotos,
                onClick = { onTemplate(template) },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: CollageTemplate,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected && enabled) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val onContainer = if (selected && enabled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(template.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainer.copy(alpha = if (enabled) 1f else 0.55f),
                )
                Text(
                    stringResource(template.subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = if (enabled) 0.8f else 0.5f),
                )
                if (!enabled) {
                    Text(
                        stringResource(R.string.collage_template_more_photos_needed, template.minimumPhotos),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = onContainer.copy(alpha = if (enabled) 0.7f else 0.3f),
            )
        }
    }
}
