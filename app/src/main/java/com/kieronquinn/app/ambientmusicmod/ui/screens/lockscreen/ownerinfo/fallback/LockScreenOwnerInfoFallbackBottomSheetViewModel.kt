package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.fallback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class LockScreenOwnerInfoFallbackBottomSheetViewModel: ViewModel() {

    abstract val ownerInfo: StateFlow<String>

    abstract fun onOwnerInfoChanged(ownerInfo: String)
    abstract fun onSaveClicked()
    abstract fun onCancelClicked()
    abstract fun onResetClicked()

}

class LockScreenOwnerInfoFallbackBottomSheetViewModelImpl(
    settingsRepository: SettingsRepository,
    private val navigation: ContainerNavigation
): LockScreenOwnerInfoFallbackBottomSheetViewModel() {

    private val ownerInfoFallback = settingsRepository.lockscreenOwnerInfoFallback

    override val ownerInfo = MutableStateFlow(ownerInfoFallback.getSync())

    override fun onOwnerInfoChanged(ownerInfo: String) {
        viewModelScope.launch {
            this@LockScreenOwnerInfoFallbackBottomSheetViewModelImpl.ownerInfo.emit(ownerInfo)
        }
    }

    override fun onSaveClicked() {
        viewModelScope.launch {
            ownerInfoFallback.set(ownerInfo.value)
            navigation.navigateBack()
        }
    }

    override fun onCancelClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onResetClicked() {
        viewModelScope.launch {
            ownerInfoFallback.set("")
            navigation.navigateBack()
        }
    }

}