package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class LockScreenOwnerInfoViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract fun onSwitchChanged(enabled: Boolean)
    abstract fun onShowNoteChanged(enabled: Boolean)
    abstract fun onFallbackClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val compatible: Boolean,
            val enabled: Boolean,
            val showNote: Boolean,
            val fallbackInfo: String
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class LockScreenOwnerInfoSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Header(
            val showNote: Boolean
        ): LockScreenOwnerInfoSettingsItem(ItemType.HEADER) {
            override fun equals(other: Any?): Boolean {
                if(other !is Header) return false
                return other.showNote == showNote
            }
        }

        object Banner: LockScreenOwnerInfoSettingsItem(ItemType.BANNER)
        object Footer: LockScreenOwnerInfoSettingsItem(ItemType.FOOTER)

        enum class ItemType: BaseSettingsItemType {
            HEADER, BANNER, FOOTER
        }
    }

}

class LockScreenOwnerInfoViewModelImpl(
    settingsRepository: SettingsRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val navigation: ContainerNavigation
): LockScreenOwnerInfoViewModel() {

    private val ownerInfo = settingsRepository.lockscreenOwnerInfo
    private val ownerInfoShowNote = settingsRepository.lockscreenOwnerInfoShowNote
    private val ownerInfoFallback = settingsRepository.lockscreenOwnerInfoFallback

    override val state = combine(
        ownerInfo.asFlow(), ownerInfoShowNote.asFlow(), ownerInfoFallback.asFlow()
    ) { enabled, showNote, fallback ->
        val isRooted = shizukuServiceRepository.runWithService { it.isRoot }.unwrap() ?: false
        State.Loaded(isRooted, enabled, showNote, fallback)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onSwitchChanged(enabled: Boolean) {
        viewModelScope.launch {
            if(!enabled){
                //Reset to the fallback now so it doesn't get stuck with the track info
                shizukuServiceRepository.runWithService {
                    it.setOwnerInfo(ownerInfoFallback.getSync())
                }
            }
            ownerInfo.set(enabled)
        }
    }

    override fun onShowNoteChanged(enabled: Boolean) {
        viewModelScope.launch {
            ownerInfoShowNote.set(enabled)
        }
    }

    override fun onFallbackClicked() {
        viewModelScope.launch {
            navigation.navigate(LockScreenOwnerInfoFragmentDirections
                .actionLockScreenOwnerInfoFragmentToLockScreenOwnerInfoFallbackBottomSheetFragment())
        }
    }

}