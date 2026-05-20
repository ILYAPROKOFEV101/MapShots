package com.ilya.safarshots.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.ilya.safarshots.domain.model.GeoPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberPhotoMarkerIcon(photo: GeoPhoto, selected: Boolean): State<BitmapDescriptor?> {
    val context = LocalContext.current
    return produceState<BitmapDescriptor?>(initialValue = null, photo.thumbnailUri, photo.imageUri, photo.isFavorite, selected) {
        value = withContext(Dispatchers.IO) {
            createMarkerIcon(context, photo.thumbnailUri ?: photo.imageUri, photo.isFavorite, selected)
        }
    }
}

private fun createMarkerIcon(
    context: Context,
    uriString: String,
    isFavorite: Boolean,
    selected: Boolean,
): BitmapDescriptor {
    val size = 132
    val pointerHeight = 24f
    val bitmap = Bitmap.createBitmap(size, size + pointerHeight.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        setShadowLayer(12f, 0f, 5f, 0x55000000)
    }
    val frame = RectF(8f, 6f, size - 8f, size - 10f)
    canvas.drawRoundRect(frame, 34f, 34f, shadowPaint)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) 0xFFFFB84D.toInt() else Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(frame, 34f, 34f, borderPaint)
    val pointer = Path().apply {
        moveTo(size / 2f - 18f, size - 18f)
        lineTo(size / 2f + 18f, size - 18f)
        lineTo(size / 2f, size + 12f)
        close()
    }
    canvas.drawPath(pointer, borderPaint)
    val imageRect = RectF(16f, 14f, size - 16f, size - 18f)
    val image = decode(context, uriString, 128)
    if (image != null) {
        val clip = Path().apply { addRoundRect(imageRect, 28f, 28f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        canvas.drawBitmap(image, crop(image), imageRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        image.recycle()
    } else {
        canvas.drawRoundRect(imageRect, 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE3EEE8.toInt() })
    }
    if (isFavorite) {
        canvas.drawCircle(size - 24f, 25f, 17f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFB84D.toInt() })
        canvas.drawText("★", size - 34f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
    }
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun decode(context: Context, uriString: String, size: Int): Bitmap? = runCatching {
    val uri = Uri.parse(uriString)
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, bounds)
        val sample = generateSequence(1) { it * 2 }
            .first { bounds.outWidth / it <= size * 2 && bounds.outHeight / it <= size * 2 }
        context.contentResolver.openInputStream(uri)?.use { second ->
            BitmapFactory.decodeStream(second, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}.getOrNull()

private fun crop(bitmap: Bitmap): Rect {
    val side = minOf(bitmap.width, bitmap.height)
    val left = (bitmap.width - side) / 2
    val top = (bitmap.height - side) / 2
    return Rect(left, top, left + side, top + side)
}
