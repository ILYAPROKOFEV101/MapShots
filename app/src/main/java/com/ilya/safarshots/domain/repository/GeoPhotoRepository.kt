package com.ilya.safarshots.domain.repository

import android.graphics.Bitmap
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.model.PhotoLocation
import kotlinx.coroutines.flow.Flow
import java.io.File

interface GeoPhotoRepository {
    fun observePhotos(): Flow<List<GeoPhoto>>
    fun observePhoto(id: String): Flow<GeoPhoto?>
    suspend fun getPhoto(id: String): GeoPhoto?
    suspend fun getPhotos(ids: List<String>): List<GeoPhoto>
    suspend fun saveImportedPhoto(
        sourceUri: String,
        location: PhotoLocation?,
    ): GeoPhoto
    suspend fun saveCapturedPhoto(
        originalFile: File,
        location: PhotoLocation?,
        cameraFacing: String?,
    ): GeoPhoto
    suspend fun updatePhotoLocation(id: String, location: PhotoLocation?)
    suspend fun toggleFavorite(id: String)
    suspend fun updateNote(id: String, note: String?)
    suspend fun updateAddressText(id: String, addressText: String?)
    suspend fun markAddedToCollage(ids: List<String>)
    suspend fun deletePhoto(id: String)
    suspend fun deleteAll()
    suspend fun ensureMapSnapshot(photoId: String, dark: Boolean = false): String?
    suspend fun saveMapSnapshot(photoId: String, bitmap: Bitmap): String?
}
