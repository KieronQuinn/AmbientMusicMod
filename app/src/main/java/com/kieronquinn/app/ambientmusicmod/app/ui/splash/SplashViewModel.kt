package com.kieronquinn.app.ambientmusicmod.app.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.constants.MODULE_VERSION_CODE_PROP
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getInt
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

abstract class SplashViewModel: ViewModel() {

    abstract val destination: Flow<Int>
    abstract fun setAnimationCompleted()

}

class SplashViewModelImpl: SplashViewModel() {

    private val animationCompleted = MutableSharedFlow<Unit>()

    override fun setAnimationCompleted() {
        viewModelScope.launch {
            animationCompleted.emit(Unit)
        }
    }

    private val requiredDestination = MutableSharedFlow<Int>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).apply {
        viewModelScope.launch {
            if(SystemProperties_getInt(MODULE_VERSION_CODE_PROP, 0) > 0){
                Log.d("Anim", "destination emit settings")
                emit(R.id.settingsFragment)
            }else{
                Log.d("Anim", "destination emit installer")
                emit(R.id.installerFragment)
            }
        }
    }

    override val destination: Flow<Int> = combine(animationCompleted, requiredDestination){ _, destination ->
        Log.d("Anim", "destination $destination")
        destination
    }

}