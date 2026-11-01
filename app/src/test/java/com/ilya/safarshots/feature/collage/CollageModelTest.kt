package com.ilya.safarshots.feature.collage

import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageCaptionStyle
import com.ilya.safarshots.domain.model.CollageCaptionPlacement
import com.ilya.safarshots.domain.model.CollageCard
import com.ilya.safarshots.domain.model.CollageMapPlacement
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.CollageTextLayer
import com.ilya.safarshots.domain.model.CollageTextShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageModelTest {
    @Test
    fun includesExpectedMvpTemplates() {
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.PHOTO_WITH_MAP))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.SPLIT_VERTICAL))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.SPLIT_HORIZONTAL))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.GRID))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.MAGAZINE))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.POSTCARD))
        assertEquals(6, CollageTemplate.entries.size)
        assertTrue(CollageTemplate.entries.all { it.supportsMiniMap })
    }

    @Test
    fun geotaggedPhotoShowsMapByDefaultInEveryTemplate() {
        assertTrue(
            CollageTemplate.entries.all { template ->
                shouldShowMiniMapByDefault(
                    hasGeotaggedPhoto = true,
                    template = template,
                )
            },
        )
    }

    @Test
    fun templatesWithoutGeotagDoNotForceMapUnlessMapIsPartOfTheirConcept() {
        assertEquals(
            false,
            shouldShowMiniMapByDefault(
                hasGeotaggedPhoto = false,
                template = CollageTemplate.SPLIT_VERTICAL,
            ),
        )
        assertTrue(
            shouldShowMiniMapByDefault(
                hasGeotaggedPhoto = false,
                template = CollageTemplate.PHOTO_WITH_MAP,
            ),
        )
        assertTrue(
            shouldShowMiniMapByDefault(
                hasGeotaggedPhoto = false,
                template = CollageTemplate.POSTCARD,
            ),
        )
    }

    @Test
    fun supportedExportSizesMatchRequirements() {
        assertEquals(1080 to 1080, CollageAspectRatio.SQUARE.width to CollageAspectRatio.SQUARE.height)
        assertEquals(1080 to 1350, CollageAspectRatio.PORTRAIT.width to CollageAspectRatio.PORTRAIT.height)
        assertEquals(1080 to 1920, CollageAspectRatio.STORY.width to CollageAspectRatio.STORY.height)
        assertEquals(1920 to 1080, CollageAspectRatio.LANDSCAPE.width to CollageAspectRatio.LANDSCAPE.height)
    }

    @Test
    fun captionCanBeCustomizedOrHiddenWithoutChangingPhotoMetadata() {
        val customized = CollageOptions(customCaption = "Weekend in Kazan")
        assertTrue(customized.showCaption)
        assertEquals("Weekend in Kazan", customized.customCaption)

        val hidden = customized.copy(showCaption = false)
        assertEquals(false, hidden.showCaption)
        assertEquals("Weekend in Kazan", hidden.customCaption)
    }

    @Test
    fun severalTextLayersKeepIndependentContentPositionAndStyle() {
        val title = CollageTextLayer(
            text = "Moscow",
            originX = 0.08f,
            originY = 0.12f,
            textColor = 0xFFFFFFFF.toInt(),
            backgroundColor = 0xFF17201D.toInt(),
            shape = CollageTextShape.PILL,
        )
        val note = CollageTextLayer(
            text = "Evening walk",
            originX = 0.34f,
            originY = 0.72f,
            borderWidthFrac = 0.004f,
            shape = CollageTextShape.ROUNDED,
        )
        val options = CollageOptions(textLayers = listOf(title, note))

        assertEquals(2, options.textLayers.size)
        assertEquals("Moscow", options.textLayers.first().text)
        assertEquals(CollageTextShape.PILL, options.textLayers.first().shape)
        assertEquals(0.72f, options.textLayers.last().originY)
        assertTrue(options.textLayers.last().borderWidthFrac > 0f)
    }

    @Test
    fun baseCaptionStyleCanBeCustomizedIndependently() {
        val options = CollageOptions(
            captionStyle = CollageCaptionStyle(
                textSizeScale = 1.35f,
                textColor = 0xFFFFFFFF.toInt(),
                backgroundColor = 0xFF2457C5.toInt(),
                borderWidthFrac = 0.004f,
                shape = CollageTextShape.PILL,
            ),
        )

        assertEquals(1.35f, options.captionStyle.textSizeScale)
        assertEquals(CollageTextShape.PILL, options.captionStyle.shape)
        assertTrue(options.captionStyle.borderWidthFrac > 0f)
    }

    @Test
    fun everyFloatingLayerKeepsItsIndependentScaleAndRotation() {
        val options = CollageOptions(
            cards = listOf(
                CollageCard(
                    photoId = "photo",
                    centerX = 0.25f,
                    centerY = 0.3f,
                    widthFrac = 0.42f,
                    rotationDeg = -18f,
                ),
            ),
            mapPlacement = CollageMapPlacement(
                centerX = 0.72f,
                centerY = 0.7f,
                widthFrac = 0.33f,
                rotationDeg = 12f,
            ),
            captionPlacement = CollageCaptionPlacement(
                originX = 0.08f,
                originY = 0.74f,
                widthFrac = 0.58f,
                rotationDeg = 6f,
            ),
            textLayers = listOf(
                CollageTextLayer(
                    text = "Rotated text",
                    originX = 0.14f,
                    originY = 0.18f,
                    widthFrac = 0.44f,
                    rotationDeg = 24f,
                ),
            ),
        )

        assertEquals(-18f, options.cards.single().rotationDeg)
        assertEquals(0.33f, options.mapPlacement?.widthFrac)
        assertEquals(12f, options.mapPlacement?.rotationDeg)
        assertEquals(6f, options.captionPlacement?.rotationDeg)
        assertEquals(24f, options.textLayers.single().rotationDeg)
    }
}
