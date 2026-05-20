package com.ilya.safarshots.domain.model

enum class CollageTemplate(val title: String, val subtitle: String) {
    PHOTO_WITH_MAP("Photo + map", "Full-bleed photo with a corner mini map"),
    POSTCARD("Postcard", "One hero photo with a mini map"),
}

enum class CollageMapCorner(val label: String) {
    TOP_LEFT("Top-left"),
    TOP_RIGHT("Top-right"),
    BOTTOM_LEFT("Bottom-left"),
    BOTTOM_RIGHT("Bottom-right"),
}

enum class CollageAspectRatio(val label: String, val width: Int, val height: Int) {
    SQUARE("1:1", 1080, 1080),
    PORTRAIT("4:5", 1080, 1350),
    STORY("9:16", 1080, 1920),
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
 * for the PHOTO_WITH_MAP template and allows free drag/resize.
 */
data class CollageMapPlacement(
    val centerX: Float,
    val centerY: Float,
    val widthFrac: Float,
)

/**
 * Anchor point + width of the caption pill in the PHOTO_WITH_MAP template.
 *
 * - [originX], [originY]: top-left corner of the caption box, normalized to [0..1].
 * - [widthFrac]: caption width as a fraction of the full canvas width. The height is
 *   determined by the rendered content so the caption can grow with extra lines without
 *   the user having to reshape it.
 */
data class CollageCaptionPlacement(
    val originX: Float,
    val originY: Float,
    val widthFrac: Float,
)

data class CollageOptions(
    val template: CollageTemplate = CollageTemplate.PHOTO_WITH_MAP,
    val aspectRatio: CollageAspectRatio = CollageAspectRatio.SQUARE,
    val showDate: Boolean = true,
    // Coordinates are noisy by default: the caption already shows the address (or, failing
    // that, falls back to coordinates as the title). Users who want a second coords line can
    // turn this on from the editor.
    val showCoordinates: Boolean = false,
    val showMiniMap: Boolean = true,
    val darkStyle: Boolean = false,
    val mapCorner: CollageMapCorner = CollageMapCorner.BOTTOM_RIGHT,
    val mapZoom: Int = 14,
    val useTouristMapStyle: Boolean = true,
    val backgroundMode: CollageBackgroundMode = CollageBackgroundMode.HERO_PHOTO,
    val backgroundColor: Int = 0xFFEFE7DD.toInt(),
    val backgroundPhotoId: String? = null,
    val cards: List<CollageCard> = emptyList(),
    val mapPlacement: CollageMapPlacement? = null,
    val captionPlacement: CollageCaptionPlacement? = null,
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
