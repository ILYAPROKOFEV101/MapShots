package com.ilya.safarshots.data.maps

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.ilya.safarshots.domain.model.PhotoLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapSnapshotStorage @Inject constructor(
    @ApplicationContext context: Context,
    private val client: StaticMapsClient,
) {
    private val dir = File(context.filesDir, "geo_photos/map_snapshots").apply { mkdirs() }

    val isConfigured: Boolean get() = client.isConfigured

    suspend fun captureFor(
        photoId: String,
        location: PhotoLocation,
        dark: Boolean = false,
    ): String? = withContext(Dispatchers.IO) {
        if (!client.isConfigured) return@withContext null
        val bitmap = client.fetch(
            locations = listOf(location),
            targetWidth = TARGET_PX,
            targetHeight = TARGET_PX,
            dark = dark,
            includeMarker = false,
        ) ?: return@withContext null
        saveBitmap(photoId, bitmap)
    }

    suspend fun saveBitmap(photoId: String, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            dir.mkdirs()
            val file = File(dir, "$photoId.jpg")
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            file.toUri().toString()
        }.getOrNull()
    }

    suspend fun deleteFor(photoId: String) = withContext(Dispatchers.IO) {
        File(dir, "$photoId.jpg").delete()
        Unit
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir.deleteRecursively()
        dir.mkdirs()
        Unit
    }

    fun directory(): File = dir

    private companion object {
        const val TARGET_PX = 1200
        const val JPEG_QUALITY = 88
    }
}
