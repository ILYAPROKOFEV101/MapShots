package com.ilya.safarshots.feature.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.data.maps.StaticMapsClient
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.GeoPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class CollageRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val staticMapsClient: StaticMapsClient,
) {
    suspend fun render(photos: List<GeoPhoto>, options: CollageOptions): Bitmap = withContext(Dispatchers.IO) {
        val width = options.aspectRatio.width
        val height = options.aspectRatio.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = Palette(options.darkStyle)
        canvas.drawColor(palette.background)
        drawSoftBackground(canvas, width, height, palette)
        when (options.template) {
            CollageTemplate.PHOTO_WITH_MAP -> drawPhotoWithMap(canvas, photos, options, palette, width, height)
            CollageTemplate.POSTCARD -> drawPostcard(canvas, photos, options, palette, width, height)
            CollageTemplate.MAP_STORY -> drawMapStory(canvas, photos, options, palette, width, height)
            CollageTemplate.JOURNEY_TIMELINE -> drawTimeline(canvas, photos, options, palette, width, height)
            CollageTemplate.PHOTO_GRID_WITH_MAP -> drawGridWithMap(canvas, photos, options, palette, width, height)
            CollageTemplate.POLAROID_MAP -> drawPolaroid(canvas, photos, options, palette, width, height)
        }
        bitmap
    }

    private suspend fun drawPhotoWithMap(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val photo = photos.firstOrNull()
        val hero = RectF(0f, 0f, width.toFloat(), height.toFloat())
        drawPhoto(canvas, photo, hero, 0f, palette)

        val captionText = buildList {
            photo?.let {
                add(collageTitle(it))
                if (options.showDate) add(DateTimeFormatters.photoDateTime(it.capturedAtMillis))
                if (options.showCoordinates && it.location != null) {
                    add(DateTimeFormatters.coordinates(it.location.latitude, it.location.longitude))
                }
            }
        }
        val mapInset = width * 0.04f
        val mapSize = min(width, height) * PHOTO_WITH_MAP_SIZE_FRACTION
        val mapSharesBottomWithCaption = options.showMiniMap &&
            photo?.location != null &&
            options.mapCorner == CollageMapCorner.BOTTOM_RIGHT
        if (captionText.isNotEmpty()) {
            val captionInset = width * 0.04f
            val maxCaptionWidth = if (mapSharesBottomWithCaption) {
                (width - mapSize - mapInset - captionInset * 2f)
                    .coerceIn(width * 0.36f, width * 0.6f)
            } else {
                width * 0.6f
            }
            val titleSize = (width * 0.036f).coerceIn(28f, 56f)
            val subSize = titleSize * 0.55f
            val captionHeight = titleSize + 12f + subSize * (captionText.size - 1) + (captionText.size - 1).coerceAtLeast(0) * 8f + captionInset * 2f
            val captionRect = RectF(
                captionInset,
                height - captionInset - captionHeight,
                captionInset + maxCaptionWidth,
                height - captionInset,
            )
            drawRoundRect(canvas, captionRect, 28f, palette.surface, shadow = true)
            var y = captionRect.top + captionInset + titleSize
            drawText(canvas, ellipsize(captionText[0], captionRect.width() - captionInset * 2f, titleSize, bold = true), captionRect.left + captionInset, y, titleSize, palette.onSurface, bold = true)
            captionText.drop(1).forEach { line ->
                y += subSize + 8f
                drawText(canvas, ellipsize(line, captionRect.width() - captionInset * 2f, subSize), captionRect.left + captionInset, y, subSize, palette.onSurfaceVariant)
            }
        }

        if (options.showMiniMap && photo?.location != null) {
            val mapRect = mapRectForCorner(options.mapCorner, width, height, mapSize, mapInset, captionText.isNotEmpty())
            drawMap(canvas, listOf(photo), mapRect, palette, options)
        }
    }

    private fun mapRectForCorner(
        corner: CollageMapCorner,
        width: Int,
        height: Int,
        size: Float,
        inset: Float,
        avoidBottomLeft: Boolean,
    ): RectF {
        val effective = if (avoidBottomLeft && corner == CollageMapCorner.BOTTOM_LEFT) CollageMapCorner.TOP_LEFT else corner
        return when (effective) {
            CollageMapCorner.TOP_LEFT -> RectF(inset, inset, inset + size, inset + size)
            CollageMapCorner.TOP_RIGHT -> RectF(width - inset - size, inset, width - inset, inset + size)
            CollageMapCorner.BOTTOM_LEFT -> RectF(inset, height - inset - size, inset + size, height - inset)
            CollageMapCorner.BOTTOM_RIGHT -> RectF(width - inset - size, height - inset - size, width - inset, height - inset)
        }
    }

    private suspend fun drawPostcard(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val photo = photos.firstOrNull()
        val margin = width * 0.06f
        val hero = RectF(margin, margin, width - margin, height * 0.66f)
        drawPhoto(canvas, photo, hero, 46f, palette)

        val captionGap = margin * 0.5f
        val caption = RectF(margin, hero.bottom + captionGap, width - margin, height - margin)
        drawRoundRect(canvas, caption, 36f, palette.surface, shadow = true)

        val captionPadding = caption.width() * 0.05f
        val mapSize = if (options.showMiniMap) (caption.height() - captionPadding * 2f).coerceAtMost(caption.width() * 0.32f) else 0f
        val mapRect = if (options.showMiniMap) {
            RectF(
                caption.right - captionPadding - mapSize,
                caption.top + captionPadding,
                caption.right - captionPadding,
                caption.top + captionPadding + mapSize,
            )
        } else null
        val textRight = (mapRect?.left ?: caption.right) - captionPadding
        val textLeft = caption.left + captionPadding
        val textWidth = textRight - textLeft

        val titleText = photo?.let { collageTitle(it) }
            ?: "GeoMapShots"
        val titleSize = (width * 0.034f).coerceIn(34f, 56f)
        val titleBottomY = caption.top + captionPadding + titleSize + 8f
        drawText(canvas, ellipsize(titleText, textWidth, titleSize, bold = true), textLeft, titleBottomY, titleSize, palette.onSurface, bold = true)

        var cursorY = titleBottomY + titleSize * 0.5f
        if (photo != null && options.showDate) {
            val size = titleSize * 0.55f
            cursorY += size + 6f
            drawText(canvas, DateTimeFormatters.photoDateTime(photo.capturedAtMillis), textLeft, cursorY, size, palette.onSurfaceVariant)
        }
        if (photo?.location != null && options.showCoordinates) {
            val size = titleSize * 0.5f
            cursorY += size + 6f
            drawText(
                canvas,
                DateTimeFormatters.coordinates(photo.location.latitude, photo.location.longitude),
                textLeft,
                cursorY,
                size,
                palette.onSurfaceVariant,
            )
        }
        if (mapRect != null && photo?.location != null) {
            drawMap(canvas, listOf(photo), mapRect, palette, options)
        }
    }

    private fun ellipsize(text: String, maxWidth: Float, size: Float, bold: Boolean = false): String {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && paint.measureText("$trimmed…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }

    private suspend fun drawMapStory(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val margin = width * 0.055f
        val titleSize = (width * 0.04f).coerceIn(38f, 60f)
        val title = storyTitle(photos)
        drawText(canvas, ellipsize(title, width - margin * 2f, titleSize, bold = true), margin, margin + titleSize, titleSize, palette.onBackground, bold = true)
        val subSize = titleSize * 0.55f
        drawText(canvas, storySubtitle(photos, options), margin, margin + titleSize + subSize + 12f, subSize, palette.onSurfaceVariant)

        if (photos.size <= 1) {
            val photo = photos.firstOrNull()
            val card = RectF(margin, height * 0.20f, width - margin, height * 0.58f)
            drawPhoto(canvas, photo, card, 36f, palette)
            val mapRect = RectF(margin, height * 0.62f, width - margin, height - margin)
            drawMap(canvas, photos, mapRect, palette, options)
            return
        }

        val map = RectF(margin, height * 0.24f, width - margin, height * 0.72f)
        drawMap(canvas, photos, map, palette, options)
        val cardSize = min(width, height) * 0.22f
        photos.take(8).forEachIndexed { index, photo ->
            val rowTop = if (index % 2 == 0) margin + titleSize + subSize + 30f else height - margin - cardSize
            val x = margin + (index / 2) * (cardSize * 0.96f)
            val rect = RectF(x, rowTop, x + cardSize, rowTop + cardSize)
            drawPhoto(canvas, photo, rect, 30f, palette)
            photo.location?.let {
                val points = MapBoundsProjector.project(photos.mapNotNull { it.location })
                val point = points.getOrNull(photos.mapNotNull { it.location }.indexOf(it))
                if (point != null) {
                    drawLine(canvas, rect.centerX(), rect.centerY(), map.left + map.width() * point.x, map.top + map.height() * point.y, palette.tertiary)
                }
            }
        }
    }

    private fun storyTitle(photos: List<GeoPhoto>): String {
        val address = photos.firstOrNull { !it.location?.addressText.isNullOrBlank() }?.location?.addressText
        if (!address.isNullOrBlank()) return address
        val first = photos.firstOrNull() ?: return "GeoMapShots"
        return collageTitle(first)
    }

    private fun collageTitle(photo: GeoPhoto): String {
        photo.location?.addressText?.takeIf { it.isNotBlank() }?.let { return it }
        photo.location?.let { return shortCoordinates(it.latitude, it.longitude) }
        return DateTimeFormatters.galleryGroup(photo.capturedAtMillis)
    }

    private suspend fun drawTimeline(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val margin = width * 0.06f
        drawText(canvas, "Journey timeline", margin, margin + 44f, 48f, palette.onBackground, bold = true)
        val mapHeight = height * 0.25f
        val map = RectF(margin, height - margin - mapHeight, width - margin, height - margin)
        drawMap(canvas, photos, map, palette, options)
        val availableTop = margin + 92f
        val availableBottom = map.top - margin
        val itemHeight = ((availableBottom - availableTop) / photos.take(5).size.coerceAtLeast(1)).coerceAtMost(250f)
        val lineX = margin + 35f
        drawLine(canvas, lineX, availableTop, lineX, availableBottom, palette.primary, stroke = 5f)
        photos.take(5).forEachIndexed { index, photo ->
            val top = availableTop + itemHeight * index
            drawPinDot(canvas, lineX, top + 60f, palette.primary)
            val imageRect = RectF(lineX + 35f, top, lineX + 35f + itemHeight * 0.85f, top + itemHeight * 0.78f)
            drawPhoto(canvas, photo, imageRect, 24f, palette)
            drawText(canvas, DateTimeFormatters.photoDateTime(photo.capturedAtMillis), imageRect.right + 30f, top + 50f, 28f, palette.onSurface, bold = true)
            if (options.showCoordinates) {
                drawText(canvas, photo.displaySubtitle, imageRect.right + 30f, top + 94f, 23f, palette.onSurfaceVariant)
            }
        }
    }

    private suspend fun drawGridWithMap(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val margin = width * 0.045f
        val map = RectF(margin, margin, width - margin, height * 0.34f)
        if (options.showMiniMap) drawMap(canvas, photos, map, palette, options)
        val gridTop = map.bottom + margin
        val cols = if (width > height) 4 else 3
        val gap = 16f
        val cell = ((width - margin * 2f - gap * (cols - 1)) / cols)
        photos.take(12).forEachIndexed { index, photo ->
            val row = index / cols
            val col = index % cols
            val left = margin + col * (cell + gap)
            val top = gridTop + row * (cell + gap)
            if (top + cell <= height - margin) {
                drawPhoto(canvas, photo, RectF(left, top, left + cell, top + cell), 24f, palette)
            }
        }
        drawText(canvas, "${photos.size} photo${if (photos.size == 1) "" else "s"} on the map", margin + 30f, map.bottom - 34f, 34f, Color.WHITE, bold = true)
    }

    private suspend fun drawPolaroid(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val margin = width * 0.06f
        drawText(canvas, "GeoMapShots", margin, margin + 42f, 46f, palette.onBackground, bold = true)
        val map = RectF(width * 0.52f, margin, width - margin, height * 0.34f)
        if (options.showMiniMap) drawMap(canvas, photos, map, palette, options)
        val cols = if (width > height) 4 else 2
        val cardWidth = (width - margin * 2f - 26f * (cols - 1)) / cols
        val cardHeight = cardWidth * 1.18f
        photos.take(8).forEachIndexed { index, photo ->
            val col = index % cols
            val row = index / cols
            val left = margin + col * (cardWidth + 26f)
            val top = height * 0.18f + row * (cardHeight + 24f)
            val card = RectF(left, top, left + cardWidth, top + cardHeight)
            if (card.bottom < height - margin) {
                drawRoundRect(canvas, card, 20f, palette.surface, shadow = true)
                drawPhoto(canvas, photo, RectF(card.left + 18f, card.top + 18f, card.right - 18f, card.bottom - 78f), 14f, palette)
                val label = if (options.showDate) DateTimeFormatters.timeOnly(photo.capturedAtMillis) else photo.displaySubtitle
                drawText(canvas, label, card.left + 24f, card.bottom - 34f, 24f, palette.onSurface, bold = true)
            }
        }
    }

    private suspend fun drawMap(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        rect: RectF,
        palette: Palette,
        options: CollageOptions,
    ) {
        val locations = photos.mapNotNull { it.location }
        if (locations.isEmpty()) {
            drawSchematicMap(canvas, photos, rect, palette)
            return
        }
        val widthPx = rect.width().toInt().coerceAtLeast(1)
        val heightPx = rect.height().toInt().coerceAtLeast(1)
        val isSingle = photos.size == 1
        val photo = photos.firstOrNull()
        val cachedSnapshotUri = photo?.mapSnapshotUri?.takeIf { isSingle }

        // Fast path: the on-disk snapshot is rendered at the same baseline settings the cache
        // is captured with (dark=false, tourist style on, zoom=14, no marker). When the user
        // keeps those defaults we can skip the network round-trip entirely and show the real
        // map immediately, avoiding the schematic placeholder on first preview.
        val cachedMatchesOptions = cachedSnapshotUri != null &&
            !options.darkStyle &&
            options.mapZoom == DEFAULT_CACHED_MAP_ZOOM &&
            options.useTouristMapStyle

        var ownedCachedBitmap: Bitmap? = null
        var realMap: Bitmap? = null

        if (cachedMatchesOptions) {
            ownedCachedBitmap = decodeBitmap(cachedSnapshotUri!!, widthPx, heightPx)
            realMap = ownedCachedBitmap
        }

        if (realMap == null) {
            val fetchedMap = staticMapsClient.fetch(
                locations = locations,
                targetWidth = widthPx,
                targetHeight = heightPx,
                dark = options.darkStyle,
                includeMarker = !isSingle,
                zoom = options.mapZoom,
                useTouristMapStyle = options.useTouristMapStyle,
            )
            if (fetchedMap != null) {
                realMap = fetchedMap
            } else if (cachedSnapshotUri != null) {
                // Network fetch failed: still prefer the on-disk snapshot over the schematic
                // placeholder even if it doesn't match the requested options perfectly.
                ownedCachedBitmap = decodeBitmap(cachedSnapshotUri, widthPx, heightPx)
                realMap = ownedCachedBitmap
            }
        }

        if (realMap == null) {
            drawSchematicMap(canvas, photos, rect, palette)
            return
        }
        val corner = (min(rect.width(), rect.height()) * 0.08f).coerceIn(20f, 40f)
        drawRoundRect(canvas, rect, corner, palette.mapBase, shadow = true)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) })
        val source = cropSource(realMap, rect)
        canvas.drawBitmap(realMap, source, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(rect, Paint().apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                0x00000000,
                if (options.darkStyle) 0x33000000 else 0x14000000,
                Shader.TileMode.CLAMP,
            )
        })
        canvas.restore()
        if (isSingle) {
            drawPulsePin(canvas, rect.centerX(), rect.centerY(), rect, palette)
        }
        drawCompass(canvas, rect, palette)
        ownedCachedBitmap?.recycle()
    }

    private fun drawPhotoMarker(
        canvas: Canvas,
        photo: GeoPhoto,
        cx: Float,
        cy: Float,
        mapRect: RectF,
    ) {
        val markerSize = (min(mapRect.width(), mapRect.height()) * 0.28f).coerceIn(96f, 220f)
        val cornerRadius = markerSize * 0.22f
        val borderWidth = markerSize * 0.07f
        val markerRect = RectF(
            cx - markerSize / 2f,
            cy - markerSize / 2f,
            cx + markerSize / 2f,
            cy + markerSize / 2f,
        )
        val borderRect = RectF(
            markerRect.left - borderWidth,
            markerRect.top - borderWidth,
            markerRect.right + borderWidth,
            markerRect.bottom + borderWidth,
        )
        canvas.drawRoundRect(
            borderRect,
            cornerRadius + borderWidth,
            cornerRadius + borderWidth,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(markerSize * 0.18f, 0f, markerSize * 0.06f, 0x55000000)
            },
        )
        val thumb = decodeBitmap(
            uriString = photo.thumbnailUri ?: photo.imageUri,
            reqWidth = markerSize.toInt(),
            reqHeight = markerSize.toInt(),
        )
        if (thumb == null) {
            canvas.drawRoundRect(
                markerRect,
                cornerRadius,
                cornerRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE0E5E2.toInt() },
            )
            return
        }
        val clipPath = Path().apply { addRoundRect(markerRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clipPath)
        val src = cropSource(thumb, markerRect)
        canvas.drawBitmap(thumb, src, markerRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        thumb.recycle()
    }

    private fun drawSchematicMap(canvas: Canvas, photos: List<GeoPhoto>, rect: RectF, palette: Palette) {
        val corner = (min(rect.width(), rect.height()) * 0.09f).coerceIn(24f, 42f)
        drawRoundRect(canvas, rect, corner, palette.mapBase, shadow = true)
        canvas.save()
        val clipPath = Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) }
        canvas.clipPath(clipPath)

        canvas.drawRect(rect, Paint().apply {
            shader = RadialGradient(
                rect.centerX(), rect.centerY(),
                min(rect.width(), rect.height()) * 0.85f,
                palette.mapHighlight,
                palette.mapBase,
                Shader.TileMode.CLAMP,
            )
        })

        drawSchematicBlocks(canvas, rect, palette)
        drawSchematicRiver(canvas, rect, palette)

        val minor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = palette.mapLine
        }
        val major = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = palette.mapLineStrong
        }
        val gridStep = (rect.width() / 6f).coerceAtLeast(40f)
        var x = rect.left + gridStep
        var i = 1
        while (x < rect.right) {
            canvas.drawLine(x, rect.top, x, rect.bottom, if (i % 3 == 0) major else minor)
            x += gridStep
            i++
        }
        var y = rect.top + gridStep
        var j = 1
        while (y < rect.bottom) {
            canvas.drawLine(rect.left, y, rect.right, y, if (j % 3 == 0) major else minor)
            y += gridStep
            j++
        }

        val locations = photos.mapNotNull { it.location }
        val points = MapBoundsProjector.project(locations)
        if (points.size > 1) {
            val routeGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primary
                alpha = 60
                strokeWidth = 18f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primary
                strokeWidth = 9f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = Path()
            points.forEachIndexed { index, point ->
                val px = rect.left + rect.width() * point.x
                val py = rect.top + rect.height() * point.y
                if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, routeGlow)
            canvas.drawPath(path, routePaint)
        }
        if (points.size == 1) {
            val point = points.first()
            val px = rect.left + rect.width() * point.x
            val py = rect.top + rect.height() * point.y
            drawPulsePin(canvas, px, py, rect, palette)
        } else {
            points.forEachIndexed { index, point ->
                val px = rect.left + rect.width() * point.x
                val py = rect.top + rect.height() * point.y
                val color = if (index == points.lastIndex) palette.tertiary else palette.primary
                drawPinHalo(canvas, px, py, color)
                drawPinDot(canvas, px, py, color)
            }
        }
        canvas.restore()
        drawCompass(canvas, rect, palette)
        val chipText = photos.firstOrNull()?.location?.let { it.addressText ?: shortCoordinates(it.latitude, it.longitude) }
        if (chipText != null && photos.size == 1 && rect.width() >= 320f) {
            drawLocationChip(canvas, rect, chipText, palette)
        }
    }

    private fun drawSchematicBlocks(canvas: Canvas, rect: RectF, palette: Palette) {
        val seed = (rect.width().toLong() * 73856093L) xor (rect.height().toLong() * 19349663L) xor rect.left.toLong()
        val rnd = java.util.Random(seed)
        val cols = 5
        val rows = 5
        val cellW = rect.width() / cols
        val cellH = rect.height() / rows
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mapBlock
            style = Paint.Style.FILL
        }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (rnd.nextFloat() > 0.55f) continue
                val padX = cellW * (0.12f + rnd.nextFloat() * 0.18f)
                val padY = cellH * (0.12f + rnd.nextFloat() * 0.18f)
                val left = rect.left + col * cellW + padX
                val top = rect.top + row * cellH + padY
                val w = cellW * (0.45f + rnd.nextFloat() * 0.35f)
                val h = cellH * (0.45f + rnd.nextFloat() * 0.35f)
                canvas.drawRoundRect(RectF(left, top, left + w, top + h), 6f, 6f, paint)
            }
        }
    }

    private fun drawSchematicRiver(canvas: Canvas, rect: RectF, palette: Palette) {
        val strokeBase = (min(rect.width(), rect.height()) * 0.07f).coerceAtLeast(10f)
        val river = Path().apply {
            moveTo(rect.left, rect.top + rect.height() * 0.28f)
            cubicTo(
                rect.left + rect.width() * 0.30f, rect.top + rect.height() * 0.05f,
                rect.left + rect.width() * 0.55f, rect.top + rect.height() * 0.88f,
                rect.right, rect.top + rect.height() * 0.72f,
            )
        }
        canvas.drawPath(river, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mapRiver
            style = Paint.Style.STROKE
            strokeWidth = strokeBase
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
        canvas.drawPath(river, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mapRiverHighlight
            style = Paint.Style.STROKE
            strokeWidth = strokeBase * 0.35f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    private fun drawPulsePin(canvas: Canvas, cx: Float, cy: Float, rect: RectF, palette: Palette) {
        val base = (min(rect.width(), rect.height()) * 0.055f).coerceIn(14f, 38f)
        canvas.drawCircle(cx, cy, base * 2.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.tertiary
            alpha = 40
        })
        canvas.drawCircle(cx, cy, base * 1.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.tertiary
            alpha = 80
        })
        canvas.drawCircle(cx, cy, base * 1.05f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(base * 0.7f, 0f, base * 0.18f, 0x55000000)
        })
        canvas.drawCircle(cx, cy, base * 0.85f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        })
        canvas.drawCircle(cx, cy, base * 0.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.tertiary
        })
        canvas.drawCircle(cx, cy, base * 0.22f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        })
    }

    private fun drawLocationChip(canvas: Canvas, rect: RectF, text: String, palette: Palette) {
        val maxWidth = rect.width() * 0.84f
        val textSize = (min(rect.width(), rect.height()) * 0.062f).coerceIn(20f, 34f)
        val measure = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val padH = textSize * 1.0f
        val padV = textSize * 0.55f
        val iconSize = textSize * 0.95f
        val gap = textSize * 0.55f
        val maxTextWidth = maxWidth - padH * 2f - iconSize - gap
        val displayText = ellipsize(text, maxTextWidth, textSize, bold = true)
        val textWidth = measure.measureText(displayText)
        val chipWidth = (textWidth + padH * 2f + iconSize + gap).coerceAtMost(maxWidth)
        val chipHeight = textSize + padV * 2f
        val cx = rect.centerX()
        val chipBottom = rect.bottom - rect.height() * 0.05f
        val chipRect = RectF(
            cx - chipWidth / 2f,
            chipBottom - chipHeight,
            cx + chipWidth / 2f,
            chipBottom,
        )
        val radius = chipHeight / 2f
        canvas.drawRoundRect(chipRect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surface
            setShadowLayer(16f, 0f, 6f, 0x33000000)
        })
        val pinX = chipRect.left + radius + textSize * 0.05f
        val pinY = chipRect.centerY()
        canvas.drawCircle(pinX, pinY, iconSize * 0.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.tertiary
        })
        canvas.drawCircle(pinX, pinY, iconSize * 0.22f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        })
        val textX = pinX + iconSize * 0.5f + gap
        val textY = chipRect.centerY() + textSize * 0.36f
        drawText(canvas, displayText, textX, textY, textSize, palette.onSurface, bold = true)
    }

    private fun shortCoordinates(latitude: Double, longitude: Double): String =
        "%.4f, %.4f".format(java.util.Locale.US, latitude, longitude)

    private fun drawPinHalo(canvas: Canvas, x: Float, y: Float, color: Int) {
        canvas.drawCircle(x, y, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 50
        })
    }

    private fun drawCompass(canvas: Canvas, rect: RectF, palette: Palette) {
        val cx = rect.right - 30f
        val cy = rect.top + 30f
        canvas.drawCircle(cx, cy, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surface
            setShadowLayer(8f, 0f, 2f, 0x22000000)
        })
        canvas.drawCircle(cx, cy, 17f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surface
        })
        val needle = Path().apply {
            moveTo(cx, cy - 11f)
            lineTo(cx + 5f, cy + 1f)
            lineTo(cx - 5f, cy + 1f)
            close()
        }
        canvas.drawPath(needle, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.tertiary })
        val south = Path().apply {
            moveTo(cx, cy + 11f)
            lineTo(cx + 5f, cy - 1f)
            lineTo(cx - 5f, cy - 1f)
            close()
        }
        canvas.drawPath(south, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurfaceVariant
            alpha = 140
        })
    }

    private fun drawPhoto(canvas: Canvas, photo: GeoPhoto?, rect: RectF, radius: Float, palette: Palette) {
        val bitmap = photo?.let { decodeBitmap(it.thumbnailUri ?: it.imageUri, rect.width().toInt(), rect.height().toInt()) }
        if (bitmap == null) {
            drawRoundRect(canvas, rect, radius, palette.placeholder, shadow = true)
            drawText(canvas, "GeoMapShots", rect.left + 28f, rect.centerY(), 28f, palette.onSurfaceVariant, bold = true)
            return
        }
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        val src = cropSource(bitmap, rect)
        canvas.drawBitmap(bitmap, src, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(rect, Paint().apply {
            shader = LinearGradient(0f, rect.top, 0f, rect.bottom, Color.TRANSPARENT, 0x66000000, Shader.TileMode.CLAMP)
        })
        canvas.restore()
        bitmap.recycle()
    }

    private fun decodeBitmap(uriString: String, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, bounds)
            val sample = calculateSample(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
            context.contentResolver.openInputStream(uri)?.use { second ->
                BitmapFactory.decodeStream(second, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    }.getOrNull()

    private fun cropSource(bitmap: Bitmap, dst: RectF): Rect {
        val dstRatio = dst.width() / dst.height()
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (sourceRatio > dstRatio) {
            val width = (bitmap.height * dstRatio).toInt()
            val left = (bitmap.width - width) / 2
            Rect(left, 0, left + width, bitmap.height)
        } else {
            val height = (bitmap.width / dstRatio).toInt()
            val top = (bitmap.height - height) / 2
            Rect(0, top, bitmap.width, top + height)
        }
    }

    private fun calculateSample(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var sample = 1
        if (width <= 0 || height <= 0) return sample
        while (width / sample > reqWidth * 2 || height / sample > reqHeight * 2) {
            sample *= 2
        }
        return sample
    }

    private fun drawSoftBackground(canvas: Canvas, width: Int, height: Int, palette: Palette) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), palette.background, palette.backgroundAccent, Shader.TileMode.CLAMP)
        })
    }

    private fun drawRoundRect(canvas: Canvas, rect: RectF, radius: Float, color: Int, shadow: Boolean = false) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            if (shadow) setShadowLayer(18f, 0f, 8f, 0x33000000)
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        canvas.drawText(text.take(54), x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        })
    }

    private fun drawLine(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, color: Int, stroke: Float = 3f) {
        canvas.drawLine(startX, startY, endX, endY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            alpha = 170
        })
    }

    private fun drawPinDot(canvas: Canvas, x: Float, y: Float, color: Int) {
        canvas.drawCircle(x, y, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE })
        canvas.drawCircle(x, y, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun storySubtitle(photos: List<GeoPhoto>, options: CollageOptions): String {
        if (photos.isEmpty()) return "Choose photos to tell a place story"
        if (photos.size == 1) {
            val photo = photos.first()
            return if (options.showDate) {
                DateTimeFormatters.photoDateTime(photo.capturedAtMillis)
            } else {
                "1 mapped memory"
            }
        }
        val minMillis = photos.minOf { it.capturedAtMillis }
        val maxMillis = photos.maxOf { it.capturedAtMillis }
        return if (options.showDate) {
            "${DateTimeFormatters.photoDateTime(minMillis)} — ${DateTimeFormatters.photoDateTime(maxMillis)}"
        } else {
            "${photos.size} mapped memories"
        }
    }

    private data class Palette(val dark: Boolean) {
        val background = if (dark) 0xFF121416.toInt() else 0xFFF7FAF8.toInt()
        val backgroundAccent = if (dark) 0xFF1E2A2C.toInt() else 0xFFEAF4EF.toInt()
        val surface = if (dark) 0xFF202426.toInt() else Color.WHITE
        val onBackground = if (dark) 0xFFE8F0EC.toInt() else 0xFF15201C.toInt()
        val onSurface = if (dark) 0xFFE8F0EC.toInt() else 0xFF17201D.toInt()
        val onSurfaceVariant = if (dark) 0xFFB8C8C1.toInt() else 0xFF50605A.toInt()
        val primary = if (dark) 0xFF8CCFFF.toInt() else 0xFF086A8F.toInt()
        val tertiary = if (dark) 0xFFFFC182.toInt() else 0xFFB45F06.toInt()
        val mapBase = if (dark) 0xFF273132.toInt() else 0xFFE3EEE8.toInt()
        val mapHighlight = if (dark) 0xFF334042.toInt() else 0xFFEFF7F1.toInt()
        val mapLine = if (dark) 0x224DD0B7 else 0x33297068
        val mapLineStrong = if (dark) 0x554DD0B7 else 0x66297068
        val mapLabel = if (dark) 0xFF9AB1AA.toInt() else 0xFF61756D.toInt()
        val mapBlock = if (dark) 0x33C3DDD4.toInt() else 0x33ADD0C2.toInt()
        val mapRiver = if (dark) 0x66639BC6.toInt() else 0x6699D0EC.toInt()
        val mapRiverHighlight = if (dark) 0x99B9DFFF.toInt() else 0xCCEAF6FF.toInt()
        val placeholder = if (dark) 0xFF293034.toInt() else 0xFFDCE7E3.toInt()
    }

    private companion object {
        const val PHOTO_WITH_MAP_SIZE_FRACTION = 0.4f
        // Keep in sync with StaticMapsClient.DEFAULT_SINGLE_LOCATION_ZOOM / CollageOptions.mapZoom
        // defaults. When the requested options match these, the on-disk snapshot is reused as-is.
        const val DEFAULT_CACHED_MAP_ZOOM = 14
    }
}
