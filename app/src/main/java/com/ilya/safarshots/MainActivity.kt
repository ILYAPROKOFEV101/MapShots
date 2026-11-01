package com.ilya.safarshots

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilya.safarshots.core.dunta.BatteryOptimizationController
import com.ilya.safarshots.core.dunta.DuntaBatterySettingsRequester
import com.ilya.safarshots.core.dunta.DuntaSdkController
import com.ilya.safarshots.core.dunta.SupportModePromptDialog
import com.ilya.safarshots.core.locale.ProvideAppLocale
import com.ilya.safarshots.core.settings.AppSettingsRepository
import com.ilya.safarshots.design.GeoMapShotsTheme
import com.ilya.safarshots.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), DuntaBatterySettingsRequester {
    @Inject lateinit var settingsRepository: AppSettingsRepository
    @Inject lateinit var duntaSdkController: DuntaSdkController

    private var consentAwaitingPermission = false
    private var resumeAwaitingPermission = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (consentAwaitingPermission) {
            consentAwaitingPermission = false
            duntaSdkController.completeConsent(granted)
        } else if (resumeAwaitingPermission) {
            resumeAwaitingPermission = false
            duntaSdkController.resumeConsentedMode(granted)
        }
    }
    private val batterySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { duntaSdkController.onBatterySettingsReturned() }

    override fun openDuntaBatterySettings() {
        batterySettingsLauncher.launch(BatteryOptimizationController.settingsIntent())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consentAwaitingPermission = savedInstanceState?.getBoolean(PENDING_CONSENT) == true
        resumeAwaitingPermission = savedInstanceState?.getBoolean(PENDING_RESUME) == true
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observe().collectAsStateWithLifecycle()
            val supportMode by duntaSdkController.state.collectAsStateWithLifecycle()
            ProvideAppLocale(language = settings.language) {
                GeoMapShotsTheme(dynamicColor = settings.dynamicColor) {
                    AppNavGraph(modifier = Modifier)
                    if (supportMode.showConsent) {
                        SupportModePromptDialog(
                            sdkConfigured = supportMode.isConfigured,
                            onEnable = ::enableSupportMode,
                            onDismiss = duntaSdkController::decline,
                        )
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            if (duntaSdkController.isPeer) {
                if (duntaSdkController.canPostNotifications()) {
                    duntaSdkController.resumeConsentedMode(true)
                } else {
                    resumeAwaitingPermission = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                duntaSdkController.showInitialConsentIfNeeded()
            }
        } else if (savedInstanceState.getBoolean(SHOW_CONSENT)) {
            duntaSdkController.showConsent()
        }
    }

    override fun onResume() {
        super.onResume()
        duntaSdkController.syncStoredState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PENDING_CONSENT, consentAwaitingPermission)
        outState.putBoolean(PENDING_RESUME, resumeAwaitingPermission)
        outState.putBoolean(SHOW_CONSENT, duntaSdkController.state.value.showConsent)
        super.onSaveInstanceState(outState)
    }

    private fun enableSupportMode() {
        duntaSdkController.hideConsentForPermissionRequest()
        if (duntaSdkController.canPostNotifications()) {
            duntaSdkController.completeConsent(true)
        } else {
            consentAwaitingPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val SHOW_CONSENT = "support_mode_show_consent"
        const val PENDING_RESUME = "support_mode_pending_resume"
        const val PENDING_CONSENT = "support_mode_pending_consent"
    }
}
