package com.ilya.safarshots.core.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.ilya.safarshots.domain.model.PhotoLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressResolver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext

    suspend fun resolve(location: PhotoLocation): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            @Suppress("DEPRECATION")
            val address = Geocoder(appContext, Locale.getDefault())
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
            address?.displayText()
        }.getOrNull()
    }

    private fun Address.displayText(): String? {
        getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { return it }
        return listOfNotNull(
            thoroughfare,
            subThoroughfare,
            locality,
            adminArea,
            countryName,
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotBlank() }
    }
}
