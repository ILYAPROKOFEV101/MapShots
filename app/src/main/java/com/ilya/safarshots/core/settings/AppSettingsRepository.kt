package com.ilya.safarshots.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Language preference exposed in Settings. `tag` is the BCP-47 tag fed into
 * [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]; `null`
 * means "follow the system locale".
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    INDONESIAN("id"),
    HINDI("hi"),
    URDU("ur");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

data class AppSettings(
    val writeGeoDataToExif: Boolean = false,
    val showPreciseCoordinates: Boolean = true,
    val dynamicColor: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("geomapshots_settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    fun observe(): StateFlow<AppSettings> = state.asStateFlow()

    fun current(): AppSettings = state.value

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

    fun setLanguage(language: AppLanguage) {
        preferences.edit()
            .putString(KEY_LANGUAGE, language.name)
            .putBoolean(KEY_LANGUAGE_INITIALIZED, true)
            .apply()
        state.value = read()
    }

    /**
     * True once we have written a language preference at least once. Used by
     * [com.ilya.safarshots.core.locale.AppLocaleController] to know whether
     * the region-based default still needs to be applied.
     */
    fun isLanguageInitialized(): Boolean =
        preferences.getBoolean(KEY_LANGUAGE_INITIALIZED, false)

    private fun read(): AppSettings = AppSettings(
        writeGeoDataToExif = preferences.getBoolean(KEY_EXIF, false),
        showPreciseCoordinates = preferences.getBoolean(KEY_PRECISE, true),
        dynamicColor = preferences.getBoolean(KEY_DYNAMIC, true),
        language = readLanguage(),
    )

    private fun readLanguage(): AppLanguage {
        val stored = preferences.getString(KEY_LANGUAGE, null) ?: return AppLanguage.SYSTEM
        return runCatching { AppLanguage.valueOf(stored) }.getOrDefault(AppLanguage.SYSTEM)
    }

    private companion object {
        const val KEY_EXIF = "write_geo_data_to_exif"
        const val KEY_PRECISE = "show_precise_coordinates"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_LANGUAGE_INITIALIZED = "app_language_initialized"
    }
}
