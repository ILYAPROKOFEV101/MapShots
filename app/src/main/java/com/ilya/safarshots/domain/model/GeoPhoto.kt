package com.ilya.safarshots.domain.model

data class GeoPhoto(
    val id: String,
    val imageUri: String,
    val thumbnailUri: String?,
    val fileName: String,
    val capturedAtMillis: Long,
    val location: PhotoLocation?,
    val cameraFacing: String?,
    val width: Int?,
    val height: Int?,
    val orientationDegrees: Int?,
    val isFavorite: Boolean,
    val note: String?,
    val addedToCollageCount: Int,
    val colorSeed: Int?,
    val mapSnapshotUri: String? = null,
) {
    val hasLocation: Boolean = location != null
    val displayTitle: String = note?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.')
    val displaySubtitle: String = location?.addressText ?: location?.let {
        "%.5f, %.5f".format(java.util.Locale.US, it.latitude, it.longitude)
    } ?: "No location"
}
