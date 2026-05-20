package com.ilya.safarshots.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val writeGeoDataToExif: Boolean = false,
    val showPreciseCoordinates: Boolean = true,
    val dynamicColor: Boolean = true,
)

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("geomapshots_settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    fun observe(): StateFlow<AppSettings> = state.asStateFlow()

    fun setWriteGeoDataToExif(value: Boolean) {
        preferences.edit().putBoolean(KEY_EXIF, value).apply()
        state.value = read()
    }

    fun setShowPreciseCoordinates(value: Boolean) {
        preferences.edit().putBoolean(KEY_PRECISE, value).apply()
        state.value = read()
    }

    fun setDynamicColor(value: Boolean) {
        preferences.edit().putBoolean(KEY_DYNAMIC, value).apply()
        state.value = read()
    }

    private fun read(): AppSettings = AppSettings(
        writeGeoDataToExif = preferences.getBoolean(KEY_EXIF, false),
        showPreciseCoordinates = preferences.getBoolean(KEY_PRECISE, true),
        dynamicColor = preferences.getBoolean(KEY_DYNAMIC, true),
    )

    private companion object {
        const val KEY_EXIF = "write_geo_data_to_exif"
        const val KEY_PRECISE = "show_precise_coordinates"
        const val KEY_DYNAMIC = "dynamic_color"
    }
}
