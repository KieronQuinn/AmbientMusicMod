package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.BedtimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime

abstract class SettingsBedtimeViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onStartTimeChanged(newTime: Long)
    abstract fun onEndTimeChanged(newTime: Long)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val startTime: String,
            val endTime: String,
            val startLocalTime: LocalTime,
            val endLocalTime: LocalTime
        ): State()
    }

    sealed class SettingsBedtimeSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        object Header: SettingsBedtimeSettingsItem(ItemType.HEADER) {
            override fun equals(other: Any?): Boolean {
                return other is Header
            }
        }

        enum class ItemType: BaseSettingsItemType {
            HEADER
        }
    }

}

class SettingsBedtimeViewModelImpl(
    private val bedtimeRepository: BedtimeRepository
): SettingsBedtimeViewModel() {

    override val state = combine(
        bedtimeRepository.isEnabled(),
        bedtimeRepository.getStartTime(),
        bedtimeRepository.getEndTime()
    ) { enabled, start, end ->
        State.Loaded(
            enabled,
            bedtimeRepository.getFormattedTime(start),
            bedtimeRepository.getFormattedTime(end),
            start,
            end
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            bedtimeRepository.setEnabled(enabled)
            bedtimeRepository.checkTimeAndSyncWorkers()
        }
    }

    override fun onStartTimeChanged(newTime: Long) {
        viewModelScope.launch {
            bedtimeRepository.setStartTime(newTime)
            bedtimeRepository.checkTimeAndSyncWorkers()
        }
    }

    override fun onEndTimeChanged(newTime: Long) {
        viewModelScope.launch {
            bedtimeRepository.setEndTime(newTime)
            bedtimeRepository.checkTimeAndSyncWorkers()
        }
    }

}