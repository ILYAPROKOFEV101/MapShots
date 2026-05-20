package com.ilya.safarshots.core.location

import com.ilya.safarshots.domain.model.PhotoLocation
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    suspend fun getCurrentLocationOrNull(timeoutMillis: Long = 5000): PhotoLocation?
    fun observeLastKnownLocation(): Flow<PhotoLocation?>
}
