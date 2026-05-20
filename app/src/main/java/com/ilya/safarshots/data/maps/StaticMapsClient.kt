package com.ilya.safarshots.data.maps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ilya.safarshots.BuildConfig
import com.ilya.safarshots.domain.model.PhotoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticMapsClient @Inject constructor() {
    private val cache = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>): Boolean = size > MAX_CACHE
    }
    private val cacheLock = Any()

    val isConfigured: Boolean get() = BuildConfig.MAPS_API_KEY.isNotBlank()

    suspend fun fetch(
        locations: List<PhotoLocation>,
        targetWidth: Int,
        targetHeight: Int,
        dark: Boolean,
        includeMarker: Boolean = true,
        zoom: Int = DEFAULT_SINGLE_LOCATION_ZOOM,
        useTouristMapStyle: Boolean = true,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured || locations.isEmpty()) return@withContext null
        val widthCss = (targetWidth / SCALE).coerceIn(MIN_DIMENSION, MAX_DIMENSION)
        val heightCss = (targetHeight / SCALE).coerceIn(MIN_DIMENSION, MAX_DIMENSION)
        val url = buildUrl(locations, widthCss, heightCss, dark, includeMarker, zoom, useTouristMapStyle)
        synchronized(cacheLock) { cache[url] }?.let { return@withContext it }

        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                doInput = true
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()?.also { bitmap ->
            synchronized(cacheLock) { cache[url] = bitmap }
        }
    }

    private fun buildUrl(
        locations: List<PhotoLocation>,
        widthCss: Int,
        heightCss: Int,
        dark: Boolean,
        includeMarker: Boolean,
        zoom: Int,
        useTouristMapStyle: Boolean,
    ): String {
        val sb = StringBuilder("https://maps.googleapis.com/maps/api/staticmap?")
        sb.append("size=").append(widthCss).append('x').append(heightCss)
        sb.append("&scale=").append(SCALE)
        sb.append("&maptype=roadmap")
        if (locations.size == 1) {
            val loc = locations.first()
            sb.append("&center=").append(formatCoord(loc.latitude)).append(',').append(formatCoord(loc.longitude))
            sb.append("&zoom=").append(zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        }
        if (includeMarker) {
            sb.append("&markers=").append("color:0x").append(MARKER_COLOR).append("%7Csize:mid")
            for (loc in locations) {
                sb.append("%7C").append(formatCoord(loc.latitude)).append(',').append(formatCoord(loc.longitude))
            }
        }
        if (locations.size > 1) {
            sb.append("&path=color:0x").append(PATH_COLOR).append("%7Cweight:5")
            for (loc in locations) {
                sb.append("%7C").append(formatCoord(loc.latitude)).append(',').append(formatCoord(loc.longitude))
            }
        }
        if (useTouristMapStyle) {
            for (style in TOURIST_STYLE) {
                sb.append("&style=").append(style)
            }
        }
        if (dark) {
            for (style in DARK_STYLE) {
                sb.append("&style=").append(style)
            }
        }
        sb.append("&key=").append(BuildConfig.MAPS_API_KEY)
        return sb.toString()
    }

    private fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    private companion object {
        const val SCALE = 2
        const val MIN_DIMENSION = 80
        const val MAX_DIMENSION = 640
        const val MAX_CACHE = 6
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 12_000
        const val MARKER_COLOR = "B45F06"
        const val PATH_COLOR = "0B57D0CC"
        const val DEFAULT_SINGLE_LOCATION_ZOOM = 14
        const val MIN_ZOOM = 11
        const val MAX_ZOOM = 17

        val TOURIST_STYLE = listOf(
            "feature:poi%7Celement:labels%7Cvisibility:off",
            "feature:poi.business%7Cvisibility:off",
            "feature:poi.business%7Celement:labels%7Cvisibility:off",
            "feature:poi.medical%7Cvisibility:off",
            "feature:poi.school%7Cvisibility:off",
            "feature:poi.sports_complex%7Cvisibility:off",
            "feature:poi.government%7Cvisibility:off",
            "feature:poi.place_of_worship%7Cvisibility:off",
            "feature:transit.station%7Cvisibility:off",
            "feature:poi.attraction%7Cvisibility:on",
        )

        val DARK_STYLE = listOf(
            "feature:all%7Celement:geometry%7Ccolor:0x212121",
            "feature:all%7Celement:labels.text.fill%7Ccolor:0xCFD8DC",
            "feature:all%7Celement:labels.text.stroke%7Ccolor:0x212121",
            "feature:road%7Celement:geometry%7Ccolor:0x2c2c2c",
            "feature:road.highway%7Celement:geometry%7Ccolor:0x3a3a3a",
            "feature:water%7Celement:geometry%7Ccolor:0x000000",
            "feature:poi%7Cvisibility:simplified",
        )
    }
}
