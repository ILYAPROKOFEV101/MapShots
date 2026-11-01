package com.ilya.safarshots.feature.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ilya.safarshots.R
import com.ilya.safarshots.core.common.DateTimeFormatters
import com.ilya.safarshots.data.maps.StaticMapsClient
import com.ilya.safarshots.domain.model.CollageBackgroundMode
import com.ilya.safarshots.domain.model.CollageCard
import com.ilya.safarshots.domain.model.CollageMapCorner
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.CollageTemplate
import com.ilya.safarshots.domain.model.CollageTextLayer
import com.ilya.safarshots.domain.model.CollageTextShape
import com.ilya.safarshots.domain.model.GeoPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class CollageRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val staticMapsClient: StaticMapsClient,
) {
    suspend fun render(photos: List<GeoPhoto>, options: CollageOptions): Bitmap =
        renderAt(
            photos = photos,
            options = options,
            width = options.aspectRatio.width,
            height = options.aspectRatio.height,
            photoSource = PhotoSource.PREVIEW,
        )

    suspend fun renderForExport(photos: List<GeoPhoto>, options: CollageOptions): Bitmap {
        val size = calculateHighQualityExportSize(photos, options)
        return renderAt(
            photos = photos,
            options = options,
            width = size.width,
            height = size.height,
            photoSource = PhotoSource.ORIGINAL,
        )
    }

    private suspend fun renderAt(
        photos: List<GeoPhoto>,
        options: CollageOptions,
        width: Int,
        height: Int,
        photoSource: PhotoSource,
    ): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = Palette(options.darkStyle)
        if (options.backgroundMode == CollageBackgroundMode.SOLID_COLOR) {
            canvas.drawColor(options.backgroundColor)
        } else {
            canvas.drawColor(palette.background)
            drawSoftBackground(canvas, width, height, palette)
        }
        when (options.template) {
            CollageTemplate.PHOTO_WITH_MAP -> drawPhotoWithMap(canvas, photos, options, palette, width, height, photoSource)
            CollageTemplate.SPLIT_VERTICAL -> drawSplit(
                canvas, photos, options, palette, width, height,
                vertical = true,
                photoSource = photoSource,
            )
            CollageTemplate.SPLIT_HORIZONTAL -> drawSplit(
                canvas, photos, options, palette, width, height,
                vertical = false,
                photoSource = photoSource,
            )
            CollageTemplate.GRID -> drawGrid(canvas, photos, options, palette, width, height, photoSource)
            CollageTemplate.MAGAZINE -> drawMagazine(canvas, photos, options, palette, width, height, photoSource)
            CollageTemplate.POSTCARD -> drawPostcard(canvas, photos, options, palette, width, height, photoSource)
        }
        if (options.renderTextLayers) {
            drawTextLayers(canvas, options.textLayers, width, height)
        }
        bitmap
    }

    private fun drawTextLayers(
        canvas: Canvas,
        layers: List<CollageTextLayer>,
        canvasWidth: Int,
        canvasHeight: Int,
    ) {
        val shorter = min(canvasWidth, canvasHeight).toFloat()
        layers.forEach { layer ->
            if (layer.text.isBlank()) return@forEach
            val boxWidth = (layer.widthFrac * canvasWidth).coerceIn(96f, canvasWidth.toFloat())
            val padding = (shorter * 0.018f).coerceIn(12f, 30f)
            val textWidth = (boxWidth - padding * 2f).coerceAtLeast(1f)
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.textColor
                textSize = (layer.textSizeFrac * shorter).coerceIn(18f, 120f)
                typeface = android.graphics.Typeface.DEFAULT
            }
            val textLayout = StaticLayout.Builder
                .obtain(layer.text, 0, layer.text.length, textPaint, textWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .build()
            val boxHeight = textLayout.height + padding * 2f
            val left = (layer.originX * canvasWidth).coerceIn(0f, (canvasWidth - boxWidth).coerceAtLeast(0f))
            val top = (layer.originY * canvasHeight).coerceIn(0f, (canvasHeight - boxHeight).coerceAtLeast(0f))
            val rect = RectF(left, top, left + boxWidth, top + boxHeight)
            val radius = when (layer.shape) {
                CollageTextShape.PILL -> boxHeight / 2f
                CollageTextShape.ROUNDED -> layer.cornerRadiusFrac * shorter
                CollageTextShape.NONE, CollageTextShape.RECTANGLE -> 0f
            }

            canvas.save()
            if (layer.rotationDeg != 0f) {
                canvas.rotate(layer.rotationDeg, rect.centerX(), rect.centerY())
            }
            if (layer.shape != CollageTextShape.NONE && layer.backgroundOpacity > 0f) {
                canvas.drawRoundRect(
                    rect,
                    radius,
                    radius,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.backgroundColor.withAlpha(layer.backgroundOpacity)
                        style = Paint.Style.FILL
                    },
                )
            }
            if (layer.borderWidthFrac > 0f) {
                val borderWidth = (layer.borderWidthFrac * shorter).coerceAtLeast(1f)
                val borderRect = RectF(
                    rect.left + borderWidth / 2f,
                    rect.top + borderWidth / 2f,
                    rect.right - borderWidth / 2f,
                    rect.bottom - borderWidth / 2f,
                )
                canvas.drawRoundRect(
                    borderRect,
                    radius,
                    radius,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.borderColor
                        style = Paint.Style.STROKE
                        strokeWidth = borderWidth
                    },
                )
            }

            canvas.save()
            canvas.translate(left + padding, top + padding)
            textLayout.draw(canvas)
            canvas.restore()
            canvas.restore()
        }
    }

    private fun Int.withAlpha(alpha: Float): Int =
        (this and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).roundToInt() shl 24)

    private suspend fun drawPhotoWithMap(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
        photoSource: PhotoSource,
    ) {
        val photosById = photos.associateBy { it.id }
        val heroPhoto = options.backgroundPhotoId?.let(photosById::get) ?: photos.firstOrNull()
        val mapPhoto = mapPhoto(photos, options)
        val hero = RectF(0f, 0f, width.toFloat(), height.toFloat())
        when (options.backgroundMode) {
            CollageBackgroundMode.HERO_PHOTO -> drawPhoto(canvas, heroPhoto, hero, 0f, palette, photoSource)
            CollageBackgroundMode.SOLID_COLOR -> drawSolidBackground(canvas, hero, options.backgroundColor)
        }

        val captionPhoto = heroPhoto ?: photos.firstOrNull()
        val captionText = buildCaptionLines(
            captionPhoto,
            options.showDate,
            options.showCoordinates,
            options.customCaption,
            options.showCaption,
        )
        val defaultMapInset = width * 0.04f
        val defaultMapSize = min(width, height) * PHOTO_WITH_MAP_SIZE_FRACTION
        val mapSharesBottomWithCaption = options.showMiniMap &&
            mapPhoto?.location != null &&
            options.mapPlacement == null &&
            options.captionPlacement == null &&
            options.mapCorner == CollageMapCorner.BOTTOM_RIGHT
        if (captionText.isNotEmpty()) {
            val defaultWidthFrac = if (mapSharesBottomWithCaption) {
                ((width - defaultMapSize - defaultMapInset * 3f) / width)
                    .coerceIn(0.36f, 0.6f)
            } else {
                0.6f
            }
            drawFloatingCaption(canvas, captionText, options, palette, width, height, defaultWidthFrac)
        }

        options.cards.forEach { card ->
            val cardPhoto = photosById[card.photoId] ?: return@forEach
            drawCard(canvas, cardPhoto, card, width, height, palette, photoSource)
        }

        if (options.showMiniMap && options.renderMiniMap && mapPhoto?.location != null) {
            val mapRect = options.mapPlacement?.let { placement ->
                val shorter = min(width, height).toFloat()
                val w = (placement.widthFrac * shorter).coerceAtLeast(64f)
                val h = w
                val cx = placement.centerX * width
                val cy = placement.centerY * height
                RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
            } ?: mapRectForCorner(options.mapCorner, width, height, defaultMapSize, defaultMapInset, captionText.isNotEmpty())
            canvas.save()
            val mapRotation = options.mapPlacement?.rotationDeg ?: 0f
            if (mapRotation != 0f) canvas.rotate(mapRotation, mapRect.centerX(), mapRect.centerY())
            drawMap(canvas, listOf(mapPhoto), mapRect, palette, options)
            canvas.restore()
        }
    }

    private fun drawSolidBackground(canvas: Canvas, rect: RectF, color: Int) {
        canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    /**
     * Returns caption rows for the PHOTO_WITH_MAP template, ensuring the same value isn't
     * shown twice (e.g. coordinates falling back into the title slot while [showCoordinates]
     * also asks for an explicit coordinates row).
     */
    internal fun buildCaptionLines(
        photo: GeoPhoto?,
        showDate: Boolean,
        showCoordinates: Boolean,
        customCaption: String = "",
        showCaption: Boolean = true,
    ): List<String> {
        if (!showCaption || photo == null) return emptyList()
        val address = photo.location?.addressText?.takeIf { it.isNotBlank() }
        val note = photo.note?.takeIf { it.isNotBlank() }
        val coordsLine = photo.location
            ?.takeIf { showCoordinates }
            ?.let { DateTimeFormatters.coordinates(it.latitude, it.longitude) }
        val dateLine = if (showDate) DateTimeFormatters.photoDateTime(photo.capturedAtMillis) else null

        // Pick a title that won't be repeated below. Address > user note > coordinates (only
        // if we're not already going to render them as a separate row) > nothing.
        val customLines = customCaption
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .take(MAX_CAPTION_LINES)
            .toList()
        val automaticTitle = address
            ?: note
            ?: photo.location
                ?.takeIf { coordsLine == null }
                ?.let { shortCoordinates(it.latitude, it.longitude) }

        return buildList {
            if (customLines.isNotEmpty()) addAll(customLines) else if (automaticTitle != null) add(automaticTitle)
            if (dateLine != null) add(dateLine)
            if (coordsLine != null && coordsLine != automaticTitle && coordsLine !in customLines) add(coordsLine)
        }.take(MAX_CAPTION_LINES)
    }

    private fun orderedPhotos(photos: List<GeoPhoto>, options: CollageOptions): List<GeoPhoto> {
        val hero = options.backgroundPhotoId?.let { id -> photos.firstOrNull { it.id == id } }
            ?: return photos
        return listOf(hero) + photos.filterNot { it.id == hero.id }
    }

    private fun mapPhoto(photos: List<GeoPhoto>, options: CollageOptions): GeoPhoto? {
        return options.mapPhotoId
            ?.let { id -> photos.firstOrNull { it.id == id && it.location != null } }
            ?: options.backgroundPhotoId
                ?.let { id -> photos.firstOrNull { it.id == id && it.location != null } }
            ?: photos.firstOrNull { it.location != null }
    }

    private suspend fun drawSplit(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
        vertical: Boolean,
        photoSource: PhotoSource,
    ) {
        val ordered = orderedPhotos(photos, options)
        val gap = min(width, height) * 0.014f
        val radius = min(width, height) * 0.022f
        if (vertical) {
            val middle = width / 2f
            drawPhoto(canvas, ordered.getOrNull(0), RectF(0f, 0f, middle - gap / 2f, height.toFloat()), radius, palette, photoSource)
            drawPhoto(canvas, ordered.getOrNull(1), RectF(middle + gap / 2f, 0f, width.toFloat(), height.toFloat()), radius, palette, photoSource)
        } else {
            val middle = height / 2f
            drawPhoto(canvas, ordered.getOrNull(0), RectF(0f, 0f, width.toFloat(), middle - gap / 2f), radius, palette, photoSource)
            drawPhoto(canvas, ordered.getOrNull(1), RectF(0f, middle + gap / 2f, width.toFloat(), height.toFloat()), radius, palette, photoSource)
        }
        drawCaptionForPhotoLayout(canvas, ordered.firstOrNull(), options, palette, width, height)
        drawMapForPhotoLayout(canvas, photos, options, palette, width, height)
    }

    private suspend fun drawGrid(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
        photoSource: PhotoSource,
    ) {
        val ordered = orderedPhotos(photos, options).take(MAX_GRID_PHOTOS)
        drawPhotoGridInRect(
            canvas = canvas,
            photos = ordered,
            rect = RectF(0f, 0f, width.toFloat(), height.toFloat()),
            palette = palette,
            gap = min(width, height) * 0.012f,
            photoSource = photoSource,
        )
        drawCaptionForPhotoLayout(canvas, ordered.firstOrNull(), options, palette, width, height)
        drawMapForPhotoLayout(canvas, photos, options, palette, width, height)
    }

    private suspend fun drawMagazine(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
        photoSource: PhotoSource,
    ) {
        val ordered = orderedPhotos(photos, options).take(MAX_GRID_PHOTOS)
        val hero = ordered.firstOrNull()
        val supporting = ordered.drop(1)
        val gap = min(width, height) * 0.014f
        val radius = min(width, height) * 0.022f
        if (width > height) {
            val split = width * 0.64f
            drawPhoto(canvas, hero, RectF(0f, 0f, split - gap / 2f, height.toFloat()), radius, palette, photoSource)
            drawPhotoGridInRect(
                canvas,
                supporting,
                RectF(split + gap / 2f, 0f, width.toFloat(), height.toFloat()),
                palette,
                gap,
                photoSource,
            )
        } else {
            val split = height * 0.64f
            drawPhoto(canvas, hero, RectF(0f, 0f, width.toFloat(), split - gap / 2f), radius, palette, photoSource)
            drawPhotoGridInRect(
                canvas,
                supporting,
                RectF(0f, split + gap / 2f, width.toFloat(), height.toFloat()),
                palette,
                gap,
                photoSource,
            )
        }
        drawCaptionForPhotoLayout(canvas, hero, options, palette, width, height)
        drawMapForPhotoLayout(canvas, photos, options, palette, width, height)
    }

    private fun drawPhotoGridInRect(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        rect: RectF,
        palette: Palette,
        gap: Float,
        photoSource: PhotoSource,
    ) {
        if (photos.isEmpty()) {
            drawPhoto(canvas, null, rect, min(rect.width(), rect.height()) * 0.04f, palette, photoSource)
            return
        }
        val columns = when (photos.size) {
            1 -> 1
            in 2..4 -> 2
            in 5..9 -> 3
            else -> 4
        }
        val rows = ceil(photos.size / columns.toFloat()).toInt()
        val cellWidth = (rect.width() - gap * (columns - 1)) / columns
        val cellHeight = (rect.height() - gap * (rows - 1)) / rows
        val radius = min(cellWidth, cellHeight) * 0.06f
        photos.forEachIndexed { index, photo ->
            val row = index / columns
            val itemsInRow = min(columns, photos.size - row * columns)
            val rowWidth = itemsInRow * cellWidth + (itemsInRow - 1) * gap
            val rowLeft = rect.left + (rect.width() - rowWidth) / 2f
            val column = index % columns
            val left = rowLeft + column * (cellWidth + gap)
            val top = rect.top + row * (cellHeight + gap)
            drawPhoto(canvas, photo, RectF(left, top, left + cellWidth, top + cellHeight), radius, palette, photoSource)
        }
    }

    private fun drawCaptionForPhotoLayout(
        canvas: Canvas,
        photo: GeoPhoto?,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        val lines = buildCaptionLines(
            photo,
            options.showDate,
            options.showCoordinates,
            options.customCaption,
            options.showCaption,
        )
        if (lines.isNotEmpty()) {
            val defaultWidth = if (options.showMiniMap) 0.55f else 0.72f
            drawFloatingCaption(canvas, lines, options, palette, width, height, defaultWidthFrac = defaultWidth)
        }
    }

    private suspend fun drawMapForPhotoLayout(
        canvas: Canvas,
        photos: List<GeoPhoto>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
    ) {
        if (!options.showMiniMap || !options.renderMiniMap) return
        val source = mapPhoto(photos, options) ?: return
        val inset = width * 0.04f
        val size = min(width, height) * PHOTO_WITH_MAP_SIZE_FRACTION
        val rect = options.mapPlacement?.let { placement ->
            val shorter = min(width, height).toFloat()
            val mapWidth = (placement.widthFrac * shorter).coerceAtLeast(64f)
            val centerX = placement.centerX * width
            val centerY = placement.centerY * height
            RectF(
                centerX - mapWidth / 2f,
                centerY - mapWidth / 2f,
                centerX + mapWidth / 2f,
                centerY + mapWidth / 2f,
            )
        } ?: mapRectForCorner(
            options.mapCorner,
            width,
            height,
            size,
            inset,
            avoidBottomLeft = options.showCaption,
        )
        canvas.save()
        val mapRotation = options.mapPlacement?.rotationDeg ?: 0f
        if (mapRotation != 0f) canvas.rotate(mapRotation, rect.centerX(), rect.centerY())
        drawMap(canvas, listOf(source), rect, palette, options)
        canvas.restore()
    }

    private fun drawFloatingCaption(
        canvas: Canvas,
        lines: List<String>,
        options: CollageOptions,
        palette: Palette,
        width: Int,
        height: Int,
        defaultWidthFrac: Float,
    ) {
        val inset = width * 0.04f
        val placement = options.captionPlacement
        val style = options.captionStyle
        val shorter = min(width, height).toFloat()
        val widthFrac = placement?.widthFrac ?: defaultWidthFrac
        val captionWidth = (widthFrac * width).coerceIn(width * 0.3f, width - inset * 2f)
        val titleSize = ((width * 0.036f).coerceIn(28f, 56f) * style.textSizeScale)
            .coerceIn(20f, 100f)
        val subSize = titleSize * 0.55f
        val extraLineCount = (lines.size - 1).coerceAtLeast(0)
        val captionHeight = titleSize + 12f + subSize * extraLineCount + extraLineCount * 8f + inset * 2f
        val left = placement?.originX?.let { (it * width).coerceIn(0f, width - captionWidth) } ?: inset
        val top = placement?.originY?.let { (it * height).coerceIn(0f, height - captionHeight) }
            ?: (height - inset - captionHeight)
        val rect = RectF(left, top, left + captionWidth, top + captionHeight)
        val radius = when (style.shape) {
            CollageTextShape.PILL -> captionHeight / 2f
            CollageTextShape.ROUNDED -> style.cornerRadiusFrac * shorter
            CollageTextShape.NONE, CollageTextShape.RECTANGLE -> 0f
        }
        canvas.save()
        val rotation = placement?.rotationDeg ?: 0f
        if (rotation != 0f) canvas.rotate(rotation, rect.centerX(), rect.centerY())
        if (style.shape != CollageTextShape.NONE && style.backgroundOpacity > 0f) {
            drawRoundRect(
                canvas,
                rect,
                radius,
                (style.backgroundColor ?: palette.surface).withAlpha(style.backgroundOpacity),
                shadow = true,
            )
        }
        if (style.borderWidthFrac > 0f) {
            val borderWidth = (style.borderWidthFrac * shorter).coerceAtLeast(1f)
            val borderRect = RectF(
                rect.left + borderWidth / 2f,
                rect.top + borderWidth / 2f,
                rect.right - borderWidth / 2f,
                rect.bottom - borderWidth / 2f,
            )
            canvas.drawRoundRect(
                borderRect,
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.borderColor ?: palette.onSurface
                    this.style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                },
            )
        }
        val titleColor = style.textColor ?: palette.onSurface
        val subtitleColor = style.textColor ?: palette.onSurfaceVariant
        var y = rect.top + inset + titleSize
        drawText(
            canvas,
            ellipsize(lines.first(), rect.width() - inset * 2f, titleSize, bold = true),
            rect.left + inset,
            y,
            titleSize,
            titleColor,
            bold = true,
        )
        lines.drop(1).forEach { line ->
            y += subSize + 8f
            drawText(
                canvas,
                ellipsize(line, rect.width() - inset * 2f, subSize),
                rect.left + inset,
                y,
                subSize,
                subtitleColor,
            )
        }
        canvas.restore()
    }

    private fun drawCard(
        canvas: Canvas,
        photo: GeoPhoto,
        card: CollageCard,
        canvasWidth: Int,
        canvasHeight: Int,
        palette: Palette,
        photoSource: PhotoSource,
    ) {
        val shorter = min(canvasWidth, canvasHeight).toFloat()
        val w = (card.widthFrac * shorter).coerceAtLeast(48f)
        val h = w
        val cx = card.centerX * canvasWidth
        val cy = card.centerY * canvasHeight
        val rect = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        canvas.save()
        if (card.rotationDeg != 0f) canvas.rotate(card.rotationDeg, cx, cy)
        val corner = (min(rect.width(), rect.height()) * 0.12f).coerceIn(14f, 40f)
        // White photo frame mat for that Instagram polaroid feel.
        val matInset = (min(rect.width(), rect.height()) * 0.05f).coerceIn(6f, 20f)
        val matRect = RectF(
            rect.left - matInset,
            rect.top - matInset,
            rect.right + matInset,
            rect.bottom + matInset,
        )
        drawRoundRect(canvas, matRect, corner + matInset * 0.6f, palette.surface, shadow = true)
        val decoded = decodePhotoBitmap(photo, rect.width().toInt(), rect.height().toInt(), photoSource)
        if (decoded == null) {
            drawRoundRect(canvas, rect, corner, palette.placeholder, shadow = false)
        } else {
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) })
            drawDecodedPhoto(canvas, decoded, rect)
            canvas.restore()
            decoded.bitmap.recycle()
        }
        canvas.restore()
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
        photoSource: PhotoSource,
    ) {
        val photo = orderedPhotos(photos, options).firstOrNull()
        val captionLines = buildCaptionLines(
            photo,
            options.showDate,
            options.showCoordinates,
            options.customCaption,
            options.showCaption,
        )
        val margin = width * 0.06f
        val heroBottom = if (captionLines.isEmpty()) height - margin else height * 0.66f
        val hero = RectF(margin, margin, width - margin, heroBottom)
        drawPhoto(canvas, photo, hero, 46f, palette, photoSource)

        // Text is optional. The map remains an independent floating layer, so it can be
        // placed in any corner just like it can in every other template.
        if (captionLines.isEmpty()) {
            drawMapForPhotoLayout(canvas, photos, options, palette, width, height)
            return
        }

        val captionGap = margin * 0.5f
        val defaultCaption = RectF(margin, hero.bottom + captionGap, width - margin, height - margin)
        val placement = options.captionPlacement
        val captionWidth = placement
            ?.let { (it.widthFrac * width).coerceIn(width * 0.3f, width * 0.9f) }
            ?: defaultCaption.width()
        val captionHeight = defaultCaption.height()
        val captionLeft = placement
            ?.let { (it.originX * width).coerceIn(0f, width - captionWidth) }
            ?: defaultCaption.left
        val captionTop = placement
            ?.let { (it.originY * height).coerceIn(0f, height - captionHeight) }
            ?: defaultCaption.top
        val caption = RectF(
            captionLeft,
            captionTop,
            captionLeft + captionWidth,
            captionTop + captionHeight,
        )
        val captionStyle = options.captionStyle
        val shorter = min(width, height).toFloat()
        val captionRadius = when (captionStyle.shape) {
            CollageTextShape.PILL -> caption.height() / 2f
            CollageTextShape.ROUNDED -> captionStyle.cornerRadiusFrac * shorter
            CollageTextShape.NONE, CollageTextShape.RECTANGLE -> 0f
        }
        canvas.save()
        val captionRotation = placement?.rotationDeg ?: 0f
        if (captionRotation != 0f) {
            canvas.rotate(captionRotation, caption.centerX(), caption.centerY())
        }
        if (captionStyle.shape != CollageTextShape.NONE && captionStyle.backgroundOpacity > 0f) {
            drawRoundRect(
                canvas,
                caption,
                captionRadius,
                (captionStyle.backgroundColor ?: palette.surface).withAlpha(captionStyle.backgroundOpacity),
                shadow = true,
            )
        }
        if (captionStyle.borderWidthFrac > 0f) {
            val borderWidth = (captionStyle.borderWidthFrac * shorter).coerceAtLeast(1f)
            canvas.drawRoundRect(
                RectF(
                    caption.left + borderWidth / 2f,
                    caption.top + borderWidth / 2f,
                    caption.right - borderWidth / 2f,
                    caption.bottom - borderWidth / 2f,
                ),
                captionRadius,
                captionRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = captionStyle.borderColor ?: palette.onSurface
                    style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                },
            )
        }

        val captionPadding = caption.width() * 0.05f
        val textRight = caption.right - captionPadding
        val textLeft = caption.left + captionPadding
        val textWidth = textRight - textLeft

        val titleSize = ((width * 0.034f).coerceIn(34f, 56f) * captionStyle.textSizeScale)
            .coerceIn(22f, 100f)
        val titleBottomY = caption.top + captionPadding + titleSize + 8f
        if (captionLines.isNotEmpty()) {
            drawText(
                canvas,
                ellipsize(captionLines.first(), textWidth, titleSize, bold = true),
                textLeft,
                titleBottomY,
                titleSize,
                captionStyle.textColor ?: palette.onSurface,
                bold = true,
            )
            var cursorY = titleBottomY
            val size = titleSize * 0.55f
            captionLines.drop(1).forEach { line ->
                cursorY += size + 8f
                drawText(
                    canvas,
                    ellipsize(line, textWidth, size),
                    textLeft,
                    cursorY,
                    size,
                    captionStyle.textColor ?: palette.onSurfaceVariant,
                )
            }
        }
        canvas.restore()
        drawMapForPhotoLayout(canvas, photos, options, palette, width, height)
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
        // Keep in sync with PulsePin in CollageEditorScreen — both use ~3.5% of the map's
        // shorter side so the live preview and exported PNG show the same marker size.
        val base = (min(rect.width(), rect.height()) * 0.035f).coerceIn(9f, 24f)
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

    private fun drawPhoto(
        canvas: Canvas,
        photo: GeoPhoto?,
        rect: RectF,
        radius: Float,
        palette: Palette,
        photoSource: PhotoSource,
    ) {
        val decoded = photo?.let {
            decodePhotoBitmap(it, rect.width().toInt(), rect.height().toInt(), photoSource)
        }
        if (decoded == null) {
            drawRoundRect(canvas, rect, radius, palette.placeholder, shadow = true)
            drawText(canvas, context.getString(R.string.app_name), rect.left + 28f, rect.centerY(), 28f, palette.onSurfaceVariant, bold = true)
            return
        }
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        drawDecodedPhoto(canvas, decoded, rect)
        canvas.restore()
        decoded.bitmap.recycle()
    }

    private fun decodePhotoBitmap(
        photo: GeoPhoto,
        reqWidth: Int,
        reqHeight: Int,
        source: PhotoSource,
    ): DecodedPhoto? {
        val useThumbnail = source == PhotoSource.PREVIEW && photo.thumbnailUri != null
        val uri = if (useThumbnail) photo.thumbnailUri!! else photo.imageUri
        val rotation = if (useThumbnail) 0 else photo.orientationDegrees?.normalizedRightAngle() ?: 0
        val rawReqWidth = if (rotation == 90 || rotation == 270) reqHeight else reqWidth
        val rawReqHeight = if (rotation == 90 || rotation == 270) reqWidth else reqHeight
        return decodeBitmap(uri, rawReqWidth, rawReqHeight)?.let { DecodedPhoto(it, rotation) }
    }

    private fun drawDecodedPhoto(canvas: Canvas, decoded: DecodedPhoto, dst: RectF) {
        val orientedWidth = if (decoded.rotationDegrees == 90 || decoded.rotationDegrees == 270) {
            decoded.bitmap.height
        } else {
            decoded.bitmap.width
        }
        val orientedHeight = if (decoded.rotationDegrees == 90 || decoded.rotationDegrees == 270) {
            decoded.bitmap.width
        } else {
            decoded.bitmap.height
        }
        val src = cropSource(orientedWidth, orientedHeight, dst)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        canvas.save()
        canvas.clipRect(dst)
        canvas.translate(dst.left, dst.top)
        canvas.scale(dst.width() / src.width(), dst.height() / src.height())
        canvas.translate(-src.left.toFloat(), -src.top.toFloat())
        canvas.concat(rawToOrientedMatrix(decoded.bitmap, decoded.rotationDegrees))
        canvas.drawBitmap(decoded.bitmap, 0f, 0f, paint)
        canvas.restore()
    }

    private fun rawToOrientedMatrix(bitmap: Bitmap, rotationDegrees: Int): Matrix = Matrix().apply {
        when (rotationDegrees) {
            90 -> {
                setRotate(90f)
                postTranslate(bitmap.height.toFloat(), 0f)
            }
            180 -> {
                setRotate(180f)
                postTranslate(bitmap.width.toFloat(), bitmap.height.toFloat())
            }
            270 -> {
                setRotate(270f)
                postTranslate(0f, bitmap.width.toFloat())
            }
        }
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
        return cropSource(bitmap.width, bitmap.height, dst)
    }

    private fun cropSource(sourceWidth: Int, sourceHeight: Int, dst: RectF): Rect {
        val dstRatio = dst.width() / dst.height()
        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
        return if (sourceRatio > dstRatio) {
            val width = (sourceHeight * dstRatio).toInt()
            val left = (sourceWidth - width) / 2
            Rect(left, 0, left + width, sourceHeight)
        } else {
            val height = (sourceWidth / dstRatio).toInt()
            val top = (sourceHeight - height) / 2
            Rect(0, top, sourceWidth, top + height)
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

    private enum class PhotoSource { PREVIEW, ORIGINAL }

    private data class DecodedPhoto(
        val bitmap: Bitmap,
        val rotationDegrees: Int,
    )

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

    private fun drawPinDot(canvas: Canvas, x: Float, y: Float, color: Int) {
        canvas.drawCircle(x, y, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE })
        canvas.drawCircle(x, y, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
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
        const val MAX_GRID_PHOTOS = 12
        const val MAX_CAPTION_LINES = 4
        // Keep in sync with StaticMapsClient.DEFAULT_SINGLE_LOCATION_ZOOM / CollageOptions.mapZoom
        // defaults. When the requested options match these, the on-disk snapshot is reused as-is.
        const val DEFAULT_CACHED_MAP_ZOOM = 14
    }
}
