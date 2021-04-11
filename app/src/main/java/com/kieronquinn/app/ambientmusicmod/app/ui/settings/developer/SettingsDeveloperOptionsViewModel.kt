package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class SettingsDeveloperOptionsViewModel: BaseViewModel() {

    abstract val enableLogging: Boolean
    abstract fun onEnableLoggingClicked()
    abstract fun onPhenotypesClicked()
    abstract fun onDumpLogsClicked()

}

class SettingsDeveloperOptionsViewModelImpl: SettingsDeveloperOptionsViewModel() {

    override val enableLogging = settings.developerEnableLogging
    override fun onEnableLoggingClicked() {
        viewModelScope.launch {
            settings.developerEnableLogging = !settings.developerEnableLogging
        }
    }

    override fun onPhenotypesClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsDeveloperOptionsFragmentDirections.actionSettingsDeveloperOptionsFragmentToSettingsDeveloperOptionsPhenotypesFragment()))
        }
    }

    override fun onDumpLogsClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsDeveloperOptionsFragmentDirections.actionSettingsDeveloperOptionsFragmentToSettingsDeveloperOptionsLogFragment()))
        }
    }

}