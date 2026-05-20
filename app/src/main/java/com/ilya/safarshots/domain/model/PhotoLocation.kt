package com.ilya.safarshots.domain.model

data class PhotoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracyMeters: Float? = null,
    val addressText: String? = null,
    val provider: String? = null,
    val isApproximate: Boolean = false,
)
