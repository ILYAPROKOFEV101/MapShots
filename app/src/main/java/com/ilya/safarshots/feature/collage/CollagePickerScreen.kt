package com.ilya.safarshots.feature.collage

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
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text("Collage") },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (state.selectedIds.isNotEmpty() && selectedPhotos.isNotEmpty()) {
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "${state.selectedIds.size} фото",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FilledTonalButton(onClick = { onEdit(state.selectedIds.toList(), state.options.template) }) {
                            Text("Continue")
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
                title = "Выберите фото для коллажа",
                subtitle = "Можно выбрать фото на карте или в галерее.",
                buttonText = "Открыть галерею",
                icon = Icons.Rounded.Collections,
                modifier = Modifier.padding(padding).fillMaxSize(),
                onButtonClick = onOpenGallery,
            )
            state.selectedIds.isEmpty() -> Column(Modifier.padding(padding).fillMaxSize()) {
                Text(
                    "Выберите 1–12 фото",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PhotoGrid(
                    groups = GalleryDateGrouper.group(state.allPhotos),
                    selectedIds = state.selectedIds,
                    selectionMode = true,
                    onPhotoClick = { viewModel.toggleSelection(it.id) },
                    onPhotoLongClick = { viewModel.toggleSelection(it.id) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> TemplatePickerContent(
                selectedCount = state.selectedIds.size,
                selected = state.options.template,
                onTemplate = {
                    viewModel.setTemplate(it)
                    onEdit(state.selectedIds.toList(), it)
                },
                modifier = Modifier.padding(padding),
            )
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
                    "$selectedCount фото готовы к коллажу",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Выберите шаблон. Схематичная карта экспортируется без Google tiles и без риска обрезать attribution.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(CollageTemplate.entries) { template ->
            TemplateCard(
                template = template,
                selected = template == selected,
                onClick = { onTemplate(template) },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: CollageTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val onContainer = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
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
                Text(template.title, style = MaterialTheme.typography.titleMedium, color = onContainer)
                Text(
                    template.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.8f),
                )
            }
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = onContainer.copy(alpha = 0.7f),
            )
        }
    }
}
