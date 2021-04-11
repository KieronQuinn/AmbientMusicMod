package com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class SettingsAdvancedViewModel: BaseViewModel() {

    val runOnLittleCores = settings.runOnLittleCores
    val useAssistantForClick = settings.useAssistantForClick
    val showAlbumArt = settings.showAlbumArt

    abstract fun onRunOnLittleCoresClicked()
    abstract fun onUseAssistantForClickClicked()
    abstract fun onShowAlbumArtClicked()
    abstract fun onCustomAmplificationClicked()

}

class SettingsAdvancedViewModelImpl: SettingsAdvancedViewModel() {

    override fun onRunOnLittleCoresClicked() {
        viewModelScope.launch {
            settings.sendUpdateIntent()
        }
    }

    override fun onUseAssistantForClickClicked() {
        viewModelScope.launch {
            settings.sendUpdateIntent()
        }
    }

    override fun onCustomAmplificationClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsAdvancedFragmentDirections.actionSettingsAdvancedFragmentToSettingsAdvancedCustomAmplificationBottomSheetFragment()))
        }
    }

    override fun onShowAlbumArtClicked() {
        viewModelScope.launch {
            settings.sendUpdateIntent()
        }
    }

}