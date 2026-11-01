package com.ilya.safarshots.core.locale

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ilya.safarshots.core.settings.AppLanguage
import com.ilya.safarshots.core.settings.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the app's UI language. Resolves the initial language
 * from the device region the first time the app starts (so Indian users get Hindi,
 * Indonesian users get Indonesian, and Pakistani users get Urdu out of the box), and keeps
 * [AppCompatDelegate.setApplicationLocales] in sync with the user's choice.
 */
@Singleton
class AppLocaleController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: AppSettingsRepository,
) {
    fun initialize() {
        val language = if (settingsRepository.isLanguageInitialized()) {
            settingsRepository.current().language
        } else {
            val detected = detectFromRegion()
            settingsRepository.setLanguage(detected)
            detected
        }
        apply(language)
    }

    fun apply(language: AppLanguage) {
        val locales = if (language.tag != null) {
            LocaleListCompat.forLanguageTags(language.tag)
        } else {
            LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun detectFromRegion(): AppLanguage {
        val country = currentCountry().uppercase(Locale.ROOT)
        return when (country) {
            "PK" -> AppLanguage.URDU
            "IN" -> AppLanguage.HINDI
            "ID" -> AppLanguage.INDONESIAN
            else -> AppLanguage.ENGLISH
        }
    }

    private fun currentCountry(): String {
        val configLocales = context.resources.configuration.locales
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !configLocales.isEmpty) {
            configLocales[0]
        } else {
            Locale.getDefault()
        }
        return locale.country.orEmpty()
    }
}
