package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.LockscreenOnTrackClicked
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class LockScreenActionViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onActionClicked(action: LockscreenOnTrackClicked)

    sealed class State {
        object Loading: State()
        data class Loaded(val action: LockscreenOnTrackClicked): State()
    }

    sealed class LockScreenActionSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Action(
            val action: LockscreenOnTrackClicked,
            val enabled: Boolean,
            val onClicked: (action: LockscreenOnTrackClicked) -> Unit
        ): LockScreenActionSettingsItem(ItemType.ACTION) {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }

        enum class ItemType: BaseSettingsItemType {
            ACTION
        }
    }

}

class LockScreenActionViewModelImpl(
    settings: SettingsRepository
): LockScreenActionViewModel() {

    private val action = settings.lockscreenOverlayClicked

    override val state = action.asFlow().map {
        State.Loaded(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onActionClicked(action: LockscreenOnTrackClicked) {
        viewModelScope.launch {
            this@LockScreenActionViewModelImpl.action.set(action)
        }
    }

}