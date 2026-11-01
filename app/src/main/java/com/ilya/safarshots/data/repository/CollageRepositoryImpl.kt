package com.ilya.safarshots.data.repository

import android.graphics.Bitmap
import com.ilya.safarshots.data.local.CollageDao
import com.ilya.safarshots.data.local.entity.CollageEntity
import com.ilya.safarshots.data.storage.CollageFileWriter
import com.ilya.safarshots.domain.model.CollageExport
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.GeoPhoto
import com.ilya.safarshots.domain.repository.CollageRepository
import com.ilya.safarshots.feature.collage.CollageRenderer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollageRepositoryImpl @Inject constructor(
    private val renderer: CollageRenderer,
    private val fileWriter: CollageFileWriter,
    private val collageDao: CollageDao,
) : CollageRepository {
    override suspend fun renderPreview(photos: List<GeoPhoto>, options: CollageOptions): Bitmap =
        renderer.render(photos, options)

    override suspend fun exportCollage(photos: List<GeoPhoto>, options: CollageOptions): CollageExport {
        val bitmap = renderer.renderForExport(photos, options)
        val id = UUID.randomUUID().toString()
        val uri = fileWriter.writeToPictures(bitmap, "pinpix_${System.currentTimeMillis()}.png")
        val export = CollageExport(
            id = id,
            template = options.template,
            photoIds = photos.map { it.id },
            outputUri = uri.toString(),
            createdAtMillis = System.currentTimeMillis(),
            width = bitmap.width,
            height = bitmap.height,
        )
        collageDao.insert(
            CollageEntity(
                id = export.id,
                template = export.template.name,
                photoIdsCsv = export.photoIds.joinToString(","),
                outputUri = export.outputUri,
                createdAtMillis = export.createdAtMillis,
                width = export.width,
                height = export.height,
            ),
        )
        bitmap.recycle()
        return export
    }
}
