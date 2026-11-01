package com.ilya.safarshots.feature.gallery

import com.ilya.safarshots.core.common.DateLabels
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.domain.model.GeoPhoto

data class PhotoDateGroup(
    val title: String,
    val photos: List<GeoPhoto>,
)

object GalleryDateGrouper {
    fun group(
        photos: List<GeoPhoto>,
        labels: DateLabels,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<PhotoDateGroup> =
        photos
            .sortedByDescending { it.capturedAtMillis }
            .groupBy { DateTimeFormatters.galleryGroup(it.capturedAtMillis, labels, nowMillis) }
            .map { (title, groupedPhotos) -> PhotoDateGroup(title, groupedPhotos) }
}
