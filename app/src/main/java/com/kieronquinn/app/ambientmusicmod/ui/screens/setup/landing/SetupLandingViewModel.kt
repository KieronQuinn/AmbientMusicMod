package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import kotlinx.coroutines.launch

abstract class SetupLandingViewModel: ViewModel() {

    abstract fun onGetStartedClicked()

}

class SetupLandingViewModelImpl(
    private val rootNavigation: RootNavigation
): SetupLandingViewModel() {

    override fun onGetStartedClicked() {
        viewModelScope.launch {
            rootNavigation.navigate(SetupLandingFragmentDirections.actionSetupLandingFragmentToSetupContainerFragment())
        }
    }

}