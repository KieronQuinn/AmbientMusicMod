package com.kieronquinn.app.ambientmusicmod.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.RecognitionBuffer
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.RecognitionPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onRecognitionPeriodClicked()
    abstract fun onRecognitionBufferClicked()
    abstract fun onTriggerWhenScreenOnChanged(enabled: Boolean)
    abstract fun onBatterySaverChanged(enabled: Boolean)
    abstract fun onBedtimeClicked()
    abstract fun onAdvancedClicked()
    abstract fun onAlbumArtChanged(enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val recognitionPeriod: RecognitionPeriod,
            val recognitionBuffer: RecognitionBuffer,
            val triggerWhenScreenOn: Boolean,
            val runOnBatterySaver: Boolean,
            val bedtimeMode: Boolean,
            val albumArtEnabled: Boolean
        ): State()
    }

}

class SettingsViewModelImpl(
    settingsRepository: SettingsRepository,
    deviceConfigRepository: DeviceConfigRepository,
    private val navigation: ContainerNavigation
): SettingsViewModel() {

    private val triggerWhenScreenOn = settingsRepository.triggerWhenScreenOn
    private val runOnBatterySaver = settingsRepository.runOnBatterySaver
    private val showAlbumArt = deviceConfigRepository.showAlbumArt

    private val recognitionState = combine(
        settingsRepository.recognitionPeriod.asFlow(),
        settingsRepository.recognitionBuffer.asFlow()
    ) { period, buffer ->
        Pair(period, buffer)
    }

    override val state = combine(
        recognitionState,
        triggerWhenScreenOn.asFlow(),
        runOnBatterySaver.asFlow(),
        settingsRepository.bedtimeModeEnabled.asFlow(),
        showAlbumArt.asFlow()
    ) { recognitionState, screenOn, batterySaver, bedtime, albumArt ->
        State.Loaded(
            recognitionState.first,
            recognitionState.second,
            screenOn,
            batterySaver,
            bedtime,
            albumArt
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onRecognitionPeriodClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsRecognitionPeriodFragment())
        }
    }

    override fun onRecognitionBufferClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsRecognitionBufferFragment())
        }
    }

    override fun onTriggerWhenScreenOnChanged(enabled: Boolean) {
        viewModelScope.launch {
            triggerWhenScreenOn.set(enabled)
        }
    }

    override fun onBatterySaverChanged(enabled: Boolean) {
        viewModelScope.launch {
            runOnBatterySaver.set(enabled)
        }
    }

    override fun onAlbumArtChanged(enabled: Boolean) {
        viewModelScope.launch {
            showAlbumArt.set(enabled)
        }
    }

    override fun onBedtimeClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsBedtimeFragment())
        }
    }

    override fun onAdvancedClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsAdvancedFragment())
        }
    }

}