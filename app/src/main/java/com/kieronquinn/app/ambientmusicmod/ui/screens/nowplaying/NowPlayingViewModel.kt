package com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerMessage
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.ui.screens.container.ContainerFragmentDirections
import com.kieronquinn.app.ambientmusicmod.ui.screens.recognition.RecognitionViewModel.StartState
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class NowPlayingViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val recognitionDialogShowing: StateFlow<Boolean>

    abstract fun reloadSettings()

    abstract fun onBannerButtonClicked(event: NavigationEvent)
    abstract fun onSwitchClicked(checked: Boolean)
    abstract fun onNotificationsClicked()
    abstract fun onLockscreenClicked()
    abstract fun onOnDemandClicked()
    abstract fun onHistoryClicked()
    abstract fun onSettingsClicked()
    abstract fun onBackupRestoreClicked()
    abstract fun onFaqClicked()
    abstract fun onFooterLinkClicked(isDark: Boolean)

    abstract fun onRecogniseFabClicked()

    abstract fun onWallpaperColourPickerClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val settings: SettingsState): State()
    }

    sealed class NowPlayingSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        object Header: NowPlayingSettingsItem(ItemType.HEADER) {
            override fun equals(other: Any?): Boolean {
                return other is Header
            }
        }
        data class Banner(val bannerMessage: BannerMessage, val onButtonClick: (NavigationEvent) -> Unit):
            NowPlayingSettingsItem(ItemType.BANNER)
        data class Footer(val onLinkClicked: () -> Unit):
            NowPlayingSettingsItem(ItemType.FOOTER)

        enum class ItemType: BaseSettingsItemType {
            HEADER, BANNER, FOOTER
        }
    }

}

class NowPlayingViewModelImpl(
    private val remoteSettingsRepository: RemoteSettingsRepository,
    private val recognitionRepository: RecognitionRepository,
    private val navigation: ContainerNavigation,
    private val rootNavigation: RootNavigation,
    private val deviceConfigRepository: DeviceConfigRepository
): NowPlayingViewModel() {

    companion object {
        private const val URL_LEARN_MORE = "https://support.google.com/pixelphone/answer/7535326?dark="
    }

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    private val notificationIntent by lazy {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, PACKAGE_NAME_PAM)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    private val historyIntent by lazy {
        Intent("com.google.intelligence.sense.NOW_PLAYING_HISTORY").apply {
            `package` = PACKAGE_NAME_PAM
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override val state = reloadBus.flatMapLatest {
        remoteSettingsRepository
            .getRemoteSettings(viewModelScope)
            .filterNotNull().mapLatest {
                State.Loaded(it)
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val recognitionDialogShowing = recognitionRepository.recognitionDialogShowing
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onBannerButtonClicked(event: NavigationEvent) {
        viewModelScope.launch {
            navigation.navigate(event)
        }
    }

    override fun reloadSettings() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
            deviceConfigRepository.sendValues()
        }
    }

    override fun onSwitchClicked(checked: Boolean) {
        viewModelScope.launch {
            remoteSettingsRepository.commitChanges(SettingsStateChange(
                mainEnabled = checked
            ))
        }
    }

    override fun onNotificationsClicked() {
        viewModelScope.launch {
            navigation.navigate(notificationIntent)
        }
    }

    override fun onLockscreenClicked() {
        viewModelScope.launch {
            navigation.navigate(NowPlayingFragmentDirections.actionNowPlayingFragmentToLockScreenFragment())
        }
    }

    override fun onOnDemandClicked() {
        viewModelScope.launch {
            navigation.navigate(NowPlayingFragmentDirections.actionNowPlayingFragmentToOnDemandFragment())
        }
    }

    override fun onHistoryClicked() {
        viewModelScope.launch {
            navigation.navigate(historyIntent)
        }
    }

    override fun onSettingsClicked() {
        viewModelScope.launch {
            navigation.navigate(NowPlayingFragmentDirections.actionNowPlayingFragmentToSettingsFragment())
        }
    }

    override fun onBackupRestoreClicked() {
        viewModelScope.launch {
            navigation.navigate(NowPlayingFragmentDirections.actionNowPlayingFragmentToBackupRestoreFragment())
        }
    }

    override fun onFaqClicked() {
        viewModelScope.launch {
            navigation.navigate(NowPlayingFragmentDirections.actionGlobalFaqFragment())
        }
    }

    override fun onFooterLinkClicked(isDark: Boolean) {
        viewModelScope.launch {
            val dark = if(isDark) "1" else "0"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("$URL_LEARN_MORE$dark")
            }
            navigation.navigate(intent)
        }
    }

    override fun onRecogniseFabClicked() {
        viewModelScope.launch {
            val settings = (state.value as? State.Loaded)?.settings as? SettingsState.Available
                ?: return@launch
            val source = if(settings.onDemandCapable){
                StartState.SOURCE_PICKER
            }else{
                StartState.RECOGNISE_NNFP
            }
            rootNavigation.navigate(
                ContainerFragmentDirections.actionContainerFragmentToRecognitionFragment(source)
            )
        }
    }

    override fun onWallpaperColourPickerClicked() {
        viewModelScope.launch {
            navigation.navigate(NowPlayingFragmentDirections.actionNowPlayingFragmentToWallpaperColourPickerBottomSheetFragment())
        }
    }

}