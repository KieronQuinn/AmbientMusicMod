package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.LockscreenOverlayStyle
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.AccessibilityRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.service.LockscreenOverlayAccessibilityService
import com.kieronquinn.app.ambientmusicmod.ui.screens.container.ContainerFragmentDirections
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getAccessibilityIntent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class LockScreenViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onSwitchChanged(context: Context, enabled: Boolean)
    abstract fun onOwnerInfoClicked()
    abstract fun onEnhancedChanged(enabled: Boolean)
    abstract fun onStyleChanged(style: LockscreenOverlayStyle)
    abstract fun onOnDemandChanged(enabled: Boolean)

    abstract fun onPositionClicked()
    abstract fun onClickActionClicked()
    abstract fun reload()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val enhancedEnabled: Boolean,
            val style: LockscreenOverlayStyle,
            val clickAction: SettingsRepository.LockscreenOnTrackClicked,
            val onDemandAvailable: Boolean,
            val onDemandEnabled: Boolean
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class LockScreenSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Header(
            val style: LockscreenOverlayStyle,
            val onStyleSelected: (style: LockscreenOverlayStyle) -> Unit,
            val onPositionClicked: () -> Unit
        ): LockScreenSettingsItem(ItemType.HEADER) {
            override fun equals(other: Any?): Boolean {
                if(other !is Header) return false
                return other.style == style
            }
        }

        object Footer: LockScreenSettingsItem(ItemType.FOOTER)

        enum class ItemType: BaseSettingsItemType {
            HEADER, FOOTER
        }
    }

}

class LockScreenViewModelImpl(
    settingsRepository: SettingsRepository,
    remoteSettingsRepository: RemoteSettingsRepository,
    private val accessibilityRepository: AccessibilityRepository,
    private val rootNavigation: RootNavigation,
    private val navigation: ContainerNavigation
): LockScreenViewModel() {

    private val overlayEnabled = accessibilityRepository.enabled
    private val overlayEnhanced = settingsRepository.lockscreenOverlayEnhanced
    private val style = settingsRepository.lockscreenOverlayStyle
    private val clicked = settingsRepository.lockscreenOverlayClicked
    private val onDemandLockscreenEnabled = settingsRepository.onDemandLockscreenEnabled

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    private val onDemand = combine(
        onDemandLockscreenEnabled.asFlow(),
        remoteSettingsRepository.getRemoteSettings(viewModelScope).filter { it is SettingsState.Available },
        reloadBus
    ) { enabled, remote, _ ->
        remote as SettingsState.Available
        Pair(remote.onDemandCapable && remote.onDemandEnabled, enabled)
    }

    override val state = combine(
        overlayEnabled,
        overlayEnhanced.asFlow(),
        style.asFlow(),
        clicked.asFlow(),
        onDemand
    ) { enabled, enhanced, style, clicked, demand ->
        State.Loaded(enabled, enhanced, style, clicked, demand.first, demand.second)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onSwitchChanged(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            accessibilityRepository.allowRestrictedIfNeeded()
            val toastMessage = if(enabled){
                R.string.lockscreen_enable_toast
            }else{
                R.string.lockscreen_disable_toast
            }
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            navigation.navigate(
                context.getAccessibilityIntent(LockscreenOverlayAccessibilityService::class.java)
            )
        }
    }

    override fun onOwnerInfoClicked() {
        viewModelScope.launch {
            navigation.navigate(LockScreenFragmentDirections.actionLockScreenFragmentToLockScreenOwnerInfoFragment())
        }
    }

    override fun onEnhancedChanged(enabled: Boolean) {
        viewModelScope.launch {
            overlayEnhanced.set(enabled)
        }
    }

    override fun onStyleChanged(style: LockscreenOverlayStyle) {
        viewModelScope.launch {
            this@LockScreenViewModelImpl.style.set(style)
        }
    }

    override fun onOnDemandChanged(enabled: Boolean) {
        viewModelScope.launch {
            onDemandLockscreenEnabled.set(enabled)
        }
    }

    override fun onPositionClicked() {
        viewModelScope.launch {
            val style = style.get()
            rootNavigation.navigate(ContainerFragmentDirections.actionContainerFragmentToLockScreenPositionFragment(style))
        }
    }

    override fun onClickActionClicked() {
        viewModelScope.launch {
            navigation.navigate(LockScreenFragmentDirections.actionLockScreenFragmentToLockScreenActionFragment())
        }
    }

    override fun reload() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }


}