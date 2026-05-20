package com.ilya.safarshots.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val showDeleteAllDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val photoRepository: GeoPhotoRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        settingsRepository.observe()
            .onEach { settings -> _state.update { it.copy(settings = settings) } }
            .launchIn(viewModelScope)
    }

    fun setWriteExif(value: Boolean) = settingsRepository.setWriteGeoDataToExif(value)

    fun setPreciseCoordinates(value: Boolean) = settingsRepository.setShowPreciseCoordinates(value)

    fun setDynamicColor(value: Boolean) = settingsRepository.setDynamicColor(value)

    fun setDeleteDialog(show: Boolean) {
        _state.update { it.copy(showDeleteAllDialog = show) }
    }

    fun deleteAll() {
        viewModelScope.launch {
            photoRepository.deleteAll()
            _state.update { it.copy(showDeleteAllDialog = false) }
            _messages.emit("Все локальные данные удалены")
        }
    }
}
