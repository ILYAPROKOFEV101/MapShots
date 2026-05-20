package com.ilya.safarshots.feature.gallery

import com.ilya.safarshots.domain.model.GeoPhoto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GalleryDateGrouperTest {
    @Test
    fun groupsPhotosByFriendlyDateBuckets() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 12, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        val thisWeek = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -3) }.timeInMillis

        val groups = GalleryDateGrouper.group(
            listOf(photo("today", now), photo("yesterday", yesterday), photo("week", thisWeek)),
            nowMillis = now,
        )

        assertEquals(listOf("Today", "Yesterday", "This week"), groups.map { it.title })
    }

    private fun photo(id: String, capturedAt: Long) = GeoPhoto(
        id = id,
        imageUri = "file:///$id.jpg",
        thumbnailUri = null,
        fileName = "$id.jpg",
        capturedAtMillis = capturedAt,
        location = null,
        cameraFacing = "back",
        width = null,
        height = null,
        orientationDegrees = null,
        isFavorite = false,
        note = null,
        addedToCollageCount = 0,
        colorSeed = null,
    )
}
