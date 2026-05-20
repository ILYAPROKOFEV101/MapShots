package com.ilya.safarshots.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geo_photos")
data class GeoPhotoEntity(
    @PrimaryKey val id: String,
    val photoUri: String,
    val thumbnailUri: String?,
    val fileName: String,
    val createdAtMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val accuracyMeters: Float?,
    val addressText: String?,
    val cameraFacing: String?,
    val width: Int?,
    val height: Int?,
    val orientationDegrees: Int?,
    val isFavorite: Boolean,
    val note: String?,
    val addedToCollageCount: Int,
    val colorSeed: Int?,
    val locationProvider: String?,
    val hasLocation: Boolean,
    val isApproximateLocation: Boolean,
    val mapSnapshotUri: String? = null,
)
