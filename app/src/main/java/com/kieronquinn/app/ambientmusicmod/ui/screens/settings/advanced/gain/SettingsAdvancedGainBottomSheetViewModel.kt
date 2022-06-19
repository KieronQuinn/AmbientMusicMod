package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.gain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepositoryImpl.Companion.DEFAULT_RECORDING_GAIN
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class SettingsAdvancedGainBottomSheetViewModel: ViewModel() {

    abstract val gain: StateFlow<Float>

    abstract fun setGain(value: Float)

    abstract fun onPositiveClicked()
    abstract fun onNegativeClicked()
    abstract fun onNeutralClicked()

}

class SettingsAdvancedGainBottomSheetViewModelImpl(
    deviceConfigRepository: DeviceConfigRepository,
    private val navigation: ContainerNavigation
): SettingsAdvancedGainBottomSheetViewModel() {

    private val gainConfig = deviceConfigRepository.recordingGain

    override val gain = MutableStateFlow(gainConfig.getSync())

    override fun setGain(value: Float) {
        viewModelScope.launch {
            gain.emit(value)
        }
    }

    override fun onPositiveClicked() {
        viewModelScope.launch {
            gainConfig.set(gain.value)
            navigation.navigateBack()
        }
    }

    override fun onNegativeClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onNeutralClicked() {
        viewModelScope.launch {
            gain.emit(DEFAULT_RECORDING_GAIN)
        }
    }

}