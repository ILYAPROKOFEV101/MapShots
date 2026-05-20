package com.ilya.safarshots.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.ilya.safarshots.core.permissions.PermissionController
import com.ilya.safarshots.domain.model.PhotoLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationClient {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocationOrNull(timeoutMillis: Long): PhotoLocation? {
        if (!PermissionController.hasAnyLocation(context)) return null
        val fine = PermissionController.hasFineLocation(context)
        return runCatching {
            val last = fusedLocationClient.lastLocation.awaitOrNull()?.toPhotoLocation(isApproximate = !fine)
            val tokenSource = CancellationTokenSource()
            val fresh = withTimeoutOrNull(timeoutMillis) {
                fusedLocationClient.getCurrentLocation(
                    if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    tokenSource.token,
                ).awaitOrNull()?.toPhotoLocation(isApproximate = !fine)
            }
            if (fresh == null) tokenSource.cancel()
            fresh ?: last
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    override fun observeLastKnownLocation(): Flow<PhotoLocation?> = flow {
        while (true) {
            emit(getCurrentLocationOrNull(timeoutMillis = 1200))
            delay(10_000)
        }
    }

    private fun Location.toPhotoLocation(isApproximate: Boolean): PhotoLocation = PhotoLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude.takeIf { hasAltitude() },
        accuracyMeters = accuracy.takeIf { hasAccuracy() },
        addressText = null,
        provider = provider,
        isApproximate = isApproximate,
    )

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener {
            if (continuation.isActive) continuation.resume(null)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel(CancellationException("Location task cancelled"))
        }
    }
}
