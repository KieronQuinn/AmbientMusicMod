package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.complete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class SetupCompleteViewModel: ViewModel() {

    abstract fun onCloseClicked()
    abstract fun onBackPressed()

}

class SetupCompleteViewModelImpl(
    private val rootNavigation: RootNavigation,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val remoteSettingsRepository: RemoteSettingsRepository
): SetupCompleteViewModel() {

    override fun onCloseClicked() {
        viewModelScope.launch {
            //Enable the main switch
            remoteSettingsRepository.commitChanges(SettingsStateChange(mainEnabled = true))
            //Give it time to apply before we restart the app
            delay(250L)
            //Restart Now Playing
            shizukuServiceRepository.runWithService { it.forceStopNowPlaying() }
            //And restart Ambient Music Mod
            rootNavigation.phoenix()
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            rootNavigation.finish()
        }
    }

}