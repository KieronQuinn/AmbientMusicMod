package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class SettingsDeveloperOptionsLogViewModel: BaseViewModel() {

    abstract fun onContentsClicked()
    abstract fun onStartDumpClicked()

}

class SettingsDeveloperOptionsLogViewModelImpl: SettingsDeveloperOptionsLogViewModel() {

    override fun onContentsClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsDeveloperOptionsLogFragmentDirections.actionSettingsDeveloperOptionsLogFragmentToSettingsDeveloperOptionsLogContentsBottomSheetFragment()))
        }
    }

    override fun onStartDumpClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsDeveloperOptionsLogFragmentDirections.actionSettingsDeveloperOptionsLogFragmentToSettingsDeveloperOptionsDumpLogBottomSheetFragment()))
        }
    }

}