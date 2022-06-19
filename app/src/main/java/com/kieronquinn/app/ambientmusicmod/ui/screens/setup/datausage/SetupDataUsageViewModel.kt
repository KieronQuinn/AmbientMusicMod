package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.datausage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.SetupNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getNetworkCapabilities
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isCharging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SetupDataUsageViewModel: ViewModel() {

    abstract val fabState: StateFlow<FabState?>
    abstract val state: StateFlow<State>

    abstract fun onBackPressed()
    abstract fun setSuperpacksRequireCharging(enabled: Boolean)
    abstract fun setSuperpacksRequireWiFi(enabled: Boolean)
    abstract fun onNextClicked()

    enum class FabState(val warning: Int?) {
        NEXT(null),
        CONNECT_CHARGER(R.string.setup_data_usage_warning_charging),
        CONNECT_NETWORK(R.string.setup_data_usage_warning_network),
        CONNECT_NETWORK_UNMETERED(R.string.setup_data_usage_warning_network_unmetered)
    }

    sealed class State {
        object Loading: State()

        data class Loaded(
            val superpacksRequireCharging: Boolean, val superpacksRequireWiFi: Boolean
        ): State()
    }

}

class SetupDataUsageViewModelImpl(
    context: Context,
    deviceConfigRepository: DeviceConfigRepository,
    private val navigation: SetupNavigation,
    private val rootNavigation: RootNavigation
): SetupDataUsageViewModel() {

    private val superpacksRequireCharging = deviceConfigRepository.superpacksRequireCharging
    private val superpacksRequireWiFi = deviceConfigRepository.superpacksRequireWiFi

    override val fabState = combine(
        superpacksRequireCharging.asFlow(),
        superpacksRequireWiFi.asFlow(),
        context.isCharging(),
        context.getNetworkCapabilities()
    ) { requireCharging, requireWifi, charging, networkCapabilities ->
        when {
            requireCharging && !charging -> FabState.CONNECT_CHARGER
            requireWifi && !networkCapabilities.unmetered -> FabState.CONNECT_NETWORK_UNMETERED
            !networkCapabilities.hasInternet -> FabState.CONNECT_NETWORK
            else -> FabState.NEXT
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val state = combine(
        superpacksRequireCharging.asFlow(),
        superpacksRequireWiFi.asFlow()
    ) { charging, wifi ->
        State.Loaded(charging, wifi)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun setSuperpacksRequireCharging(enabled: Boolean) {
        viewModelScope.launch {
            superpacksRequireCharging.set(enabled)
        }
    }

    override fun setSuperpacksRequireWiFi(enabled: Boolean) {
        viewModelScope.launch {
            superpacksRequireWiFi.set(enabled)
        }
    }

    override fun onNextClicked() {
        viewModelScope.launch {
            navigation.navigate(SetupDataUsageFragmentDirections.actionSetupDataUsageFragmentToSetupCountryPickerFragment())
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            rootNavigation.navigateBack()
        }
    }

}

