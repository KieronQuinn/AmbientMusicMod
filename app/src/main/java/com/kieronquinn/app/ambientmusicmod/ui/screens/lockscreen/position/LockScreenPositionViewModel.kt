package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.position

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import kotlinx.coroutines.launch

abstract class LockScreenPositionViewModel: ViewModel() {

    abstract val initialYPos: Int

    abstract fun commit(yPos: Int)
    abstract fun onBackPressed()

}

class LockScreenPositionViewModelImpl(
    private val settingsRepository: SettingsRepository,
    private val navigation: RootNavigation
): LockScreenPositionViewModel() {

    override val initialYPos = settingsRepository.lockscreenOverlayYPos.getSync()

    override fun commit(yPos: Int) {
        viewModelScope.launch {
            settingsRepository.lockscreenOverlayYPos.set(yPos)
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}