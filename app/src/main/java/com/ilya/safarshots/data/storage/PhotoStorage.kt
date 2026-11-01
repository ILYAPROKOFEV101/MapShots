package com.ilya.safarshots.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.ilya.safarshots.domain.model.PhotoLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val originalsDir = File(context.filesDir, "geo_photos/originals")
    private val thumbnailsDir = File(context.filesDir, "geo_photos/thumbnails")

    init {
        originalsDir.mkdirs()
        thumbnailsDir.mkdirs()
    }

    fun createOriginalFile(nowMillis: Long = System.currentTimeMillis(), extension: String = "jpg"): File {
        originalsDir.mkdirs()
        return File(originalsDir, "${nowMillis}_${UUID.randomUUID()}.$extension")
    }

    suspend fun importFromUri(sourceUri: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = Uri.parse(sourceUri)
            val extension = context.contentResolver.getType(parsed)
                ?.substringAfterLast('/', "")
                ?.lowercase()
                ?.takeIf { it in setOf("jpeg", "jpg", "png", "webp", "heic") }
                ?.let { if (it == "jpeg") "jpg" else it }
                ?: "jpg"
            val outputFile = createOriginalFile(extension = extension)
            try {
                context.contentResolver.openInputStream(parsed)?.use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
            } catch (error: Throwable) {
                outputFile.delete()
                throw error
            }
            outputFile
        }.getOrNull()
    }

    suspend fun createThumbnail(originalFile: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(originalFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 512, 512)
            val bitmap = BitmapFactory.decodeFile(
                originalFile.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return@withContext null
            val rotated = rotateIfNeeded(bitmap, readOrientationDegrees(originalFile))
            val thumb = centerCrop(rotated, 512, 512)
            val thumbnailFile = File(thumbnailsDir, originalFile.nameWithoutExtension + "_thumb.jpg")
            thumbnailFile.outputStream().use { output ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 86, output)
            }
            if (thumb !== rotated) thumb.recycle()
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()
            thumbnailFile.toUri().toString()
        }.getOrNull()
    }

    suspend fun readMetadata(file: File): PhotoMetadata = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        PhotoMetadata(
            width = bounds.outWidth.takeIf { it > 0 },
            height = bounds.outHeight.takeIf { it > 0 },
            orientationDegrees = readOrientationDegrees(file),
            colorSeed = readColorSeed(file),
        )
    }

    // Cheap metadata: only JPEG header + EXIF, no full decode. Used on the hot capture path.
    suspend fun readFastMetadata(file: File): PhotoMetadata = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        PhotoMetadata(
            width = bounds.outWidth.takeIf { it > 0 },
            height = bounds.outHeight.takeIf { it > 0 },
            orientationDegrees = readOrientationDegrees(file),
            colorSeed = null,
        )
    }

    suspend fun readColorSeedAsync(file: File): Int? = withContext(Dispatchers.IO) {
        readColorSeed(file)
    }

    /** Persists the capture location in the original image as standard GPS EXIF metadata. */
    suspend fun writeLocationExif(file: File, location: PhotoLocation): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ExifInterface(file).apply {
                setLatLong(location.latitude, location.longitude)
                location.altitude?.let(::setAltitude)
                saveAttributes()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun deletePhotoFiles(photoUri: String, thumbnailUri: String?) = withContext(Dispatchers.IO) {
        deleteUriFile(photoUri)
        thumbnailUri?.let(::deleteUriFile)
    }

    suspend fun deleteAllPhotoFiles() = withContext(Dispatchers.IO) {
        originalsDir.deleteRecursively()
        thumbnailsDir.deleteRecursively()
        originalsDir.mkdirs()
        thumbnailsDir.mkdirs()
    }

    fun getPhotoDirectory(): File = originalsDir

    fun getThumbnailDirectory(): File = thumbnailsDir

    private fun deleteUriFile(uri: String) {
        val parsed = uri.toUri()
        if (parsed.scheme == "file") {
            File(parsed.path.orEmpty()).delete()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
        return max(1, inSampleSize)
    }

    private fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
        val scaledWidth = scale * source.width
        val scaledHeight = scale * source.height
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(source, Matrix().apply {
            setScale(scale, scale)
            postTranslate(left, top)
        }, null)
        return output
    }

    private fun rotateIfNeeded(source: Bitmap, degrees: Int?): Bitmap {
        val rotation = degrees ?: return source
        if (rotation == 0) return source
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply {
            postRotate(rotation.toFloat())
        }, true)
    }

    private fun readOrientationDegrees(file: File): Int? = runCatching {
        when (ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrNull()

    private fun readColorSeed(file: File): Int? = runCatching {
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = 32 },
        ) ?: return@runCatching null
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        val step = max(1, bitmap.width / 16)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                red += Color.red(color)
                green += Color.green(color)
                blue += Color.blue(color)
                count++
                x += step
            }
            y += step
        }
        bitmap.recycle()
        if (count == 0L) null else Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    }.getOrNull()
}
