package com.kieronquinn.app.ambientmusicmod.ui.activities

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isAppInstalled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

abstract class MainActivityViewModel: ViewModel() {

    abstract val startDestination: StateFlow<Int?>

}

class MainActivityViewModelImpl(
    context: Context,
    settingsRepository: SettingsRepository
): MainActivityViewModel() {

    init {
        AmbientMusicModForegroundService.start(context, true)
    }

    companion object {
        private const val SPLASH_TIMEOUT = 1000L
    }

    private val shouldShowSetup = flow {
        val isPamInstalled = context.packageManager.isAppInstalled(PACKAGE_NAME_PAM)
        val hasSeenSetup = settingsRepository.hasSeenSetup.get()
        emit(!isPamInstalled || !hasSeenSetup)
    }

    private val splashTimeout = flow {
        delay(SPLASH_TIMEOUT)
        emit(Unit)
    }

    override val startDestination = combine(shouldShowSetup, splashTimeout) { showSetup, _ ->
        if(showSetup){
            R.id.setupLandingFragment
        }else{
            R.id.containerFragment
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

}