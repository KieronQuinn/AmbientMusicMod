package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.SetupNavigation
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseContainerViewModel
import kotlinx.coroutines.launch

abstract class SetupContainerViewModel: ViewModel(), BaseContainerViewModel

class SetupContainerViewModelImpl(
    private val navigation: SetupNavigation,
    private val rootNavigation: RootNavigation
): SetupContainerViewModel() {

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onParentBackPressed(): Boolean {
        viewModelScope.launch {
            rootNavigation.navigateBack()
        }
        return true
    }

}