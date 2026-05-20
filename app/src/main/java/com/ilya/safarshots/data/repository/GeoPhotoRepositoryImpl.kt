package com.ilya.safarshots.data.repository

import android.graphics.Bitmap
import androidx.core.net.toUri
import com.ilya.safarshots.data.local.GeoPhotoDao
import com.ilya.safarshots.data.local.entity.GeoPhotoEntity
import com.ilya.safarshots.data.maps.MapSnapshotStorage
import com.ilya.safarshots.data.model.toDomain
import com.ilya.safarshots.data.storage.PhotoStorage
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoPhotoRepositoryImpl @Inject constructor(
    private val dao: GeoPhotoDao,
    private val photoStorage: PhotoStorage,
    private val mapSnapshotStorage: MapSnapshotStorage,
    private val applicationScope: CoroutineScope,
) : GeoPhotoRepository {
    override fun observePhotos(): Flow<List<GeoPhoto>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePhoto(id: String): Flow<GeoPhoto?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getPhoto(id: String): GeoPhoto? = dao.getById(id)?.toDomain()

    override suspend fun getPhotos(ids: List<String>): List<GeoPhoto> {
        val byId = dao.getByIds(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    override suspend fun saveCapturedPhoto(
        originalFile: File,
        location: PhotoLocation?,
        cameraFacing: String?,
    ): GeoPhoto = withContext(Dispatchers.IO) {
        // Fast path: only cheap metadata (header bounds + EXIF). No bitmap decode here.
        val metadata = photoStorage.readFastMetadata(originalFile)
        val entity = GeoPhotoEntity(
            id = UUID.randomUUID().toString(),
            photoUri = originalFile.toUri().toString(),
            thumbnailUri = null,
            fileName = originalFile.name,
            createdAtMillis = System.currentTimeMillis(),
            latitude = location?.latitude,
            longitude = location?.longitude,
            altitude = location?.altitude,
            accuracyMeters = location?.accuracyMeters,
            addressText = location?.addressText,
            cameraFacing = cameraFacing,
            width = metadata.width,
            height = metadata.height,
            orientationDegrees = metadata.orientationDegrees,
            isFavorite = false,
            note = null,
            addedToCollageCount = 0,
            colorSeed = null,
            locationProvider = location?.provider,
            hasLocation = location != null,
            isApproximateLocation = location?.isApproximate == true,
        )
        dao.upsert(entity)

        // Heavy enrichment runs in the background so the UI can navigate right away.
        applicationScope.launch(Dispatchers.IO) {
            runCatching {
                val thumbnailUri = photoStorage.createThumbnail(originalFile)
                if (thumbnailUri != null) dao.updateThumbnailUri(entity.id, thumbnailUri)
            }
            runCatching {
                val colorSeed = photoStorage.readColorSeedAsync(originalFile)
                if (colorSeed != null) dao.updateColorSeed(entity.id, colorSeed)
            }
            if (location != null) {
                runCatching {
                    val snapshotUri = mapSnapshotStorage.captureFor(entity.id, location)
                    if (snapshotUri != null) dao.updateMapSnapshotUri(entity.id, snapshotUri)
                }
            }
        }
        entity.toDomain()
    }

    override suspend fun ensureMapSnapshot(photoId: String, dark: Boolean): String? {
        val entity = dao.getById(photoId) ?: return null
        entity.mapSnapshotUri?.let { return it }
        val lat = entity.latitude ?: return null
        val lng = entity.longitude ?: return null
        val snapshotUri = mapSnapshotStorage.captureFor(
            photoId = entity.id,
            location = PhotoLocation(latitude = lat, longitude = lng),
            dark = dark,
        ) ?: return null
        dao.updateMapSnapshotUri(entity.id, snapshotUri)
        return snapshotUri
    }

    override suspend fun saveMapSnapshot(photoId: String, bitmap: Bitmap): String? {
        val uri = mapSnapshotStorage.saveBitmap(photoId, bitmap) ?: return null
        dao.updateMapSnapshotUri(photoId, uri)
        return uri
    }

    override suspend fun toggleFavorite(id: String) = dao.toggleFavorite(id)

    override suspend fun updateNote(id: String, note: String?) = dao.updateNote(id, note)

    override suspend fun updateAddressText(id: String, addressText: String?) =
        dao.updateAddressText(id, addressText)

    override suspend fun markAddedToCollage(ids: List<String>) {
        if (ids.isNotEmpty()) dao.incrementCollageCount(ids)
    }

    override suspend fun deletePhoto(id: String) {
        val entity = dao.getById(id) ?: return
        dao.deleteById(id)
        photoStorage.deletePhotoFiles(entity.photoUri, entity.thumbnailUri)
        mapSnapshotStorage.deleteFor(entity.id)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
        photoStorage.deleteAllPhotoFiles()
        mapSnapshotStorage.deleteAll()
    }
}
