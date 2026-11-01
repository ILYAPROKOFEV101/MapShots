package com.ilya.safarshots.data.storage

import android.content.Context
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ilya.safarshots.domain.model.PhotoLocation
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoStorageExifTest {
    @Test
    fun capturedLocationIsWrittenIntoOriginalJpeg() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = PhotoStorage(context)
        val file = File(context.cacheDir, "location-exif-test.jpg")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        } finally {
            bitmap.recycle()
        }
        val expected = PhotoLocation(
            latitude = 55.755826,
            longitude = 37.617300,
            altitude = 156.5,
            provider = "test",
        )

        try {
            assertTrue(storage.writeLocationExif(file, expected))
            val exif = ExifInterface(file)
            val coordinates = requireNotNull(exif.latLong)
            assertEquals(expected.latitude, coordinates[0], COORDINATE_DELTA)
            assertEquals(expected.longitude, coordinates[1], COORDINATE_DELTA)
            assertEquals(expected.altitude!!, exif.getAltitude(0.0), ALTITUDE_DELTA)
        } finally {
            file.delete()
        }
    }

    private companion object {
        const val COORDINATE_DELTA = 0.00001
        const val ALTITUDE_DELTA = 0.1
    }
}
