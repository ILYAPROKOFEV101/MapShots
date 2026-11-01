package com.ilya.safarshots.feature.collage

import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.GeoPhoto
import kotlin.math.roundToInt

/** Pixel dimensions used by the lossless, full-resolution export renderer. */
internal data class CollageExportSize(
    val width: Int,
    val height: Int,
)

/**
 * Keeps the selected aspect ratio while using as many source pixels as possible without
 * upscaling the hero photo. For a source whose ratio already matches the selected ratio this
 * returns the source's exact (orientation-corrected) dimensions.
 */
internal fun calculateHighQualityExportSize(
    photos: List<GeoPhoto>,
    options: CollageOptions,
): CollageExportSize {
    val fallback = CollageExportSize(options.aspectRatio.width, options.aspectRatio.height)
    val reference = options.backgroundPhotoId
        ?.let { id -> photos.firstOrNull { it.id == id } }
        ?: photos.firstOrNull()
        ?: return fallback

    val rawWidth = reference.width?.takeIf { it > 0 } ?: return fallback
    val rawHeight = reference.height?.takeIf { it > 0 } ?: return fallback
    val rotation = reference.orientationDegrees?.normalizedRightAngle() ?: 0
    val sourceWidth = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
    val sourceHeight = if (rotation == 90 || rotation == 270) rawWidth else rawHeight
    val targetRatio = options.aspectRatio.width.toDouble() / options.aspectRatio.height
    val sourceRatio = sourceWidth.toDouble() / sourceHeight

    return if (sourceRatio >= targetRatio) {
        CollageExportSize(
            width = (sourceHeight * targetRatio).roundToInt().coerceAtLeast(1),
            height = sourceHeight,
        )
    } else {
        CollageExportSize(
            width = sourceWidth,
            height = (sourceWidth / targetRatio).roundToInt().coerceAtLeast(1),
        )
    }
}

internal fun Int.normalizedRightAngle(): Int = ((this % 360) + 360) % 360
