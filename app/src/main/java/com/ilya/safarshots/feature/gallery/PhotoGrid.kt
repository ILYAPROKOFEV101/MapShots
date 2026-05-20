package com.ilya.safarshots.feature.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.domain.model.GeoPhoto

@Composable
fun PhotoGrid(
    groups: List<PhotoDateGroup>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onPhotoClick: (GeoPhoto) -> Unit,
    onPhotoLongClick: (GeoPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(132.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { group ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(group.photos, key = { it.id }) { photo ->
                PhotoGridItem(
                    photo = photo,
                    selected = photo.id in selectedIds,
                    selectionMode = selectionMode,
                    onClick = { onPhotoClick(photo) },
                    onLongClick = { onPhotoLongClick(photo) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: GeoPhoto,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val cornerRadius = if (selected) 16.dp else 22.dp
    val outerPadding = if (selected) 4.dp else 0.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .padding(outerPadding)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(cornerRadius),
            )
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = photo.thumbnailUri ?: photo.imageUri,
            contentDescription = photo.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (photo.hasLocation) BadgeIcon(Icons.Rounded.LocationOn)
            if (photo.isFavorite) BadgeIcon(Icons.Rounded.Favorite, tint = MaterialTheme.colorScheme.tertiary)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        ) {
            Text(
                DateTimeFormatters.timeOnly(photo.capturedAtMillis),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                photo.displaySubtitle,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        if (selectionMode || selected) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f),
                border = if (selected) null else androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Color.White) {
    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.4f)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(5.dp).size(16.dp))
    }
}
