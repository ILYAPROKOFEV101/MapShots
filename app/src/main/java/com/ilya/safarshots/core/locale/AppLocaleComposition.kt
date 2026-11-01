package com.ilya.safarshots.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.ilya.safarshots.R
import com.ilya.safarshots.core.common.DateLabels
import com.ilya.safarshots.core.settings.AppLanguage
import java.util.Locale

@Composable
fun rememberDateLabels(): DateLabels = DateLabels(
    today = stringResource(R.string.date_today),
    yesterday = stringResource(R.string.date_yesterday),
    thisWeek = stringResource(R.string.date_this_week),
)

/**
 * Overrides Compose [LocalConfiguration] and [LocalResources] so [stringResource]
 * reads the user-selected app language immediately.
 *
 * [LocalContext] is intentionally left as the Activity instance — Hilt ViewModels
 * require an activity context and crash if it is replaced with [Context.createConfigurationContext].
 */
@Composable
fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val activityContext = LocalContext.current
    if (language == AppLanguage.SYSTEM) {
        content()
        return
    }

    val localizedResources = remember(activityContext, language) {
        activityContext.withAppLanguage(language).resources
    }
    val configuration = remember(localizedResources) {
        Configuration(localizedResources.configuration)
    }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalResources provides localizedResources,
    ) {
        content()
    }
}

private fun Context.withAppLanguage(language: AppLanguage): Context {
    val tag = language.tag ?: return this
    val locale = Locale.forLanguageTag(tag)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLocales(LocaleList(locale))
    return createConfigurationContext(config)
}
