package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionbuffer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.RecognitionBuffer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsRecognitionBufferViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onBufferSelected(buffer: RecognitionBuffer)

    sealed class State {
        object Loading: State()
        data class Loaded(val buffer: RecognitionBuffer): State()
    }

    sealed class SettingsRecognitionBufferSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Buffer(
            val buffer: RecognitionBuffer,
            val enabled: Boolean,
            val onClicked: (period: RecognitionBuffer) -> Unit
        ): SettingsRecognitionBufferSettingsItem(ItemType.BUFFER) {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }

        enum class ItemType: BaseSettingsItemType {
            BUFFER
        }
    }

}

class SettingsRecognitionBufferViewModelImpl(
    settingsRepository: SettingsRepository
): SettingsRecognitionBufferViewModel() {

    private val recognitionBuffer = settingsRepository.recognitionBuffer

    override val state = recognitionBuffer.asFlow().mapLatest {
        State.Loaded(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onBufferSelected(buffer: RecognitionBuffer) {
        viewModelScope.launch {
            recognitionBuffer.set(buffer)
        }
    }

}