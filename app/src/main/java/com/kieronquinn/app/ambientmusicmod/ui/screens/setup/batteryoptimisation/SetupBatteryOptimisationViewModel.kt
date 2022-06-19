package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.batteryoptimisation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.container.SetupContainerFragmentDirections
import kotlinx.coroutines.launch

abstract class SetupBatteryOptimisationViewModel: ViewModel() {

    abstract fun moveToNext()

}

class SetupBatteryOptimisationViewModelImpl(
    private val rootNavigation: RootNavigation,
    private val settingsRepository: SettingsRepository
): SetupBatteryOptimisationViewModel() {

    override fun moveToNext() {
        viewModelScope.launch {
            settingsRepository.hasSeenSetup.set(true)
            rootNavigation.navigate(SetupContainerFragmentDirections.actionSetupContainerFragmentToSetupCompleteFragment())
        }
    }

}