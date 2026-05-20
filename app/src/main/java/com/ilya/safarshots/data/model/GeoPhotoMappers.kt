package com.ilya.safarshots.data.model

import com.ilya.safarshots.data.local.entity.GeoPhotoEntity
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.model.PhotoLocation

fun GeoPhotoEntity.toDomain(): GeoPhoto = GeoPhoto(
    id = id,
    imageUri = photoUri,
    thumbnailUri = thumbnailUri,
    fileName = fileName,
    capturedAtMillis = createdAtMillis,
    location = if (hasLocation && latitude != null && longitude != null) {
        PhotoLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracyMeters = accuracyMeters,
            addressText = addressText,
            provider = locationProvider,
            isApproximate = isApproximateLocation,
        )
    } else {
        null
    },
    cameraFacing = cameraFacing,
    width = width,
    height = height,
    orientationDegrees = orientationDegrees,
    isFavorite = isFavorite,
    note = note,
    addedToCollageCount = addedToCollageCount,
    colorSeed = colorSeed,
    mapSnapshotUri = mapSnapshotUri,
)
