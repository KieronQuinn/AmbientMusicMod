package com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerAttentionLevel
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerButton
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.GoogleAppState
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class OnDemandViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun close()
    abstract fun onBannerButtonClicked(event: NavigationEvent)
    abstract fun onSwitchChanged(enabled: Boolean)
    abstract fun onOnDemandSaveChanged(enabled: Boolean)
    abstract fun onOnDemandVibrateChanged(enabled: Boolean)
    abstract fun onBannerDisableButtonClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val googleAppState: GoogleAppState,
            val onDemandEnabled: Boolean,
            val onDemandSaveEnabled: Boolean,
            val onDemandVibrate: Boolean
        ): State()
    }

    sealed class OnDemandSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Banner(
            val bannerTitle: CharSequence,
            val bannerContent: CharSequence,
            val attentionLevel: BannerAttentionLevel,
            val button: BannerButton?,
            val isOptionEnabled: Boolean,
            val onButtonClick: (NavigationEvent) -> Unit,
            val onDisableClick: () -> Unit
        ): OnDemandSettingsItem(ItemType.BANNER)

        object Header: OnDemandSettingsItem(ItemType.HEADER) {
            override fun equals(other: Any?): Boolean {
                return other is Header
            }
        }

        enum class ItemType: BaseSettingsItemType {
            BANNER, HEADER
        }
    }

}

class OnDemandViewModelImpl(
    private val remoteSettingsRepository: RemoteSettingsRepository,
    private val deviceConfig: DeviceConfigRepository,
    private val navigation: ContainerNavigation
): OnDemandViewModel() {

    override val state = combine(
        remoteSettingsRepository.getRemoteSettings(viewModelScope),
        remoteSettingsRepository.googleAppSupported,
        deviceConfig.cacheShardEnabled.asFlow(),
        deviceConfig.onDemandVibrateEnabled.asFlow()
    ) { settings, googleApp, onDemandSave, onDemandVibrate ->
        val remoteSettings = settings as? SettingsState.Available ?: return@combine State.Loading
        State.Loaded(googleApp, remoteSettings.onDemandEnabled, onDemandSave, onDemandVibrate)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun close() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onBannerButtonClicked(event: NavigationEvent) {
        viewModelScope.launch {
            navigation.navigate(event)
        }
    }

    override fun onSwitchChanged(enabled: Boolean) {
        viewModelScope.launch {
            remoteSettingsRepository.commitChanges(SettingsStateChange(onDemandEnabled = enabled))
        }
    }

    override fun onOnDemandSaveChanged(enabled: Boolean) {
        viewModelScope.launch {
            deviceConfig.cacheShardEnabled.set(enabled)
        }
    }

    override fun onOnDemandVibrateChanged(enabled: Boolean) {
        viewModelScope.launch {
            deviceConfig.onDemandVibrateEnabled.set(enabled)
        }
    }

    override fun onBannerDisableButtonClicked() {
        viewModelScope.launch {
            remoteSettingsRepository.commitChanges(SettingsStateChange(onDemandEnabled = false))
        }
    }

}