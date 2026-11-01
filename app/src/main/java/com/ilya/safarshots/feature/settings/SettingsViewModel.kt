package com.ilya.safarshots.feature.settings

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.safarshots.R
import com.ilya.safarshots.core.dunta.DuntaSdkController
import com.ilya.safarshots.core.dunta.DuntaSdkUiState
import com.ilya.safarshots.core.locale.AppLocaleController
import com.ilya.safarshots.core.settings.AppLanguage
import com.ilya.safarshots.core.settings.AppSettings
import com.ilya.safarshots.core.settings.AppSettingsRepository
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val duntaSdk: DuntaSdkUiState = DuntaSdkUiState(),
    val showDeleteAllDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val photoRepository: GeoPhotoRepository,
    private val localeController: AppLocaleController,
    private val duntaSdkController: DuntaSdkController,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<Int>()
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    init {
        settingsRepository.observe()
            .onEach { settings -> _state.update { it.copy(settings = settings) } }
            .launchIn(viewModelScope)
        duntaSdkController.state
            .onEach { duntaSdk -> _state.update { it.copy(duntaSdk = duntaSdk) } }
            .launchIn(viewModelScope)
    }

    fun setWriteExif(value: Boolean) = settingsRepository.setWriteGeoDataToExif(value)

    fun setPreciseCoordinates(value: Boolean) = settingsRepository.setShowPreciseCoordinates(value)

    fun setDynamicColor(value: Boolean) = settingsRepository.setDynamicColor(value)

    fun setDuntaDataSharing(activity: AppCompatActivity?, enabled: Boolean) {
        if (!_state.value.duntaSdk.isConfigured) {
            emitMessage(R.string.settings_dunta_not_configured)
            return
        }

        if (activity == null) {
            emitMessage(R.string.settings_dunta_activity_unavailable)
            return
        }

        if (enabled) {
            duntaSdkController.showConsent()
        } else {
            duntaSdkController.disable()
            emitMessage(R.string.settings_dunta_disabled)
        }
    }

    fun setLanguage(language: AppLanguage) {
        settingsRepository.setLanguage(language)
        localeController.apply(language)
        _state.update { it.copy(showLanguageDialog = false) }
    }

    fun setLanguageDialog(show: Boolean) {
        _state.update { it.copy(showLanguageDialog = show) }
    }

    fun setDeleteDialog(show: Boolean) {
        _state.update { it.copy(showDeleteAllDialog = show) }
    }

    fun deleteAll() {
        viewModelScope.launch {
            photoRepository.deleteAll()
            _state.update { it.copy(showDeleteAllDialog = false) }
            _messages.emit(R.string.settings_data_deleted)
        }
    }

    private fun emitMessage(resId: Int) {
        viewModelScope.launch {
            _messages.emit(resId)
        }
    }
}
