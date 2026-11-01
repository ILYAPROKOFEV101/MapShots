package com.ilya.safarshots.data

import com.ilya.safarshots.data.local.entity.GeoPhotoEntity
import com.ilya.safarshots.data.model.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GeoPhotoMapperTest {
    @Test
    fun nullLocationMapsToDomainWithoutLocation() {
        val domain = entity(
            latitude = null,
            longitude = null,
            hasLocation = false,
        ).toDomain()

        assertNull(domain.location)
        assertFalse(domain.hasLocation)
        assertNull(domain.locationSubtitle)
    }

    @Test
    fun locationFieldsMapToPhotoLocation() {
        val domain = entity(
            latitude = 54.7104,
            longitude = 20.4522,
            hasLocation = true,
        ).toDomain()

        assertEquals(54.7104, domain.location?.latitude ?: 0.0, 0.00001)
        assertEquals(20.4522, domain.location?.longitude ?: 0.0, 0.00001)
        assertEquals("gps", domain.location?.provider)
    }

    private fun entity(
        latitude: Double?,
        longitude: Double?,
        hasLocation: Boolean,
    ) = GeoPhotoEntity(
        id = "photo-1",
        photoUri = "file:///photo.jpg",
        thumbnailUri = null,
        fileName = "photo.jpg",
        createdAtMillis = 1_700_000_000_000,
        latitude = latitude,
        longitude = longitude,
        altitude = null,
        accuracyMeters = 8f,
        addressText = null,
        cameraFacing = "back",
        width = 100,
        height = 120,
        orientationDegrees = 0,
        isFavorite = false,
        note = null,
        addedToCollageCount = 0,
        colorSeed = null,
        locationProvider = "gps",
        hasLocation = hasLocation,
        isApproximateLocation = false,
    )
}
