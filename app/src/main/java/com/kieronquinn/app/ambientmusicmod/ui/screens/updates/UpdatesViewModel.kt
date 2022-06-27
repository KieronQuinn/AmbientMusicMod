package com.kieronquinn.app.ambientmusicmod.ui.screens.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.model.update.Release
import com.kieronquinn.app.ambientmusicmod.model.update.toRelease
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardDownloadState
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardsState
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository.UpdateState
import com.kieronquinn.app.ambientmusicmod.ui.screens.container.ContainerFragmentDirections
import com.kieronquinn.app.ambientmusicmod.utils.extensions.firstNotNull
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getNetworkCapabilities
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isCharging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class UpdatesViewModel: ViewModel() {

    abstract fun reload(clearCache: Boolean = false)
    abstract val state: StateFlow<State>

    abstract fun onShardsUpdateClicked(shardsState: ShardsState)
    abstract fun onShardsViewTracksClicked()
    abstract fun onCountryClicked()
    abstract fun onAMMUpdateClicked(label: String, updateState: UpdateState)
    abstract fun onPAMUpdateClicked(label: String, updateState: UpdateState)

    abstract fun onContributorsClicked()
    abstract fun onDonateClicked()
    abstract fun onGitHubClicked()
    abstract fun onLibrariesClicked()
    abstract fun onTwitterClicked()
    abstract fun onXdaClicked()

    abstract fun onAutomaticDatabaseUpdatesChanged(enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val shardsState: ShardsState,
            val ammState: UpdateState,
            val pamState: UpdateState,
            val automaticDatabaseUpdates: Boolean
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class UpdatesSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Shards(
            val shardsState: ShardsState,
            val onUpdateClicked: (ShardsState) -> Unit,
            val onViewTracksClicked: () -> Unit,
            val onCountryClicked: () -> Unit
        ): UpdatesSettingsItem(ItemType.SHARDS)

        data class AMM(
            val updateState: UpdateState,
            val onUpdateClicked: (UpdateState) -> Unit
        ): UpdatesSettingsItem(ItemType.AMM)

        data class PAM(
            val updateState: UpdateState,
            val onUpdateClicked: (UpdateState) -> Unit
        ): UpdatesSettingsItem(ItemType.PAM)

        data class About(
            val onContributorsClicked: () -> Unit,
            val onDonateClicked: () -> Unit,
            val onGitHubClicked: () -> Unit,
            val onTwitterClicked: () -> Unit,
            val onXdaClicked: () -> Unit,
            val onLibrariesClicked: () -> Unit
        ): UpdatesSettingsItem(ItemType.ABOUT)

        enum class ItemType: BaseSettingsItemType {
            SHARDS, AMM, PAM, ABOUT
        }
    }

}

