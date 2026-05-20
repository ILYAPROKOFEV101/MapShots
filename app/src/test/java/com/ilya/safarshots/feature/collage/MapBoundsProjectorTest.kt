package com.ilya.safarshots.feature.collage

import com.ilya.safarshots.domain.model.PhotoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBoundsProjectorTest {
    @Test
    fun projectsCoordinatesIntoPaddedUnitSpace() {
        val points = MapBoundsProjector.project(
            listOf(
                PhotoLocation(latitude = 10.0, longitude = 20.0),
                PhotoLocation(latitude = 12.0, longitude = 24.0),
            ),
        )

        assertEquals(2, points.size)
        assertTrue(points.all { it.x in 0.08f..0.92f && it.y in 0.08f..0.92f })
        assertTrue(points.first().x < points.last().x)
        assertTrue(points.first().y > points.last().y)
    }

    @Test
    fun identicalCoordinatesRemainProjectable() {
        val points = MapBoundsProjector.project(
            listOf(
                PhotoLocation(latitude = 10.0, longitude = 20.0),
                PhotoLocation(latitude = 10.0, longitude = 20.0),
            ),
        )

        assertEquals(points.first().x, points.last().x)
        assertEquals(points.first().y, points.last().y)
    }
}
