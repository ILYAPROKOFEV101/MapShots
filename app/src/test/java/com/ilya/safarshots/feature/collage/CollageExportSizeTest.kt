package com.ilya.safarshots.feature.collage

import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.GeoPhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class CollageExportSizeTest {
    @Test
    fun matchingRatioKeepsEverySourcePixel() {
        val size = calculateHighQualityExportSize(
            photos = listOf(photo(width = 3024, height = 4032)),
            options = CollageOptions(aspectRatio = CollageAspectRatio.CLASSIC_PORTRAIT),
        )

        assertEquals(CollageExportSize(3024, 4032), size)
    }

    @Test
    fun cropToSelectedRatioNeverUpscalesSource() {
        val size = calculateHighQualityExportSize(
            photos = listOf(photo(width = 4032, height = 3024)),
            options = CollageOptions(aspectRatio = CollageAspectRatio.SQUARE),
        )

        assertEquals(CollageExportSize(3024, 3024), size)
    }

    @Test
    fun exifRotationIsAppliedBeforeChoosingExportDimensions() {
        val size = calculateHighQualityExportSize(
            photos = listOf(photo(width = 4032, height = 3024, orientationDegrees = 90)),
            options = CollageOptions(aspectRatio = CollageAspectRatio.CLASSIC_PORTRAIT),
        )

        assertEquals(CollageExportSize(3024, 4032), size)
    }

    @Test
    fun missingMetadataFallsBackToConfiguredCanvas() {
        val size = calculateHighQualityExportSize(
            photos = listOf(photo(width = null, height = null)),
            options = CollageOptions(aspectRatio = CollageAspectRatio.STORY),
        )

        assertEquals(CollageExportSize(1080, 1920), size)
    }

    private fun photo(
        width: Int?,
        height: Int?,
        orientationDegrees: Int = 0,
    ) = GeoPhoto(
        id = "photo",
        imageUri = "file:///original.jpg",
        thumbnailUri = "file:///thumbnail.jpg",
        fileName = "original.jpg",
        capturedAtMillis = 0L,
        location = null,
        cameraFacing = null,
        width = width,
        height = height,
        orientationDegrees = orientationDegrees,
        isFavorite = false,
        note = null,
        addedToCollageCount = 0,
        colorSeed = null,
    )
}