class UpdatesViewModelImpl(
    private val shardsRepository: ShardsRepository,
    private val updatesRepository: UpdatesRepository,
    private val rootNavigation: RootNavigation,
    private val navigation: ContainerNavigation,
    private val deviceConfigRepository: DeviceConfigRepository,
    settingsRepository: SettingsRepository,
    context: Context
): UpdatesViewModel() {

    companion object {
        private const val LINK_TWITTER = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/twitter"
        private const val LINK_GITHUB = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/github"
        private const val LINK_XDA = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/xda"
        private const val LINK_DONATE = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/donate"
    }

    private val reloadBus = MutableStateFlow(Pair(System.currentTimeMillis(), false))
    private val automaticDatabaseUpdates = settingsRepository.automaticMusicDatabaseUpdates

    private fun getAmmState(clearCache: Boolean) = flow {
        emit(updatesRepository.getAMMUpdateState(clearCache))
    }.flowOn(Dispatchers.IO)

    private fun getPamState(clearCache: Boolean) = flow {
        emit(updatesRepository.getPAMUpdateState(clearCache))
    }.flowOn(Dispatchers.IO)

    private val downloadState = combine(
        shardsRepository.getCurrentDownloads(),
        deviceConfigRepository.superpacksRequireWiFi.asFlow(),
        deviceConfigRepository.superpacksRequireCharging.asFlow(),
        context.getNetworkCapabilities(),
        context.isCharging()
    ) { downloadCount, requireWifi, requireCharging, capabilities, charging ->
        if(downloadCount == 0) return@combine null
        when {
            !capabilities.unmetered && capabilities.hasInternet && requireWifi -> {
                ShardDownloadState.WAITING_FOR_NETWORK
            }
            !capabilities.hasInternet -> {
                ShardDownloadState.WAITING_FOR_NETWORK
            }
            !charging && requireCharging -> {
                ShardDownloadState.WAITING_FOR_CHARGING
            }
            else -> ShardDownloadState.DOWNLOADING
        }
    }

    override val state = reloadBus.flatMapLatest {
        combine(
            getAmmState(it.second),
            getPamState(it.second),
            downloadState,
            automaticDatabaseUpdates.asFlow()
        ) { amm, pam, download, automaticUpdates ->
            val shards = shardsRepository.getShardsState(it.second).firstNotNull().apply {
                downloadState = download
            }
            if(automaticUpdates && shards.updateAvailable && shards.remote != null){
                //Automatically apply the update, this will trigger a download if possible
                deviceConfigRepository.indexManifest.set(shards.remote.url)
            }
            State.Loaded(shards, amm, pam, automaticUpdates)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun reload(clearCache: Boolean) {
        viewModelScope.launch {
            reloadBus.emit(Pair(System.currentTimeMillis(), clearCache))
        }
    }

    override fun onShardsUpdateClicked(shardsState: ShardsState) {
        viewModelScope.launch {
            val remoteUrl = shardsState.remote?.url ?: return@launch
            deviceConfigRepository.indexManifest.set(remoteUrl)
        }
    }

    override fun onCountryClicked() {
        viewModelScope.launch {
            navigation.navigate(UpdatesFragmentDirections.actionUpdatesFragmentToCountryPickerFragment())
        }
    }

    override fun onAMMUpdateClicked(label: String, updateState: UpdateState) {
        val release = updateState.toRelease(label) ?: return
        viewModelScope.launch {
            navigation.navigate(UpdatesFragmentDirections.actionUpdatesFragmentToUpdatesDownloadFragment(release))
        }
    }

    override fun onPAMUpdateClicked(label: String, updateState: UpdateState) {
        val release = updateState.toRelease(label) ?: return
        viewModelScope.launch {
            navigation.navigate(UpdatesFragmentDirections.actionUpdatesFragmentToUpdatesDownloadFragment(release))
        }
    }

    private fun UpdateState.toRelease(label: String): Release? {
        return when(this){
            is UpdateState.UpdateAvailable -> Pair(localVersion, release)
            is UpdateState.NotInstalled -> Pair("", release)
            else -> null
        }?.let {
            it.second.toRelease(label, it.first)
        }
    }

    override fun onShardsViewTracksClicked() {
        viewModelScope.launch {
            rootNavigation.navigate(ContainerFragmentDirections.actionContainerFragmentToTracklistFragment())
        }
    }

    override fun onAutomaticDatabaseUpdatesChanged(enabled: Boolean) {
        viewModelScope.launch {
            automaticDatabaseUpdates.set(enabled)
        }
    }

    override fun onContributorsClicked() {
        viewModelScope.launch {
            navigation.navigate(UpdatesFragmentDirections.actionUpdatesFragmentToContributorsFragment())
        }
    }

    override fun onDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_DONATE.toIntent())
        }
    }

    override fun onGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_GITHUB.toIntent())
        }
    }

    override fun onLibrariesClicked() {
        viewModelScope.launch {
            navigation.navigate(UpdatesFragmentDirections.actionUpdatesFragmentToOssLicensesMenuActivity())
        }
    }

    override fun onTwitterClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_TWITTER.toIntent())
        }
    }

    override fun onXdaClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_XDA.toIntent())
        }
    }

    private fun String.toIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(this@toIntent)
        }
    }

}