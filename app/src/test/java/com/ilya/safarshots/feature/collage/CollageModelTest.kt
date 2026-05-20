package com.ilya.safarshots.feature.collage

import com.ilya.safarshots.domain.model.CollageAspectRatio
import com.ilya.safarshots.domain.model.CollageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageModelTest {
    @Test
    fun includesExpectedMvpTemplates() {
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.POSTCARD))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.MAP_STORY))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.JOURNEY_TIMELINE))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.PHOTO_GRID_WITH_MAP))
        assertTrue(CollageTemplate.entries.contains(CollageTemplate.POLAROID_MAP))
    }

    @Test
    fun supportedExportSizesMatchRequirements() {
        assertEquals(1080 to 1080, CollageAspectRatio.SQUARE.width to CollageAspectRatio.SQUARE.height)
        assertEquals(1080 to 1350, CollageAspectRatio.PORTRAIT.width to CollageAspectRatio.PORTRAIT.height)
        assertEquals(1080 to 1920, CollageAspectRatio.STORY.width to CollageAspectRatio.STORY.height)
        assertEquals(1920 to 1080, CollageAspectRatio.LANDSCAPE.width to CollageAspectRatio.LANDSCAPE.height)
    }
}
