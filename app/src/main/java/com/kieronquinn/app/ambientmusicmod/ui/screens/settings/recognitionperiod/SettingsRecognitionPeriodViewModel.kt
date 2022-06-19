package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.RecognitionPeriod
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsRecognitionPeriodViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onAutomaticChanged(enabled: Boolean)
    abstract fun onPeriodSelected(period: RecognitionPeriod)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val adaptiveEnabled: Boolean,
            val period: RecognitionPeriod
        ): State()
    }

    sealed class SettingsRecognitionPeriodSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Period(
            val period: RecognitionPeriod,
            val enabled: Boolean,
            val onClicked: (period: RecognitionPeriod) -> Unit
        ): SettingsRecognitionPeriodSettingsItem(ItemType.PERIOD) {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }

        enum class ItemType: BaseSettingsItemType {
            PERIOD
        }
    }

}

class SettingsRecognitionPeriodViewModelImpl(
    settingsRepository: SettingsRepository
): SettingsRecognitionPeriodViewModel() {

    private val adaptivePeriod = settingsRepository.recognitionPeriodAdaptive
    private val recognitionPeriod = settingsRepository.recognitionPeriod

    override val state = combine(
        adaptivePeriod.asFlow(),
        recognitionPeriod.asFlow()
    ) { adaptive, period ->
        State.Loaded(adaptive, period)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onAutomaticChanged(enabled: Boolean) {
        viewModelScope.launch {
            adaptivePeriod.set(enabled)
            AmbientMusicModForegroundService.sendImmediateTrigger()
        }
    }

    override fun onPeriodSelected(period: RecognitionPeriod) {
        viewModelScope.launch {
            recognitionPeriod.set(period)
            AmbientMusicModForegroundService.sendImmediateTrigger()
        }
    }

}