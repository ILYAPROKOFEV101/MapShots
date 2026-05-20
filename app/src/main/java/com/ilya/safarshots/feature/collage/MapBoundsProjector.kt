package com.ilya.safarshots.feature.collage

import com.ilya.safarshots.domain.model.PhotoLocation
import kotlin.math.max

data class NormalizedMapPoint(
    val latitude: Double,
    val longitude: Double,
    val x: Float,
    val y: Float,
)

object MapBoundsProjector {
    fun project(
        locations: List<PhotoLocation>,
        paddingFraction: Float = 0.12f,
    ): List<NormalizedMapPoint> {
        if (locations.isEmpty()) return emptyList()
        val minLat = locations.minOf { it.latitude }
        val maxLat = locations.maxOf { it.latitude }
        val minLng = locations.minOf { it.longitude }
        val maxLng = locations.maxOf { it.longitude }
        val rawLatSpan = maxLat - minLat
        val rawLngSpan = maxLng - minLng
        if (rawLatSpan < 1e-6 && rawLngSpan < 1e-6) {
            return locations.map {
                NormalizedMapPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    x = 0.5f,
                    y = 0.5f,
                )
            }
        }
        val latSpan = max(1e-6, rawLatSpan)
        val lngSpan = max(1e-6, rawLngSpan)
        val content = 1f - paddingFraction * 2f
        return locations.map { location ->
            val x = paddingFraction + (((location.longitude - minLng) / lngSpan).toFloat() * content)
            val y = paddingFraction + ((1f - ((location.latitude - minLat) / latSpan).toFloat()) * content)
            NormalizedMapPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                x = x.coerceIn(paddingFraction, 1f - paddingFraction),
                y = y.coerceIn(paddingFraction, 1f - paddingFraction),
            )
        }
    }
}
