package com.ilya.safarshots.domain.model

enum class CollageTemplate(val title: String, val subtitle: String) {
    PHOTO_WITH_MAP("Photo + map", "Full-bleed photo with a corner mini map"),
    POSTCARD("Postcard", "One hero photo with a mini map"),
    MAP_STORY("Map story", "Photos wrapped around a route"),
    JOURNEY_TIMELINE("Journey timeline", "Dates, places, and a path"),
    PHOTO_GRID_WITH_MAP("Grid + map", "A clean travel-board layout"),
    POLAROID_MAP("Polaroid map", "Playful cards on a soft surface"),
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

data class CollageOptions(
    val template: CollageTemplate = CollageTemplate.PHOTO_WITH_MAP,
    val aspectRatio: CollageAspectRatio = CollageAspectRatio.SQUARE,
    val showDate: Boolean = true,
    val showCoordinates: Boolean = true,
    val showMiniMap: Boolean = true,
    val darkStyle: Boolean = false,
    val mapCorner: CollageMapCorner = CollageMapCorner.BOTTOM_RIGHT,
    val mapZoom: Int = 14,
    val useTouristMapStyle: Boolean = true,
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
