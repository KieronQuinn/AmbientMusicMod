package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.AmbientServiceRepository
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
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
    abstract fun onNnfpv3Changed(enabled: Boolean)
    abstract fun onSuperpacksRequireWiFiChanged(enabled: Boolean)
    abstract fun onSuperpacksRequireChargingChanged(enabled: Boolean)
    abstract fun onClearAlbumArtClicked(context: Context)
    abstract fun onEnableLoggingChanged(enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val alternativeEncoding: Boolean,
            val runOnSmallCores: Boolean,
            val nnfpv3: Boolean,
            val superpacksRequireWifi: Boolean,
            val superpacksRequireCharging: Boolean,
            val enableLogging: Boolean
        ): State()
    }

}

class SettingsAdvancedViewModelImpl(
    deviceConfigRepository: DeviceConfigRepository,
    private val serviceRepository: AmbientServiceRepository,
    private val navigation: ContainerNavigation
): SettingsAdvancedViewModel() {

    private val runOnSmallCores = deviceConfigRepository.runOnSmallCores
    private val nnfpv3 = deviceConfigRepository.nnfpv3Enabled
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
        nnfpv3.asFlow(),
        superpacksConfig,
        enableLogging.asFlow(),
        alternativeEncoding.asFlow(),
    ) { small, nnfpv3, superpacks, logging, alternative ->
        State.Loaded(alternative, small, nnfpv3, superpacks.first, superpacks.second, logging)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onGainClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsAdvancedFragmentDirections.actionSettingsAdvancedFragmentToSettingsAdvancedGainBottomSheetFragment())
        }
    }

    override fun onNnfpv3Changed(enabled: Boolean) {
        viewModelScope.launch {
            nnfpv3.set(enabled)
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

}