package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.AmbientServiceRepository
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.JobsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsAdvancedViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onGainClicked()
    abstract fun onAlternativeEncodingChanged(enabled: Boolean)
    abstract fun onRunOnLittleCoresChanged(enabled: Boolean)
    abstract fun onSuperpacksRequireWiFiChanged(enabled: Boolean)
    abstract fun onSuperpacksRequireChargingChanged(enabled: Boolean)
    abstract fun onClearAlbumArtClicked(context: Context)
    abstract fun onEnableLoggingChanged(enabled: Boolean)
    abstract fun onExternalAccessClicked()
    abstract fun onPokeJobSchedulerClicked(context: Context)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val alternativeEncoding: Boolean,
            val runOnSmallCores: Boolean,
            val superpacksRequireWifi: Boolean,
            val superpacksRequireCharging: Boolean,
            val enableLogging: Boolean
        ): State()
    }

}

class SettingsAdvancedViewModelImpl(
    private val deviceConfigRepository: DeviceConfigRepository,
    private val serviceRepository: AmbientServiceRepository,
    private val jobsRepository: JobsRepository,
    private val navigation: ContainerNavigation
): SettingsAdvancedViewModel() {

    private val runOnSmallCores = deviceConfigRepository.runOnSmallCores
    private val superpacksRequireWifi = deviceConfigRepository.superpacksRequireWiFi
    private val superpacksRequireCharging = deviceConfigRepository.superpacksRequireCharging
    private val enableLogging = deviceConfigRepository.enableLogging
    private val alternativeEncoding = deviceConfigRepository.alternativeEncoding

    private val superpacksConfig = combine(
        superpacksRequireWifi.asFlow(),
        superpacksRequireCharging.asFlow()
    ) { wifi, charging ->
        Pair(wifi, charging)
    }

    override val state = combine(
        runOnSmallCores.asFlow(),
        superpacksConfig,
        enableLogging.asFlow(),
        alternativeEncoding.asFlow(),
    ) { small, superpacks, logging, alternative ->
        State.Loaded(alternative, small, superpacks.first, superpacks.second, logging)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onGainClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsAdvancedFragmentDirections.actionSettingsAdvancedFragmentToSettingsAdvancedGainBottomSheetFragment())
        }
    }

    override fun onRunOnLittleCoresChanged(enabled: Boolean) {
        viewModelScope.launch {
            runOnSmallCores.set(enabled)
        }
    }

    override fun onSuperpacksRequireWiFiChanged(enabled: Boolean) {
        viewModelScope.launch {
            superpacksRequireWifi.set(enabled)
        }
    }

    override fun onSuperpacksRequireChargingChanged(enabled: Boolean) {
        viewModelScope.launch {
            superpacksRequireCharging.set(enabled)
        }
    }

    override fun onEnableLoggingChanged(enabled: Boolean) {
        viewModelScope.launch {
            enableLogging.set(enabled)
        }
    }

    override fun onExternalAccessClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsAdvancedFragmentDirections.actionSettingsAdvancedFragmentToSettingsAdvancedExternalAccessFragment())
        }
    }

    override fun onAlternativeEncodingChanged(enabled: Boolean) {
        viewModelScope.launch {
            alternativeEncoding.set(enabled)
        }
    }

    override fun onClearAlbumArtClicked(context: Context) {
        viewModelScope.launch {
            serviceRepository.getService()?.clearAlbumArtCache()
            Toast.makeText(
                context,
                R.string.settings_advanced_clear_album_art_cache_toast,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPokeJobSchedulerClicked(context: Context) {
        viewModelScope.launch {
            jobsRepository.forceExpediteJobs()
            Toast.makeText(context, R.string.settings_advanced_poke_jobscheduler_toast, Toast.LENGTH_LONG).show()
        }
    }

}