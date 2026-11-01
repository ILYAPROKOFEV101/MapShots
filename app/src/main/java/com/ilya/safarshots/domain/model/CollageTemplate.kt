package com.ilya.safarshots.domain.model

import androidx.annotation.StringRes
import com.ilya.safarshots.R

enum class CollageTemplate(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val minimumPhotos: Int,
    val supportsMiniMap: Boolean = true,
) {
    PHOTO_WITH_MAP(
        R.string.template_photo_with_map,
        R.string.template_photo_with_map_subtitle,
        minimumPhotos = 1,
        supportsMiniMap = true,
    ),
    SPLIT_VERTICAL(
        R.string.template_split_vertical,
        R.string.template_split_vertical_subtitle,
        minimumPhotos = 2,
    ),
    SPLIT_HORIZONTAL(
        R.string.template_split_horizontal,
        R.string.template_split_horizontal_subtitle,
        minimumPhotos = 2,
    ),
    GRID(
        R.string.template_grid,
        R.string.template_grid_subtitle,
        minimumPhotos = 2,
    ),
    MAGAZINE(
        R.string.template_magazine,
        R.string.template_magazine_subtitle,
        minimumPhotos = 3,
    ),
    POSTCARD(
        R.string.template_postcard,
        R.string.template_postcard_subtitle,
        minimumPhotos = 1,
        supportsMiniMap = true,
    ),
}

enum class CollageMapCorner(@StringRes val labelRes: Int) {
    TOP_LEFT(R.string.corner_top_left),
    TOP_RIGHT(R.string.corner_top_right),
    BOTTOM_LEFT(R.string.corner_bottom_left),
    BOTTOM_RIGHT(R.string.corner_bottom_right),
}

// Aspect ratio labels are language-neutral numeric strings; no need to localize.
enum class CollageAspectRatio(val label: String, val width: Int, val height: Int) {
    SQUARE("1:1", 1080, 1080),
    PORTRAIT("4:5", 1080, 1350),
    CLASSIC_PORTRAIT("3:4", 1080, 1440),
    STORY("9:16", 1080, 1920),
    CLASSIC_LANDSCAPE("4:3", 1440, 1080),
    LANDSCAPE("16:9", 1920, 1080),
}

enum class CollageBackgroundMode {
    HERO_PHOTO,
    SOLID_COLOR,
}

/**
 * A draggable element placed on top of the collage hero canvas.
 * Coordinates are normalized to [0..1] relative to the rendered canvas.
 *
 * - [centerX], [centerY]: normalized center of the card.
 * - [widthFrac]: card width as a fraction of the canvas's shorter side.
 * - [rotationDeg]: rotation around the card's center, in degrees.
 */
data class CollageCard(
    val photoId: String,
    val centerX: Float,
    val centerY: Float,
    val widthFrac: Float,
    val rotationDeg: Float = 0f,
)

/**
 * Position of the mini-map card. When non-null, overrides legacy [CollageOptions.mapCorner]
 * and allows free drag, pinch-resize, and rotation.
 */
data class CollageMapPlacement(
    val centerX: Float,
    val centerY: Float,
    val widthFrac: Float,
    val rotationDeg: Float = 0f,
)

/**
 * Anchor point + width of the caption pill in the PHOTO_WITH_MAP template.
 *
 * - [originX], [originY]: top-left corner of the caption box, normalized to [0..1].
 * - [widthFrac]: caption width as a fraction of the full canvas width. The height is
 *   determined by the rendered content so the caption can grow with extra lines without
 *   the user having to reshape it.
 * - [rotationDeg]: rotation around the caption centre, in degrees.
 */
data class CollageCaptionPlacement(
    val originX: Float,
    val originY: Float,
    val widthFrac: Float,
    val rotationDeg: Float = 0f,
)

enum class CollageTextShape {
    NONE,
    RECTANGLE,
    ROUNDED,
    PILL,
}

/**
 * An independent, draggable text field rendered above every collage template.
 * Positions and sizes are normalized so the editor preview and exported bitmap use the
 * same layout regardless of the selected aspect ratio. [rotationDeg] is applied around the
 * rendered text box centre.
 */
data class CollageTextLayer(
    val text: String,
    val originX: Float,
    val originY: Float,
    val widthFrac: Float = 0.5f,
    val rotationDeg: Float = 0f,
    val textSizeFrac: Float = 0.042f,
    val textColor: Int = 0xFF17201D.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundOpacity: Float = 0.9f,
    val borderColor: Int = 0xFF17201D.toInt(),
    val borderWidthFrac: Float = 0f,
    val cornerRadiusFrac: Float = 0.035f,
    val shape: CollageTextShape = CollageTextShape.ROUNDED,
)

/** Visual settings for the automatic photo caption (address/date/coordinates). */
data class CollageCaptionStyle(
    val textSizeScale: Float = 1f,
    /** Null keeps the light/dark theme colour. */
    val textColor: Int? = null,
    /** Null keeps the light/dark theme surface colour. */
    val backgroundColor: Int? = null,
    val backgroundOpacity: Float = 1f,
    /** Null keeps the light/dark theme foreground colour. */
    val borderColor: Int? = null,
    val borderWidthFrac: Float = 0f,
    val cornerRadiusFrac: Float = 0.026f,
    val shape: CollageTextShape = CollageTextShape.ROUNDED,
)

data class CollageOptions(
    val template: CollageTemplate = CollageTemplate.PHOTO_WITH_MAP,
    val aspectRatio: CollageAspectRatio = CollageAspectRatio.SQUARE,
    val showCaption: Boolean = true,
    /** Optional collage-only text. It does not overwrite the photo note or address. */
    val customCaption: String = "",
    val showDate: Boolean = true,
    // Coordinates are noisy by default: the caption already shows the address (or, failing
    // that, falls back to coordinates as the title). Users who want a second coords line can
    // turn this on from the editor.
    val showCoordinates: Boolean = false,
    val showMiniMap: Boolean = true,
    /** Renderer-only switch used when Compose draws the same map as a draggable overlay. */
    val renderMiniMap: Boolean = true,
    /** Renderer-only switch used when Compose draws draggable text fields in the preview. */
    val renderTextLayers: Boolean = true,
    val darkStyle: Boolean = false,
    val mapCorner: CollageMapCorner = CollageMapCorner.BOTTOM_RIGHT,
    val mapZoom: Int = 14,
    val useTouristMapStyle: Boolean = true,
    val backgroundMode: CollageBackgroundMode = CollageBackgroundMode.HERO_PHOTO,
    val backgroundColor: Int = 0xFFEFE7DD.toInt(),
    val backgroundPhotoId: String? = null,
    /** Photo whose geo position is used for the optional map layer. */
    val mapPhotoId: String? = null,
    val cards: List<CollageCard> = emptyList(),
    val mapPlacement: CollageMapPlacement? = null,
    val captionPlacement: CollageCaptionPlacement? = null,
    val captionStyle: CollageCaptionStyle = CollageCaptionStyle(),
    val textLayers: List<CollageTextLayer> = emptyList(),
)

data class CollageExport(
    val id: String,
    val template: CollageTemplate,
    val photoIds: List<String>,
    val outputUri: String,
    val createdAtMillis: Long,
    val width: Int,
    val height: Int,
)
