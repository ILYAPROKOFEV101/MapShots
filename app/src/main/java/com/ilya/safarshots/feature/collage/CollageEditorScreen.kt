package com.ilya.safarshots.feature.collage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ilya.safarshots.R
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.core.maps.GestureSafeMapView
import com.ilya.safarshots.core.permissions.PermissionController
import com.ilya.safarshots.design.EmptyState
import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageBackgroundMode
import com.ilya.safarshots.domain.model.CollageCaptionStyle
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.CollageTextLayer
import com.ilya.safarshots.domain.model.CollageTextShape
import com.ilya.safarshots.domain.model.GeoPhoto
import kotlin.math.min
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------

// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    onBack: () -> Unit,
    viewModel: CollageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.renderPreview() }
    LaunchedEffect(state.message, resources) {
        val message = state.message ?: return@LaunchedEffect
        val text = if (message.intArg != null) {
            resources.getString(message.resId, message.intArg)
        } else {
            resources.getString(message.resId)
        }
        snackbarHostState.showSnackbar(text)
        viewModel.messageShown()
    }

    var activeTab by rememberSaveable { mutableStateOf(EditorTab.PHOTOS) }
    var selectedLayerIndex by remember { mutableStateOf<Int?>(null) }
    var showAddPhotosSheet by remember { mutableStateOf(false) }
    // Whether the tool drawer's pane area is expanded. Collapsing it gives the preview
    // most of the screen for comfortable editing; the tabs remain visible so the user
    // can re-expand into any pane with a single tap.
    var drawerExpanded by rememberSaveable { mutableStateOf(true) }

    // If the underlying cards list shrinks (photo dropped), forget any stale selection
    // pointing at an index that no longer exists.
    LaunchedEffect(state.options.cards.size) {
        val idx = selectedLayerIndex
        if (idx != null && idx >= 0 && idx !in state.options.cards.indices) {
            selectedLayerIndex = null
        }
    }
    LaunchedEffect(state.options.showCaption) {
        if (!state.options.showCaption && selectedLayerIndex == CAPTION_LAYER_INDEX) {
            selectedLayerIndex = null
        }
    }
    LaunchedEffect(state.options.textLayers.size) {
        val textIndex = selectedTextLayerIndex(selectedLayerIndex)
        if (textIndex != null && textIndex !in state.options.textLayers.indices) {
            selectedLayerIndex = null
        }
    }
    // Open the matching tool when a canvas layer is selected. In particular, dragging
    // the map keeps the Map controls visible instead of unexpectedly jumping to Photos.
    LaunchedEffect(selectedLayerIndex) {
        when {
            selectedLayerIndex == MAP_LAYER_INDEX -> activeTab = EditorTab.MAP
            selectedLayerIndex == CAPTION_LAYER_INDEX || selectedTextLayerIndex(selectedLayerIndex) != null -> {
                activeTab = EditorTab.INFO
            }
            selectedLayerIndex == null -> Unit
            else -> activeTab = EditorTab.PHOTOS
        }
        if (selectedLayerIndex != null) {
            drawerExpanded = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EditorTopBar(
                photoCount = state.selectedIds.size,
                canExport = state.selectedIds.isNotEmpty() && !state.isExporting,
                isExporting = state.isExporting,
                onBack = onBack,
                onExport = viewModel::export,
            )
        },
    ) { padding ->
        if (state.selectedIds.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.collage_picker_empty_title),
                subtitle = stringResource(R.string.collage_picker_empty_subtitle),
                buttonText = null,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PreviewStage(
                state = state,
                onMapSnapshot = viewModel::onMapSnapshotCaptured,
                selectedLayerIndex = selectedLayerIndex,
                onSelectLayer = { selectedLayerIndex = it },
                onTransformCard = viewModel::transformCard,
                onTransformMap = viewModel::transformMap,
                onTransformCaption = viewModel::transformCaption,
                onTransformTextLayer = viewModel::transformTextLayer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            ToolDrawer(
                state = state,
                activeTab = activeTab,
                onTabChange = {
                    activeTab = it
                    selectedLayerIndex = null
                    drawerExpanded = true
                },
                selectedLayerIndex = selectedLayerIndex,
                onSelectionChange = { selectedLayerIndex = it },
                onClearSelection = { selectedLayerIndex = null },
                onRequestAddPhotos = { showAddPhotosSheet = true },
                expanded = drawerExpanded,
                onToggleExpanded = { drawerExpanded = !drawerExpanded },
                viewModel = viewModel,
            )
        }

        if (showAddPhotosSheet) {
            AddPhotosSheet(
                availablePhotos = state.allPhotos.filter { it.id !in state.selectedIds },
                atSelectionLimit = state.selectedIds.size >= MAX_COLLAGE_PHOTOS,
                onPick = { viewModel.toggleSelection(it) },
                onDismiss = { showAddPhotosSheet = false },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Top bar: back button, compact title with photo-count chip, and a primary "Export" CTA.
// The CTA gets the prominent FilledTonalButton treatment (vs. an icon-only button on the
// original screen) so it's the most obvious next action even before the user has scrolled.
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    photoCount: Int,
    canExport: Boolean,
    isExporting: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.collage_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (photoCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = "$photoCount/$MAX_COLLAGE_PHOTOS",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onExport,
                enabled = canExport,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(
                    Icons.Rounded.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (isExporting) R.string.collage_editor_saving else R.string.collage_editor_export))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

// ---------------------------------------------------------------------------------------
// The preview stage. Renders the live, draggable preview on a soft gradient background so
// the canvas reads as the "stage" of the editor (à la Canva / VSCO). The card itself keeps
// the same draggable behaviour as before — only its framing changed.
// ---------------------------------------------------------------------------------------

@Composable
private fun PreviewStage(
    state: CollageUiState,
    onMapSnapshot: (String, android.graphics.Bitmap) -> Unit,
    selectedLayerIndex: Int?,
    onSelectLayer: (Int?) -> Unit,
    onTransformCard: (Int, Float, Float, Float, Float) -> Unit,
    onTransformMap: (Float, Float, Float, Float) -> Unit,
    onTransformCaption: (Float, Float, Float, Float) -> Unit,
    onTransformTextLayer: (Int, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    )
    Box(
        modifier = modifier
            .background(gradient)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Fit the preview card inside the available stage box while preserving the
            // requested aspect ratio. We compute the largest box that fits and let Compose
            // handle the centring.
            val ratio = state.options.aspectRatio.width.toFloat() /
                state.options.aspectRatio.height.toFloat()
            val maxW = maxWidth
            val maxH = maxHeight
            val widthByHeight = maxH * ratio
            val targetWidth = if (widthByHeight <= maxW) widthByHeight else maxW
            Surface(
                modifier = Modifier
                    .width(targetWidth)
                    .aspectRatio(ratio)
                    .shadow(elevation = 18.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
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
                    if (state.options.template == CollageTemplate.PHOTO_WITH_MAP &&
                        state.selectedIds.isNotEmpty()
                    ) {
                        InteractivePhotoMapPreview(
                            state = state,
                            selectedCardIndex = selectedLayerIndex,
                            onSelectCard = onSelectLayer,
                            onTransformCard = onTransformCard,
                            onTransformMap = onTransformMap,
                            onTransformCaption = onTransformCaption,
                            onTransformTextLayer = onTransformTextLayer,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    } else {
                        val preview = state.preview
                        if (preview != null) {
                            InteractivePhotoLayoutPreview(
                                state = state,
                                selectedLayerIndex = selectedLayerIndex,
                                onSelectLayer = onSelectLayer,
                                onClearSelection = { onSelectLayer(null) },
                                onTransformMap = onTransformMap,
                                onTransformCaption = onTransformCaption,
                                onTransformTextLayer = onTransformTextLayer,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (state.isRendering) R.string.collage_editor_rendering
                                    else R.string.collage_editor_preview,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Floating progress affordance pinned to the top of the stage — keeps the
            // preview itself unobstructed while still signalling background work.
            if (state.isRendering || state.isExporting) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(min = 140.dp)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.isExporting) R.string.collage_editor_exporting
                                else R.string.collage_editor_rendering,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// The bottom tool drawer. A persistent, elevated panel that hosts a tab bar + pane content.
// Each pane is self-contained so adding a new tool category is a single `when` arm.
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolDrawer(
    state: CollageUiState,
    activeTab: EditorTab,
    onTabChange: (EditorTab) -> Unit,
    selectedLayerIndex: Int?,
    onSelectionChange: (Int?) -> Unit,
    onClearSelection: () -> Unit,
    onRequestAddPhotos: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    viewModel: CollageViewModel,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 1.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            // Tappable handle area — collapses the pane to free up screen space for
            // the preview, or re-expands it when the user wants to edit again. The
            // handle bar fades to a stronger accent color while collapsed so the
            // affordance is obvious.
            val handleColor = if (expanded) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(top = 6.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(handleColor),
                )
            }
            ToolTabBar(
                selected = activeTab,
                onSelect = onTabChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                    },
                    label = "tool-pane",
                ) { tab ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                            when (tab) {
                                EditorTab.PHOTOS -> PhotosPane(
                                    state = state,
                                    selectedLayerIndex = selectedLayerIndex,
                                    onSelectLayer = onSelectionChange,
                                    onResize = viewModel::resizeCard,
                                    onResizeMap = viewModel::resizeMap,
                                    onResizeCaption = viewModel::resizeCaption,
                                    onPromote = { idx ->
                                        viewModel.promoteCardToHero(idx)
                                        onClearSelection()
                                    },
                                    onPickHero = viewModel::setBackgroundPhotoId,
                                    onRemoveFromCollage = { photoId ->
                                        viewModel.toggleSelection(photoId)
                                        onClearSelection()
                                    },
                                    onRequestAddPhotos = onRequestAddPhotos,
                                    onClearSelection = onClearSelection,
                                )
                                EditorTab.LAYOUT -> LayoutPane(
                                    state = state,
                                    onTemplateChange = viewModel::setTemplate,
                                    onAspectChange = viewModel::setAspectRatio,
                                )
                                EditorTab.STYLE -> StylePane(
                                    state = state,
                                    onBackgroundModeChange = viewModel::setBackgroundMode,
                                    onBackgroundColorChange = viewModel::setBackgroundColor,
                                    onDarkStyleChange = viewModel::setDarkStyle,
                                )
                                EditorTab.MAP -> MapPane(
                                    state = state,
                                    onShowMiniMapChange = viewModel::setShowMiniMap,
                                    onMapPhotoChange = viewModel::setMapPhotoId,
                                    onSaveMapLocation = viewModel::saveManualMapLocation,
                                    onMapCornerChange = viewModel::setMapCorner,
                                    onMapSizeChange = viewModel::resizeMap,
                                    onMapZoomChange = viewModel::setMapZoom,
                                    onUseTouristMapStyleChange = viewModel::setUseTouristMapStyle,
                                )
                                EditorTab.INFO -> InfoPane(
                                    state = state,
                                    captionSelected = selectedLayerIndex == CAPTION_LAYER_INDEX,
                                    selectedTextLayerIndex = selectedTextLayerIndex(selectedLayerIndex),
                                    onSelectTextLayer = { index ->
                                        onSelectionChange(index?.let(::textLayerSelectionIndex))
                                    },
                                    onAddTextLayer = { text ->
                                        val index = viewModel.addTextLayer(text)
                                        onSelectionChange(textLayerSelectionIndex(index))
                                    },
                                    onRemoveTextLayer = { index ->
                                        viewModel.removeTextLayer(index)
                                        onClearSelection()
                                    },
                                    onTextLayerTextChange = viewModel::setTextLayerText,
                                    onTextLayerWidthChange = viewModel::resizeTextLayer,
                                    onTextLayerSizeChange = viewModel::setTextLayerSize,
                                    onTextLayerTextColorChange = viewModel::setTextLayerTextColor,
                                    onTextLayerBackgroundColorChange = viewModel::setTextLayerBackgroundColor,
                                    onTextLayerBackgroundOpacityChange = viewModel::setTextLayerBackgroundOpacity,
                                    onTextLayerBorderColorChange = viewModel::setTextLayerBorderColor,
                                    onTextLayerBorderEnabledChange = viewModel::setTextLayerBorderEnabled,
                                    onTextLayerCornerRadiusChange = viewModel::setTextLayerCornerRadius,
                                    onTextLayerShapeChange = viewModel::setTextLayerShape,
                                    onShowCaptionChange = viewModel::setShowCaption,
                                    onCustomCaptionChange = viewModel::setCustomCaption,
                                    onCaptionWidthChange = viewModel::resizeCaption,
                                    onCaptionTextSizeChange = viewModel::setCaptionTextSizeScale,
                                    onCaptionTextColorChange = viewModel::setCaptionTextColor,
                                    onCaptionBackgroundColorChange = viewModel::setCaptionBackgroundColor,
                                    onCaptionBackgroundOpacityChange = viewModel::setCaptionBackgroundOpacity,
                                    onCaptionBorderColorChange = viewModel::setCaptionBorderColor,
                                    onCaptionBorderEnabledChange = viewModel::setCaptionBorderEnabled,
                                    onCaptionCornerRadiusChange = viewModel::setCaptionCornerRadius,
                                    onCaptionShapeChange = viewModel::setCaptionShape,
                                    onShowDateChange = viewModel::setShowDate,
                                    onShowCoordinatesChange = viewModel::setShowCoordinates,
                                    onLocationLabelDraftChange = viewModel::setLocationLabelDraft,
                                    onSaveLocationLabel = viewModel::saveLocationLabel,
                                )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Tab bar. A custom pill-style segmented control. Sits inside the drawer so it stays close
// to the panes it controls and remains tappable even with the IME up.
// ---------------------------------------------------------------------------------------

private enum class EditorTab(@androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    PHOTOS(R.string.collage_editor_tab_photos, Icons.Rounded.Photo),
    LAYOUT(R.string.collage_editor_tab_layout, Icons.Rounded.GridView),
    STYLE(R.string.collage_editor_tab_style, Icons.Rounded.Palette),
    MAP(R.string.collage_editor_tab_map, Icons.Rounded.Map),
    INFO(R.string.collage_editor_tab_info, Icons.Rounded.TextFields),
}

@Composable
private fun ToolTabBar(
    selected: EditorTab,
    onSelect: (EditorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = EditorTab.entries.indexOf(selected),
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
    ) {
        EditorTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val tabLabel = stringResource(tab.labelRes)
            Tab(
                selected = isSelected,
                onClick = { onSelect(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tabLabel,
                        modifier = Modifier.size(20.dp),
                    )
                },
                text = {
                    Text(
                        text = tabLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// PHOTOS pane: shows every photo in the collage as a chip strip with hero-star + remove ×
// badges and a "+ add" affordance. When a card layer is selected, surfaces a contextual
// size slider and a "use as background" button.
// ---------------------------------------------------------------------------------------

@Composable
private fun PhotosPane(
    state: CollageUiState,
    selectedLayerIndex: Int?,
    onSelectLayer: (Int?) -> Unit,
    onResize: (Int, Float) -> Unit,
    onResizeMap: (Float) -> Unit,
    onResizeCaption: (Float) -> Unit,
    onPromote: (Int) -> Unit,
    onPickHero: (String) -> Unit,
    onRemoveFromCollage: (String) -> Unit,
    onRequestAddPhotos: () -> Unit,
    onClearSelection: () -> Unit,
) {
    PaneHeader(
        title = stringResource(R.string.collage_editor_photos_title),
        subtitle = stringResource(R.string.collage_editor_photos_subtitle),
    )

    val photosById = remember(state.allPhotos) { state.allPhotos.associateBy { it.id } }
    val heroId = state.options.backgroundPhotoId
    val orderedPhotos = remember(state.selectedIds, state.options.cards, heroId, state.allPhotos) {
        buildList {
            if (heroId != null) photosById[heroId]?.let(::add)
            state.options.cards.forEach { card ->
                photosById[card.photoId]?.let { if (it.id != heroId) add(it) }
            }
            state.selectedPhotos.forEach { photo ->
                if (none { it.id == photo.id }) add(photo)
            }
        }
    }
    val canAddMore = state.selectedIds.size < MAX_COLLAGE_PHOTOS &&
        state.allPhotos.size > state.selectedIds.size

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
    ) {
        items(orderedPhotos) { photo ->
            val isHero = photo.id == heroId
            val cardIndex = state.options.cards.indexOfFirst { it.photoId == photo.id }
            val isSelected = !isHero && cardIndex == selectedLayerIndex
            PhotoSelectionChip(
                photo = photo,
                isHero = isHero,
                isSelected = isSelected,
                onClick = {
                    if (isHero) return@PhotoSelectionChip
                    if (cardIndex >= 0) {
                        onSelectLayer(if (selectedLayerIndex == cardIndex) null else cardIndex)
                    } else {
                        onPickHero(photo.id)
                    }
                },
                onRemove = { onRemoveFromCollage(photo.id) },
            )
        }
        if (canAddMore) {
            item("add-photo") { AddPhotoChip(onClick = onRequestAddPhotos) }
        }
    }

    // Selection context — only present when the user has tapped a card / mini-map / caption
    // layer either on the preview or in the chip strip above.
    AnimatedVisibility(
        visible = selectedLayerIndex != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        SelectionContextBar(
            state = state,
            selectedLayerIndex = selectedLayerIndex,
            onResize = onResize,
            onResizeMap = onResizeMap,
            onResizeCaption = onResizeCaption,
            onPromote = onPromote,
            onClearSelection = onClearSelection,
        )
    }
}

@Composable
private fun PaneHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact Material 3 settings header with the visibility action kept in context. */
@Composable
private fun CaptionVisibilityHeader(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SelectionContextBar(
    state: CollageUiState,
    selectedLayerIndex: Int?,
    onResize: (Int, Float) -> Unit,
    onResizeMap: (Float) -> Unit,
    onResizeCaption: (Float) -> Unit,
    onPromote: (Int) -> Unit,
    onClearSelection: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val titleRes = when {
                selectedLayerIndex == MAP_LAYER_INDEX -> R.string.collage_editor_minimap_title
                selectedLayerIndex == CAPTION_LAYER_INDEX -> R.string.collage_editor_caption_layer
                selectedLayerIndex != null && selectedLayerIndex >= 0 -> R.string.collage_editor_selected_photo
                else -> R.string.collage_editor_selection
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(titleRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onClearSelection) {
                    Text(stringResource(R.string.action_done))
                }
            }

            val activeIndex = selectedLayerIndex
                ?.takeIf { it >= 0 && it in state.options.cards.indices }
            if (activeIndex != null) {
                val card = state.options.cards[activeIndex]
                Text(
                    stringResource(R.string.collage_editor_size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = card.widthFrac,
                    onValueChange = { onResize(activeIndex, it) },
                    valueRange = 0.14f..0.7f,
                )
                FilledTonalButton(
                    onClick = { onPromote(activeIndex) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.collage_editor_use_as_background))
                }
            }

            if (selectedLayerIndex == MAP_LAYER_INDEX && state.options.showMiniMap) {
                val mapWidthFrac = state.options.mapPlacement?.widthFrac ?: 0.4f
                Text(
                    stringResource(R.string.collage_editor_map_size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = mapWidthFrac,
                    onValueChange = onResizeMap,
                    valueRange = 0.18f..0.7f,
                )
            }

            if (selectedLayerIndex == CAPTION_LAYER_INDEX) {
                val captionWidthFrac = state.options.captionPlacement?.widthFrac
                    ?: DEFAULT_CAPTION_WIDTH_FRAC
                Text(
                    stringResource(R.string.collage_editor_caption_width),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = captionWidthFrac,
                    onValueChange = onResizeCaption,
                    valueRange = 0.3f..0.9f,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// LAYOUT pane: template + aspect ratio + (conditional) map-corner picker.
// ---------------------------------------------------------------------------------------

@Composable
private fun LayoutPane(
    state: CollageUiState,
    onTemplateChange: (CollageTemplate) -> Unit,
    onAspectChange: (CollageAspectRatio) -> Unit,
) {
    PaneHeader(
        stringResource(R.string.collage_editor_template),
        stringResource(R.string.collage_editor_template_subtitle),
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(CollageTemplate.entries.size) { index ->
            val template = CollageTemplate.entries[index]
            EditorChip(
                selected = state.options.template == template,
                label = stringResource(template.titleRes),
                enabled = state.selectedIds.size >= template.minimumPhotos,
                onClick = { onTemplateChange(template) },
            )
        }
    }

    PaneHeader(stringResource(R.string.collage_editor_aspect_ratio))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CollageAspectRatio.entries.forEach { ratio ->
            EditorChip(
                selected = state.options.aspectRatio == ratio,
                label = ratio.label,
                onClick = { onAspectChange(ratio) },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// STYLE pane: background mode + colour swatches + dark style toggle.
// ---------------------------------------------------------------------------------------

@Composable
private fun StylePane(
    state: CollageUiState,
    onBackgroundModeChange: (CollageBackgroundMode) -> Unit,
    onBackgroundColorChange: (Int) -> Unit,
    onDarkStyleChange: (Boolean) -> Unit,
) {
    PaneHeader(
        stringResource(R.string.collage_editor_background),
        stringResource(R.string.collage_editor_background_subtitle),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorChip(
            selected = state.options.backgroundMode == CollageBackgroundMode.HERO_PHOTO,
            label = stringResource(R.string.collage_editor_bg_photo),
            onClick = { onBackgroundModeChange(CollageBackgroundMode.HERO_PHOTO) },
        )
        EditorChip(
            selected = state.options.backgroundMode == CollageBackgroundMode.SOLID_COLOR,
            label = stringResource(R.string.collage_editor_bg_color),
            onClick = { onBackgroundModeChange(CollageBackgroundMode.SOLID_COLOR) },
        )
    }
    if (state.options.backgroundMode == CollageBackgroundMode.SOLID_COLOR) {
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
                    onClick = { onBackgroundColorChange(color) },
                )
            }
        }
    } else {
        Text(
            stringResource(R.string.collage_editor_hero_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    PaneHeader(stringResource(R.string.collage_editor_theme))
    ToggleRow(stringResource(R.string.collage_editor_dark_style), state.options.darkStyle, onDarkStyleChange)
}

// ---------------------------------------------------------------------------------------
// MAP pane: mini-map toggle + zoom + tourist style.
// ---------------------------------------------------------------------------------------

@Composable
private fun MapPane(
    state: CollageUiState,
    onShowMiniMapChange: (Boolean) -> Unit,
    onMapPhotoChange: (String) -> Unit,
    onSaveMapLocation: (String, Double, Double, String?) -> Unit,
    onMapCornerChange: (CollageMapCorner) -> Unit,
    onMapSizeChange: (Float) -> Unit,
    onMapZoomChange: (Int) -> Unit,
    onUseTouristMapStyleChange: (Boolean) -> Unit,
) {
    val mapCandidates = state.selectedPhotos
    val mapTarget = state.mapTargetPhoto
    val mapIsActive = state.options.showMiniMap && state.mapPhoto != null
    var locationPickerPhoto by remember { mutableStateOf<GeoPhoto?>(null) }
    PaneHeader(
        stringResource(R.string.collage_editor_minimap_title),
        stringResource(R.string.collage_editor_minimap_subtitle),
    )
    if (!mapIsActive) {
        FilledTonalButton(
            onClick = {
                if (mapTarget?.location == null) {
                    locationPickerPhoto = mapTarget
                } else {
                    onShowMiniMapChange(true)
                }
            },
            enabled = mapTarget != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Map, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.collage_editor_add_map))
        }
        if (mapTarget?.location == null) {
            Text(
                stringResource(R.string.collage_editor_choose_location_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        FilledTonalButton(
            onClick = { onShowMiniMapChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.collage_editor_remove_map))
        }
    }
    if (mapCandidates.size > 1) {
        PaneHeader(stringResource(R.string.collage_editor_choose_map_photo))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mapCandidates, key = { it.id }) { photo ->
                MapSourcePhotoChip(
                    photo = photo,
                    selected = mapTarget?.id == photo.id,
                    onClick = { onMapPhotoChange(photo.id) },
                )
            }
        }
    }
    if (mapTarget != null) {
        OutlinedButton(
            onClick = { locationPickerPhoto = mapTarget },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Place, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    if (mapTarget.location == null) R.string.collage_editor_choose_place
                    else R.string.collage_editor_change_place,
                ),
            )
        }
        mapTarget.location?.let { location ->
            Text(
                location.addressText?.takeIf { it.isNotBlank() }
                    ?: DateTimeFormatters.coordinates(location.latitude, location.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    AnimatedVisibility(
        visible = mapIsActive,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaneHeader(stringResource(R.string.collage_editor_map_position))
            MapCornerPicker(
                selected = state.options.mapCorner,
                onSelect = onMapCornerChange,
            )
            Text(
                stringResource(R.string.collage_editor_map_size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = state.options.mapPlacement?.widthFrac ?: 0.4f,
                onValueChange = onMapSizeChange,
                valueRange = 0.18f..0.7f,
            )
            MapZoomRow(
                zoom = state.options.mapZoom,
                onZoomChange = onMapZoomChange,
            )
            ToggleRow(
                title = stringResource(R.string.collage_editor_tourist_map),
                checked = state.options.useTouristMapStyle,
                onCheckedChange = onUseTouristMapStyleChange,
            )
        }
    }

    locationPickerPhoto?.let { photo ->
        MapLocationPickerSheet(
            photo = photo,
            onDismiss = { locationPickerPhoto = null },
            onSave = { latitude, longitude, address ->
                onSaveMapLocation(photo.id, latitude, longitude, address)
                locationPickerPhoto = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapLocationPickerSheet(
    photo: GeoPhoto,
    onDismiss: () -> Unit,
    onSave: (Double, Double, String?) -> Unit,
) {
    val context = LocalContext.current
    val initialPoint = photo.location?.let { LatLng(it.latitude, it.longitude) }
    var pickedPoint by remember(photo.id) { mutableStateOf(initialPoint) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPoint ?: LatLng(20.0, 0.0), if (initialPoint == null) 2.5f else 14f)
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { nextValue -> nextValue != SheetValue.Hidden },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PaneHeader(
                stringResource(R.string.collage_editor_place_picker_title),
                stringResource(R.string.collage_editor_place_picker_subtitle),
            )
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(22.dp)),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = PermissionController.hasAnyLocation(context),
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = true,
                ),
                mapViewFactory = ::GestureSafeMapView,
                onMapClick = { point ->
                    pickedPoint = point
                },
            ) {
                pickedPoint?.let { point ->
                    Marker(
                        state = remember(point) { MarkerState(position = point) },
                        title = stringResource(R.string.collage_editor_selected_place),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val point = pickedPoint ?: return@Button
                        onSave(point.latitude, point.longitude, null)
                    },
                    enabled = pickedPoint != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MapSourcePhotoChip(
    photo: GeoPhoto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = photo.thumbnailUri ?: photo.imageUri,
            contentDescription = photo.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (selected) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(22.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    Icons.Rounded.Map,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// INFO pane: date / coordinates toggles + editable address label for the hero photo.
// ---------------------------------------------------------------------------------------

@Composable
private fun InfoPane(
    state: CollageUiState,
    captionSelected: Boolean,
    selectedTextLayerIndex: Int?,
    onSelectTextLayer: (Int?) -> Unit,
    onAddTextLayer: (String) -> Unit,
    onRemoveTextLayer: (Int) -> Unit,
    onTextLayerTextChange: (Int, String) -> Unit,
    onTextLayerWidthChange: (Int, Float) -> Unit,
    onTextLayerSizeChange: (Int, Float) -> Unit,
    onTextLayerTextColorChange: (Int, Int) -> Unit,
    onTextLayerBackgroundColorChange: (Int, Int) -> Unit,
    onTextLayerBackgroundOpacityChange: (Int, Float) -> Unit,
    onTextLayerBorderColorChange: (Int, Int) -> Unit,
    onTextLayerBorderEnabledChange: (Int, Boolean) -> Unit,
    onTextLayerCornerRadiusChange: (Int, Float) -> Unit,
    onTextLayerShapeChange: (Int, CollageTextShape) -> Unit,
    onShowCaptionChange: (Boolean) -> Unit,
    onCustomCaptionChange: (String) -> Unit,
    onCaptionWidthChange: (Float) -> Unit,
    onCaptionTextSizeChange: (Float) -> Unit,
    onCaptionTextColorChange: (Int) -> Unit,
    onCaptionBackgroundColorChange: (Int) -> Unit,
    onCaptionBackgroundOpacityChange: (Float) -> Unit,
    onCaptionBorderColorChange: (Int) -> Unit,
    onCaptionBorderEnabledChange: (Boolean) -> Unit,
    onCaptionCornerRadiusChange: (Float) -> Unit,
    onCaptionShapeChange: (CollageTextShape) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onShowCoordinatesChange: (Boolean) -> Unit,
    onLocationLabelDraftChange: (String) -> Unit,
    onSaveLocationLabel: () -> Unit,
) {
    if (captionSelected) {
        PhotoCaptionPane(
            state = state,
            selected = true,
            onShowCaptionChange = onShowCaptionChange,
            onCustomCaptionChange = onCustomCaptionChange,
            onCaptionWidthChange = onCaptionWidthChange,
            onCaptionTextSizeChange = onCaptionTextSizeChange,
            onCaptionTextColorChange = onCaptionTextColorChange,
            onCaptionBackgroundColorChange = onCaptionBackgroundColorChange,
            onCaptionBackgroundOpacityChange = onCaptionBackgroundOpacityChange,
            onCaptionBorderColorChange = onCaptionBorderColorChange,
            onCaptionBorderEnabledChange = onCaptionBorderEnabledChange,
            onCaptionCornerRadiusChange = onCaptionCornerRadiusChange,
            onCaptionShapeChange = onCaptionShapeChange,
            onShowDateChange = onShowDateChange,
            onShowCoordinatesChange = onShowCoordinatesChange,
            onLocationLabelDraftChange = onLocationLabelDraftChange,
            onSaveLocationLabel = onSaveLocationLabel,
        )
        return
    }

    val newText = stringResource(R.string.collage_editor_new_text_default)
    PaneHeader(
        stringResource(R.string.collage_editor_text_fields_title),
        stringResource(R.string.collage_editor_text_fields_subtitle),
    )
    FilledTonalButton(
        onClick = { onAddTextLayer(newText) },
        enabled = state.options.textLayers.size < MAX_TEXT_LAYERS,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.collage_editor_add_text_field))
    }
    if (state.options.textLayers.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.options.textLayers) { index, layer ->
                FilterChip(
                    selected = selectedTextLayerIndex == index,
                    onClick = { onSelectTextLayer(if (selectedTextLayerIndex == index) null else index) },
                    label = {
                        Text(
                            layer.text.lineSequence().firstOrNull()?.take(18)?.ifBlank { null }
                                ?: stringResource(R.string.collage_editor_text_field_number, index + 1),
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }

    val selectedLayer = selectedTextLayerIndex?.let(state.options.textLayers::getOrNull)
    if (selectedLayer != null) {
        val index = requireNotNull(selectedTextLayerIndex)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = selectedLayer.text,
                    onValueChange = { onTextLayerTextChange(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.collage_editor_text_content)) },
                    minLines = 1,
                    maxLines = 5,
                )
                TextSettingSlider(
                    label = stringResource(R.string.collage_editor_text_width),
                    value = selectedLayer.widthFrac,
                    range = 0.2f..0.9f,
                    onValueChange = { onTextLayerWidthChange(index, it) },
                )
                TextSettingSlider(
                    label = stringResource(R.string.collage_editor_text_size),
                    value = selectedLayer.textSizeFrac,
                    range = 0.018f..0.1f,
                    onValueChange = { onTextLayerSizeChange(index, it) },
                )

                Text(
                    stringResource(R.string.collage_editor_text_shape),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CollageTextShape.entries) { shape ->
                        EditorChip(
                            selected = selectedLayer.shape == shape,
                            label = stringResource(shape.labelRes()),
                            onClick = { onTextLayerShapeChange(index, shape) },
                        )
                    }
                }
                if (selectedLayer.shape == CollageTextShape.ROUNDED) {
                    TextSettingSlider(
                        label = stringResource(R.string.collage_editor_text_roundness),
                        value = selectedLayer.cornerRadiusFrac,
                        range = 0f..0.12f,
                        onValueChange = { onTextLayerCornerRadiusChange(index, it) },
                    )
                }

                ColorSettingRow(
                    label = stringResource(R.string.collage_editor_text_color),
                    colors = TEXT_COLOR_SWATCHES,
                    selectedColor = selectedLayer.textColor,
                    onSelect = { onTextLayerTextColorChange(index, it) },
                )
                if (selectedLayer.shape != CollageTextShape.NONE) {
                    ColorSettingRow(
                        label = stringResource(R.string.collage_editor_text_background),
                        colors = TEXT_BACKGROUND_SWATCHES,
                        selectedColor = selectedLayer.backgroundColor,
                        onSelect = { onTextLayerBackgroundColorChange(index, it) },
                    )
                    TextSettingSlider(
                        label = stringResource(R.string.collage_editor_text_background_opacity),
                        value = selectedLayer.backgroundOpacity,
                        range = 0f..1f,
                        onValueChange = { onTextLayerBackgroundOpacityChange(index, it) },
                    )
                }

                ToggleRow(
                    title = stringResource(R.string.collage_editor_text_border),
                    checked = selectedLayer.borderWidthFrac > 0f,
                    onCheckedChange = { onTextLayerBorderEnabledChange(index, it) },
                )
                if (selectedLayer.borderWidthFrac > 0f) {
                    ColorSettingRow(
                        label = stringResource(R.string.collage_editor_text_border_color),
                        colors = TEXT_COLOR_SWATCHES,
                        selectedColor = selectedLayer.borderColor,
                        onSelect = { onTextLayerBorderColorChange(index, it) },
                    )
                }
                TextButton(
                    onClick = { onRemoveTextLayer(index) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.collage_editor_delete_text_field))
                }
            }
        }
    } else if (state.options.textLayers.isNotEmpty()) {
        Text(
            stringResource(R.string.collage_editor_select_text_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (!captionSelected) {
        PhotoCaptionPane(
            state = state,
            selected = false,
            onShowCaptionChange = onShowCaptionChange,
            onCustomCaptionChange = onCustomCaptionChange,
            onCaptionWidthChange = onCaptionWidthChange,
            onCaptionTextSizeChange = onCaptionTextSizeChange,
            onCaptionTextColorChange = onCaptionTextColorChange,
            onCaptionBackgroundColorChange = onCaptionBackgroundColorChange,
            onCaptionBackgroundOpacityChange = onCaptionBackgroundOpacityChange,
            onCaptionBorderColorChange = onCaptionBorderColorChange,
            onCaptionBorderEnabledChange = onCaptionBorderEnabledChange,
            onCaptionCornerRadiusChange = onCaptionCornerRadiusChange,
            onCaptionShapeChange = onCaptionShapeChange,
            onShowDateChange = onShowDateChange,
            onShowCoordinatesChange = onShowCoordinatesChange,
            onLocationLabelDraftChange = onLocationLabelDraftChange,
            onSaveLocationLabel = onSaveLocationLabel,
        )
    }
}

@Composable
private fun PhotoCaptionPane(
    state: CollageUiState,
    selected: Boolean,
    onShowCaptionChange: (Boolean) -> Unit,
    onCustomCaptionChange: (String) -> Unit,
    onCaptionWidthChange: (Float) -> Unit,
    onCaptionTextSizeChange: (Float) -> Unit,
    onCaptionTextColorChange: (Int) -> Unit,
    onCaptionBackgroundColorChange: (Int) -> Unit,
    onCaptionBackgroundOpacityChange: (Float) -> Unit,
    onCaptionBorderColorChange: (Int) -> Unit,
    onCaptionBorderEnabledChange: (Boolean) -> Unit,
    onCaptionCornerRadiusChange: (Float) -> Unit,
    onCaptionShapeChange: (CollageTextShape) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onShowCoordinatesChange: (Boolean) -> Unit,
    onLocationLabelDraftChange: (String) -> Unit,
    onSaveLocationLabel: () -> Unit,
) {
    val style = state.options.captionStyle
    val themedTextColor = if (state.options.darkStyle) 0xFFE8F0EC.toInt() else 0xFF17201D.toInt()
    val themedBackgroundColor = if (state.options.darkStyle) 0xFF202426.toInt() else 0xFFFFFFFF.toInt()
    var showAdvanced by rememberSaveable(selected) { mutableStateOf(false) }
    CaptionVisibilityHeader(
        title = stringResource(
            if (selected) R.string.collage_editor_selected_caption_title
            else R.string.collage_editor_caption_title,
        ),
        subtitle = if (selected) null else stringResource(R.string.collage_editor_caption_subtitle),
        checked = state.options.showCaption,
        onCheckedChange = onShowCaptionChange,
    )
    AnimatedVisibility(
        visible = state.options.showCaption,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextSettingSlider(
                label = stringResource(R.string.collage_editor_text_size),
                value = style.textSizeScale,
                range = 0.65f..1.8f,
                onValueChange = onCaptionTextSizeChange,
            )
            if (state.options.template != CollageTemplate.POSTCARD) {
                TextSettingSlider(
                    label = stringResource(R.string.collage_editor_caption_width),
                    value = state.options.captionPlacement?.widthFrac ?: DEFAULT_CAPTION_WIDTH_FRAC,
                    range = 0.3f..0.9f,
                    onValueChange = onCaptionWidthChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.collage_editor_text_shape),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        stringResource(
                            if (showAdvanced) R.string.collage_editor_fewer_settings
                            else R.string.collage_editor_more_settings,
                        ),
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(CollageTextShape.entries) { shape ->
                    EditorChip(
                        selected = style.shape == shape,
                        label = stringResource(shape.labelRes()),
                        onClick = { onCaptionShapeChange(shape) },
                    )
                }
            }
            AnimatedVisibility(
                visible = showAdvanced,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (style.shape == CollageTextShape.ROUNDED) {
                        TextSettingSlider(
                            label = stringResource(R.string.collage_editor_text_roundness),
                            value = style.cornerRadiusFrac,
                            range = 0f..0.12f,
                            onValueChange = onCaptionCornerRadiusChange,
                        )
                    }
                    ColorSettingRow(
                        label = stringResource(R.string.collage_editor_text_color),
                        colors = TEXT_COLOR_SWATCHES,
                        selectedColor = style.textColor ?: themedTextColor,
                        onSelect = onCaptionTextColorChange,
                    )
                    if (style.shape != CollageTextShape.NONE) {
                        ColorSettingRow(
                            label = stringResource(R.string.collage_editor_text_background),
                            colors = TEXT_BACKGROUND_SWATCHES,
                            selectedColor = style.backgroundColor ?: themedBackgroundColor,
                            onSelect = onCaptionBackgroundColorChange,
                        )
                        TextSettingSlider(
                            label = stringResource(R.string.collage_editor_text_background_opacity),
                            value = style.backgroundOpacity,
                            range = 0f..1f,
                            onValueChange = onCaptionBackgroundOpacityChange,
                        )
                    }
                    ToggleRow(
                        title = stringResource(R.string.collage_editor_text_border),
                        checked = style.borderWidthFrac > 0f,
                        onCheckedChange = onCaptionBorderEnabledChange,
                    )
                    if (style.borderWidthFrac > 0f) {
                        ColorSettingRow(
                            label = stringResource(R.string.collage_editor_text_border_color),
                            colors = TEXT_COLOR_SWATCHES,
                            selectedColor = style.borderColor ?: themedTextColor,
                            onSelect = onCaptionBorderColorChange,
                        )
                    }

                    PaneHeader(stringResource(R.string.collage_editor_caption_content))
                    OutlinedTextField(
                        value = state.options.customCaption,
                        onValueChange = onCustomCaptionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.collage_editor_custom_text)) },
                        placeholder = { Text(stringResource(R.string.collage_editor_custom_text_placeholder)) },
                        supportingText = { Text(stringResource(R.string.collage_editor_custom_text_hint)) },
                        trailingIcon = if (state.options.customCaption.isNotEmpty()) {
                            {
                                IconButton(onClick = { onCustomCaptionChange("") }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.collage_editor_clear_text_cd),
                                    )
                                }
                            }
                        } else null,
                        minLines = 1,
                        maxLines = 3,
                    )
                    ToggleRow(stringResource(R.string.collage_editor_caption_date), state.options.showDate, onShowDateChange)
                    ToggleRow(stringResource(R.string.collage_editor_caption_coords), state.options.showCoordinates, onShowCoordinatesChange)
                    if (state.heroPhoto?.location != null) {
                        PaneHeader(stringResource(R.string.collage_editor_caption_address))
                        OutlinedTextField(
                            value = state.locationLabelDraft,
                            onValueChange = onLocationLabelDraftChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.collage_editor_address_placeholder)) },
                            minLines = 1,
                            maxLines = 3,
                        )
                        FilledTonalButton(
                            onClick = onSaveLocationLabel,
                            enabled = state.isLocationLabelDirty,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    if (state.isResolvingLocationLabel) R.string.collage_editor_address_resolving
                                    else R.string.collage_editor_save_address,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextSettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColorSettingRow(
    label: String,
    colors: List<Int>,
    selectedColor: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            items(colors) { color ->
                ColorSwatch(
                    color = color,
                    selected = color == selectedColor,
                    onClick = { onSelect(color) },
                )
            }
        }
    }
}

@androidx.annotation.StringRes
private fun CollageTextShape.labelRes(): Int = when (this) {
    CollageTextShape.NONE -> R.string.collage_editor_text_shape_none
    CollageTextShape.RECTANGLE -> R.string.collage_editor_text_shape_rectangle
    CollageTextShape.ROUNDED -> R.string.collage_editor_text_shape_rounded
    CollageTextShape.PILL -> R.string.collage_editor_text_shape_pill
}

// ---------------------------------------------------------------------------------------
// Rendered photo layouts use a bitmap for the photo composition and a live Compose map on
// top. Keeping the map out of the bitmap makes direct manipulation possible in every layout.
// ---------------------------------------------------------------------------------------

@Composable
private fun InteractivePhotoLayoutPreview(
    state: CollageUiState,
    selectedLayerIndex: Int?,
    onSelectLayer: (Int?) -> Unit,
    onClearSelection: () -> Unit,
    onTransformMap: (Float, Float, Float, Float) -> Unit,
    onTransformCaption: (Float, Float, Float, Float) -> Unit,
    onTransformTextLayer: (Int, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onClearSelection() })
        },
    ) {
        state.preview?.let { preview ->
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = stringResource(R.string.collage_editor_preview_cd),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // In the photo-layout templates the renderer bakes the automatic caption into the
        // preview bitmap. Mirror it with a transparent Compose layer so the visible caption
        // still receives taps and drags just like the live PHOTO_WITH_MAP caption does.
        val captionLines = remember(
            state.heroPhoto,
            state.options.showDate,
            state.options.showCoordinates,
            state.options.customCaption,
            state.options.showCaption,
        ) {
            buildCaptionLinesForPreview(
                state.heroPhoto,
                state.options.showDate,
                state.options.showCoordinates,
                state.options.customCaption,
                state.options.showCaption,
            )
        }
        if (captionLines.isNotEmpty()) {
            val placement = state.options.captionPlacement
            val isPostcard = state.options.template == CollageTemplate.POSTCARD
            val postcardMarginPx = constraints.maxWidth * 0.06f
            val postcardTopPx = constraints.maxHeight * 0.66f + postcardMarginPx * 0.5f
            val defaultWidth = when {
                isPostcard -> 0.88f
                state.options.showMiniMap -> 0.55f
                else -> 0.72f
            }
            DraggableCaptionLayer(
                canvasW = constraints.maxWidth,
                canvasH = constraints.maxHeight,
                originX = placement?.originX ?: if (isPostcard) 0.06f else DEFAULT_CAPTION_ORIGIN_X,
                originY = placement?.originY ?: if (isPostcard) {
                    postcardTopPx / constraints.maxHeight
                } else {
                    null
                },
                widthFrac = placement?.widthFrac ?: defaultWidth,
                rotationDeg = placement?.rotationDeg ?: 0f,
                fixedHeightPx = if (isPostcard) {
                    (constraints.maxHeight - postcardMarginPx - postcardTopPx).coerceAtLeast(1f)
                } else {
                    renderedCaptionHeightPx(
                        canvasWidthPx = constraints.maxWidth,
                        renderedCanvasWidth = state.options.aspectRatio.width,
                        lineCount = captionLines.size,
                        textSizeScale = state.options.captionStyle.textSizeScale,
                    )
                },
                selected = selectedLayerIndex == CAPTION_LAYER_INDEX,
                onSelect = { onSelectLayer(CAPTION_LAYER_INDEX) },
                onTransform = onTransformCaption,
            ) {
                CaptionOverlay(
                    lines = captionLines,
                    darkStyle = state.options.darkStyle,
                    captionStyle = state.options.captionStyle,
                    modifier = Modifier.alpha(0f),
                )
            }
        }

        DraggableMapOverlay(
            state = state,
            canvasW = constraints.maxWidth,
            canvasH = constraints.maxHeight,
            selected = selectedLayerIndex == MAP_LAYER_INDEX,
            onSelect = { onSelectLayer(MAP_LAYER_INDEX) },
            onTransform = onTransformMap,
        )
        TextLayersOverlay(
            layers = state.options.textLayers,
            canvasW = constraints.maxWidth,
            canvasH = constraints.maxHeight,
            selectedLayerIndex = selectedTextLayerIndex(selectedLayerIndex),
            onSelect = { onSelectLayer(textLayerSelectionIndex(it)) },
            onTransform = onTransformTextLayer,
        )
    }
}

@Composable
private fun DraggableMapOverlay(
    state: CollageUiState,
    canvasW: Int,
    canvasH: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
) {
    val opts = state.options
    val mapSourcePhoto = state.mapPhoto
    if (!opts.showMiniMap || mapSourcePhoto?.location == null) return
    val placement = opts.mapPlacement
    val centerX = placement?.centerX ?: when (opts.mapCorner) {
        CollageMapCorner.TOP_LEFT, CollageMapCorner.BOTTOM_LEFT -> 0.22f
        CollageMapCorner.TOP_RIGHT, CollageMapCorner.BOTTOM_RIGHT -> 0.78f
    }
    val centerY = placement?.centerY ?: when (opts.mapCorner) {
        CollageMapCorner.TOP_LEFT, CollageMapCorner.TOP_RIGHT -> 0.22f
        CollageMapCorner.BOTTOM_LEFT, CollageMapCorner.BOTTOM_RIGHT -> 0.78f
    }
    DraggableLayer(
        canvasW = canvasW,
        canvasH = canvasH,
        centerX = centerX,
        centerY = centerY,
        widthFrac = placement?.widthFrac ?: 0.4f,
        rotationDeg = placement?.rotationDeg ?: 0f,
        selected = selected,
        onSelect = onSelect,
        onTransform = onTransform,
    ) {
        MiniMapTile(
            photo = mapSourcePhoto,
            mapZoom = opts.mapZoom,
            useTouristMapStyle = opts.useTouristMapStyle,
            darkStyle = opts.darkStyle,
        )
    }
}

@Composable
private fun TextLayersOverlay(
    layers: List<CollageTextLayer>,
    canvasW: Int,
    canvasH: Int,
    selectedLayerIndex: Int?,
    onSelect: (Int) -> Unit,
    onTransform: (Int, Float, Float, Float, Float) -> Unit,
) {
    layers.forEachIndexed { index, layer ->
        if (layer.text.isBlank()) return@forEachIndexed
        DraggableTextLayer(
            layer = layer,
            canvasW = canvasW,
            canvasH = canvasH,
            selected = selectedLayerIndex == index,
            onSelect = { onSelect(index) },
            onTransform = { x, y, width, rotation ->
                onTransform(index, x, y, width, rotation)
            },
        )
    }
}

@Composable
private fun DraggableTextLayer(
    layer: CollageTextLayer,
    canvasW: Int,
    canvasH: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    val shorter = min(canvasW, canvasH).toFloat()
    val originXPx = layer.originX * canvasW
    val originYPx = layer.originY * canvasH
    var measuredHeightPx by remember(layer.text, layer.widthFrac, layer.textSizeFrac) { mutableFloatStateOf(0f) }
    var pendingX by remember(originXPx) { mutableFloatStateOf(0f) }
    var pendingY by remember(originYPx) { mutableFloatStateOf(0f) }
    var pendingScale by remember(layer.widthFrac) { mutableFloatStateOf(1f) }
    var pendingRotation by remember(layer.rotationDeg) { mutableFloatStateOf(0f) }
    val effectiveWidthFrac = (layer.widthFrac * pendingScale).coerceIn(0.2f, 0.9f)
    val widthPx = effectiveWidthFrac * canvasW
    val shape = when (layer.shape) {
        CollageTextShape.PILL -> RoundedCornerShape(50)
        CollageTextShape.ROUNDED -> RoundedCornerShape(
            with(density) { (layer.cornerRadiusFrac * shorter).toDp() },
        )
        CollageTextShape.NONE, CollageTextShape.RECTANGLE -> RoundedCornerShape(0.dp)
    }
    val background = Color(layer.backgroundColor).copy(alpha = layer.backgroundOpacity)
    val borderWidth = with(density) { (layer.borderWidthFrac * shorter).toDp() }
    val contentPadding = with(density) { (shorter * 0.018f).toDp() }
    val textSize = (layer.textSizeFrac * shorter / density.density).sp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (originXPx + pendingX).roundToInt(),
                    (originYPx + pendingY).roundToInt(),
                )
            }
            .rotate(layer.rotationDeg + pendingRotation)
            .width(with(density) { widthPx.toDp() })
            .onGloballyPositioned { measuredHeightPx = it.size.height.toFloat() }
            .clip(shape)
            .then(
                if (layer.shape != CollageTextShape.NONE) Modifier.background(background)
                else Modifier,
            )
            .then(
                if (layer.borderWidthFrac > 0f) Modifier.border(borderWidth, Color(layer.borderColor), shape)
                else Modifier,
            )
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier,
            )
            .pointerInput(
                canvasW,
                canvasH,
                layer.originX,
                layer.originY,
                layer.widthFrac,
                layer.rotationDeg,
            ) {
                detectLayerTransformGestures(
                    onStart = onSelect,
                    onEnd = { changed ->
                        if (changed) {
                            val finalWidthFrac = (layer.widthFrac * pendingScale).coerceIn(0.2f, 0.9f)
                            val maxX = (1f - finalWidthFrac).coerceAtLeast(0f)
                            val heightFrac = if (canvasH > 0) measuredHeightPx / canvasH else 0f
                            val maxY = (1f - heightFrac).coerceAtLeast(0f)
                            onTransform(
                                ((originXPx + pendingX) / canvasW).coerceIn(0f, maxX),
                                ((originYPx + pendingY) / canvasH).coerceIn(0f, maxY),
                                finalWidthFrac,
                                layer.rotationDeg + pendingRotation,
                            )
                        }
                        pendingX = 0f
                        pendingY = 0f
                        pendingScale = 1f
                        pendingRotation = 0f
                    },
                    onCancel = {
                        pendingX = 0f
                        pendingY = 0f
                        pendingScale = 1f
                        pendingRotation = 0f
                    },
                    onTransform = { pan, zoom, rotation ->
                        pendingX += pan.x
                        pendingY += pan.y
                        pendingScale = (pendingScale * zoom).coerceIn(
                            0.2f / layer.widthFrac,
                            0.9f / layer.widthFrac,
                        )
                        pendingRotation += rotation
                    },
                )
            }
            .padding(contentPadding),
    ) {
        Text(
            text = layer.text,
            color = Color(layer.textColor),
            fontSize = textSize,
            lineHeight = textSize * 1.15f,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Live, draggable preview for PHOTO_WITH_MAP, including its movable photo cards and caption.
@Composable
private fun InteractivePhotoMapPreview(
    state: CollageUiState,
    selectedCardIndex: Int?,
    onSelectCard: (Int?) -> Unit,
    onTransformCard: (Int, Float, Float, Float, Float) -> Unit,
    onTransformMap: (Float, Float, Float, Float) -> Unit,
    onTransformCaption: (Float, Float, Float, Float) -> Unit,
    onTransformTextLayer: (Int, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val opts = state.options
    val photosById = remember(state.allPhotos) { state.allPhotos.associateBy { it.id } }
    val heroPhoto = opts.backgroundPhotoId?.let(photosById::get) ?: state.selectedPhotos.firstOrNull()
    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onSelectCard(null) })
        },
    ) {
        val canvasW = constraints.maxWidth
        val canvasH = constraints.maxHeight

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
        val captionLines = remember(
            captionPhoto,
            opts.showDate,
            opts.showCoordinates,
            opts.customCaption,
            opts.showCaption,
        ) {
            buildCaptionLinesForPreview(
                captionPhoto,
                opts.showDate,
                opts.showCoordinates,
                opts.customCaption,
                opts.showCaption,
            )
        }

        DraggableMapOverlay(
            state = state,
            canvasW = canvasW,
            canvasH = canvasH,
            selected = selectedCardIndex == MAP_LAYER_INDEX,
            onSelect = { onSelectCard(MAP_LAYER_INDEX) },
            onTransform = onTransformMap,
        )

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
                onTransform = { nx, ny, width, rotation ->
                    onTransformCard(index, nx, ny, width, rotation)
                },
            ) {
                PhotoCardTile(cardPhoto, darkStyle = opts.darkStyle)
            }
        }

        if (captionPhoto != null && captionLines.isNotEmpty()) {
            val captionPlacement = opts.captionPlacement
            val originX = captionPlacement?.originX ?: DEFAULT_CAPTION_ORIGIN_X
            val captionWidthFrac = captionPlacement?.widthFrac ?: DEFAULT_CAPTION_WIDTH_FRAC
            DraggableCaptionLayer(
                canvasW = canvasW,
                canvasH = canvasH,
                originX = originX,
                originY = captionPlacement?.originY,
                widthFrac = captionWidthFrac,
                rotationDeg = captionPlacement?.rotationDeg ?: 0f,
                selected = selectedCardIndex == CAPTION_LAYER_INDEX,
                onSelect = { onSelectCard(CAPTION_LAYER_INDEX) },
                onTransform = onTransformCaption,
            ) {
                CaptionOverlay(
                    lines = captionLines,
                    darkStyle = opts.darkStyle,
                    captionStyle = opts.captionStyle,
                )
            }
        }

        TextLayersOverlay(
            layers = opts.textLayers,
            canvasW = canvasW,
            canvasH = canvasH,
            selectedLayerIndex = selectedTextLayerIndex(selectedCardIndex),
            onSelect = { onSelectCard(textLayerSelectionIndex(it)) },
            onTransform = onTransformTextLayer,
        )
    }
}

private const val MAP_LAYER_INDEX = -1
private const val CAPTION_LAYER_INDEX = -2
private const val TEXT_LAYER_INDEX_BASE = -1000
private const val DEFAULT_CAPTION_ORIGIN_X = 0.04f
private const val DEFAULT_CAPTION_WIDTH_FRAC = 0.6f

private fun textLayerSelectionIndex(index: Int): Int = TEXT_LAYER_INDEX_BASE - index

private fun selectedTextLayerIndex(selection: Int?): Int? = selection
    ?.takeIf { it <= TEXT_LAYER_INDEX_BASE }
    ?.let { TEXT_LAYER_INDEX_BASE - it }

/** Mirrors the bitmap renderer's floating-caption height for a pixel-perfect tap target. */
private fun renderedCaptionHeightPx(
    canvasWidthPx: Int,
    renderedCanvasWidth: Int,
    lineCount: Int,
    textSizeScale: Float,
): Float {
    val renderedWidth = renderedCanvasWidth.toFloat()
    val titleSize = ((renderedWidth * 0.036f).coerceIn(28f, 56f) * textSizeScale)
        .coerceIn(20f, 100f)
    val subtitleSize = titleSize * 0.55f
    val extraLines = (lineCount - 1).coerceAtLeast(0)
    val renderedHeight = titleSize + 12f + subtitleSize * extraLines +
        extraLines * 8f + renderedWidth * 0.08f
    return renderedHeight * canvasWidthPx / renderedWidth
}

/**
 * Mirrors [CollageRenderer.buildCaptionLines] so the live Compose preview shows exactly the
 * same caption rows as the exported PNG (including dedup of the coordinate fallback).
 */
private fun buildCaptionLinesForPreview(
    photo: GeoPhoto?,
    showDate: Boolean,
    showCoordinates: Boolean,
    customCaption: String,
    showCaption: Boolean,
): List<String> {
    if (!showCaption || photo == null) return emptyList()
    val address = photo.location?.addressText?.takeIf { it.isNotBlank() }
    val note = photo.note?.takeIf { it.isNotBlank() }
    val coordsLine = photo.location
        ?.takeIf { showCoordinates }
        ?.let { DateTimeFormatters.coordinates(it.latitude, it.longitude) }
    val dateLine = if (showDate) DateTimeFormatters.photoDateTime(photo.capturedAtMillis) else null
    val customLines = customCaption
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(MAX_PREVIEW_CAPTION_LINES)
        .toList()
    val title = address
        ?: note
        ?: photo.location
            ?.takeIf { coordsLine == null }
            ?.let { "%.5f, %.5f".format(java.util.Locale.US, it.latitude, it.longitude) }
    return buildList {
        if (customLines.isNotEmpty()) addAll(customLines) else if (title != null) add(title)
        if (dateLine != null) add(dateLine)
        if (coordsLine != null && coordsLine != title && coordsLine !in customLines) add(coordsLine)
    }.take(MAX_PREVIEW_CAPTION_LINES)
}

private const val MAX_PREVIEW_CAPTION_LINES = 4
private const val MAX_TEXT_LAYERS = 8

private val TEXT_COLOR_SWATCHES = listOf(
    0xFF17201D.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF2457C5.toInt(),
    0xFFE34646.toInt(),
    0xFFF5B700.toInt(),
    0xFF1B8A5A.toInt(),
    0xFF8B5CF6.toInt(),
)

private val TEXT_BACKGROUND_SWATCHES = listOf(
    0xFFFFFFFF.toInt(),
    0xFF17201D.toInt(),
    0xFFFFE9C7.toInt(),
    0xFFE0F4FF.toInt(),
    0xFFFDE2E4.toInt(),
    0xFFE7F5E9.toInt(),
    0xFFE3DAFE.toInt(),
)

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
private fun ColorSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
        )
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
            .size(64.dp)
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
                    contentDescription = stringResource(R.string.collage_editor_hero_photo_cd),
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
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
                contentDescription = stringResource(R.string.collage_editor_remove_from_collage_cd),
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PhotoCardTile(photo: GeoPhoto, darkStyle: Boolean) {
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
 * Live mini-map tile rendered inside the draggable layer. Composes a real [GoogleMap] so the
 * "Map camera" zoom slider and the "Tourist map" / "Dark style" toggles update the preview
 * in real time. The persisted `photo.mapSnapshotUri` is shown as a placeholder behind the
 * live map so we never flash an empty tile while tiles are loading.
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
                    stringResource(R.string.collage_editor_map_label),
                    color = if (darkStyle) Color(0xFFB8C8C1) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
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
        if (darkStyle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
        }
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
    captionStyle: CollageCaptionStyle,
    modifier: Modifier = Modifier,
) {
    val themedSurface = if (darkStyle) DARK_SURFACE else Color.White
    val themedTitle = if (darkStyle) DARK_ON_SURFACE else Color(0xFF17201D)
    val themedSub = if (darkStyle) DARK_ON_SURFACE_VARIANT else Color(0xFF50605A)
    val surfaceColor = captionStyle.backgroundColor?.let(::Color) ?: themedSurface
    val titleColor = captionStyle.textColor?.let(::Color) ?: themedTitle
    val subColor = captionStyle.textColor?.let(::Color) ?: themedSub
    val shape = when (captionStyle.shape) {
        CollageTextShape.PILL -> RoundedCornerShape(50)
        CollageTextShape.ROUNDED -> RoundedCornerShape(
            (14f * (captionStyle.cornerRadiusFrac / 0.026f)).coerceIn(0f, 36f).dp,
        )
        CollageTextShape.NONE, CollageTextShape.RECTANGLE -> RoundedCornerShape(0.dp)
    }
    val borderColor = captionStyle.borderColor?.let(::Color) ?: themedTitle
    Surface(
        modifier = modifier.then(
            if (captionStyle.borderWidthFrac > 0f) Modifier.border(2.dp, borderColor, shape)
            else Modifier,
        ),
        shape = shape,
        color = if (captionStyle.shape == CollageTextShape.NONE) Color.Transparent
        else surfaceColor.copy(alpha = captionStyle.backgroundOpacity),
        shadowElevation = if (captionStyle.shape == CollageTextShape.NONE) 0.dp else 4.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            lines.forEachIndexed { index, line ->
                if (index == 0) {
                    Text(
                        line,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = MaterialTheme.typography.titleSmall.fontSize * captionStyle.textSizeScale,
                        ),
                        color = titleColor,
                        maxLines = 1,
                    )
                } else {
                    Text(
                        line,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * captionStyle.textSizeScale,
                        ),
                        color = subColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

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
    originY: Float?,
    widthFrac: Float,
    rotationDeg: Float,
    fixedHeightPx: Float? = null,
    selected: Boolean,
    onSelect: () -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val originXPx = originX * canvasW
    var measuredHeightPx by remember(widthFrac) { mutableFloatStateOf(0f) }
    val originYPx = originY?.times(canvasH)
        ?: (canvasH - canvasW * 0.04f - measuredHeightPx).coerceAtLeast(0f)

    var pendingX by remember(originXPx) { mutableFloatStateOf(0f) }
    var pendingY by remember(originYPx) { mutableFloatStateOf(0f) }
    var pendingScale by remember(widthFrac) { mutableFloatStateOf(1f) }
    var pendingRotation by remember(rotationDeg) { mutableFloatStateOf(0f) }

    val leftPx = originXPx + pendingX
    val topPx = originYPx + pendingY
    val effectiveWidthFrac = (widthFrac * pendingScale).coerceIn(0.3f, 0.9f)
    val widthPx = effectiveWidthFrac * canvasW

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .rotate(rotationDeg + pendingRotation)
            .then(
                fixedHeightPx?.let { heightPx ->
                    Modifier.height(with(density) { heightPx.toDp() })
                } ?: Modifier,
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .onGloballyPositioned { measuredHeightPx = it.size.height.toFloat() }
            .pointerInput(canvasW, canvasH, originX, originY, widthFrac, rotationDeg) {
                detectLayerTransformGestures(
                    onStart = onSelect,
                    onEnd = { changed ->
                        if (changed) {
                            val finalWidthFrac = (widthFrac * pendingScale).coerceIn(0.3f, 0.9f)
                            val maxX = (1f - finalWidthFrac).coerceAtLeast(0f)
                            val heightFrac = if (canvasH > 0) measuredHeightPx / canvasH else 0f
                            val maxY = (1f - heightFrac).coerceAtLeast(0f)
                            onTransform(
                                ((originXPx + pendingX) / canvasW).coerceIn(0f, maxX),
                                ((originYPx + pendingY) / canvasH).coerceIn(0f, maxY),
                                finalWidthFrac,
                                rotationDeg + pendingRotation,
                            )
                        }
                        pendingX = 0f
                        pendingY = 0f
                        pendingScale = 1f
                        pendingRotation = 0f
                    },
                    onCancel = {
                        pendingX = 0f
                        pendingY = 0f
                        pendingScale = 1f
                        pendingRotation = 0f
                    },
                    onTransform = { pan, zoom, rotation ->
                        pendingX += pan.x
                        pendingY += pan.y
                        pendingScale = (pendingScale * zoom).coerceIn(
                            0.3f / widthFrac,
                            0.9f / widthFrac,
                        )
                        pendingRotation += rotation
                    },
                )
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
    onTransform: (Float, Float, Float, Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val shorter = min(canvasW, canvasH)
    val centerXPx = centerX * canvasW
    val centerYPx = centerY * canvasH

    var pendingX by remember(centerXPx) { mutableFloatStateOf(0f) }
    var pendingY by remember(centerYPx) { mutableFloatStateOf(0f) }
    var pendingScale by remember(widthFrac) { mutableFloatStateOf(1f) }
    var pendingRotation by remember(rotationDeg) { mutableFloatStateOf(0f) }

    val effectiveWidthFrac = (widthFrac * pendingScale).coerceIn(0.14f, 0.7f)
    val sizePx = effectiveWidthFrac * shorter
    val leftPx = centerXPx - sizePx / 2f + pendingX
    val topPx = centerYPx - sizePx / 2f + pendingY

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .size(with(density) { sizePx.toDp() })
            .rotate(rotationDeg + pendingRotation)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        content()
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(canvasW, canvasH, centerX, centerY, widthFrac, rotationDeg) {
                    detectLayerTransformGestures(
                        onStart = onSelect,
                        onEnd = { changed ->
                            if (changed) {
                                val finalWidthFrac = (widthFrac * pendingScale).coerceIn(0.14f, 0.7f)
                                val finalSizePx = finalWidthFrac * shorter
                                val halfWidthFrac = (finalSizePx / 2f / canvasW).coerceAtMost(0.5f)
                                val halfHeightFrac = (finalSizePx / 2f / canvasH).coerceAtMost(0.5f)
                                val newCx = ((centerXPx + pendingX) / canvasW)
                                    .coerceIn(halfWidthFrac, 1f - halfWidthFrac)
                                val newCy = ((centerYPx + pendingY) / canvasH)
                                    .coerceIn(halfHeightFrac, 1f - halfHeightFrac)
                                onTransform(
                                    newCx,
                                    newCy,
                                    finalWidthFrac,
                                    rotationDeg + pendingRotation,
                                )
                            }
                            pendingX = 0f
                            pendingY = 0f
                            pendingScale = 1f
                            pendingRotation = 0f
                        },
                        onCancel = {
                            pendingX = 0f
                            pendingY = 0f
                            pendingScale = 1f
                            pendingRotation = 0f
                        },
                        onTransform = { pan, zoom, rotation ->
                            pendingX += pan.x
                            pendingY += pan.y
                            pendingScale = (pendingScale * zoom).coerceIn(
                                0.14f / widthFrac,
                                0.7f / widthFrac,
                            )
                            pendingRotation += rotation
                        },
                    )
                },
        )
    }
}

/**
 * One pointer pans a layer. Two pointers additionally scale and rotate it around its centre.
 * Changes stay local until all pointers are lifted, so bitmap previews are rendered once per
 * gesture instead of once per pointer event.
 */
internal suspend fun PointerInputScope.detectLayerTransformGestures(
    onStart: () -> Unit,
    onTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onEnd: (changed: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        onStart()
        var changed = false
        var cancelled = false
        do {
            val event = awaitPointerEvent()
            cancelled = event.changes.any { it.isConsumed }
            if (!cancelled) {
                val pan = event.calculatePan()
                val zoom = event.calculateZoom()
                val rotation = event.calculateRotation()
                if (pan != Offset.Zero || zoom != 1f || rotation != 0f) {
                    changed = true
                    onTransform(pan, zoom, rotation)
                    event.changes.forEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                }
            }
        } while (!cancelled && event.changes.any { it.pressed })

        if (cancelled) onCancel() else onEnd(changed)
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
private fun EditorChip(
    selected: Boolean,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
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
                stringResource(R.string.collage_editor_map_camera),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(
                    when {
                        zoom <= 12 -> R.string.collage_editor_zoom_overview
                        zoom >= 16 -> R.string.collage_editor_zoom_close
                        else -> R.string.collage_editor_zoom_balanced
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onZoomChange(zoom - 1) },
            enabled = zoom > MIN_MAP_ZOOM,
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.collage_editor_higher_zoom_cd))
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
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.collage_editor_closer_zoom_cd))
        }
    }
}

private const val MIN_MAP_ZOOM = 11
private const val MAX_MAP_ZOOM = 17

// Keep in sync with CollageViewModel.MAX_SELECTION — the picker enforces the same cap and
// the editor's "+" chip is hidden once the user reaches the limit.
private const val MAX_COLLAGE_PHOTOS = 12

@Composable
private fun AddPhotoChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(R.string.collage_editor_add_photo_cd),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Bottom-sheet picker for adding more photos to the current collage without leaving the
 * editor. Lists every photo from the library that the user hasn't already added so taps map
 * directly to "add this photo".
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
                stringResource(R.string.collage_editor_add_photos),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subtitle = when {
                atSelectionLimit -> stringResource(R.string.collage_editor_at_limit, MAX_COLLAGE_PHOTOS)
                availablePhotos.isEmpty() -> stringResource(R.string.collage_editor_all_added)
                else -> stringResource(R.string.collage_editor_pick_to_add)
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
