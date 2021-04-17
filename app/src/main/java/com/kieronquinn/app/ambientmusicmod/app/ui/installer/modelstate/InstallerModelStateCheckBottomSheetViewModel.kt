package com.kieronquinn.app.ambientmusicmod.app.ui.installer.modelstate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.IRootService
import com.kieronquinn.app.ambientmusicmod.app.service.AmbientRootService
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.constants.MODEL_UUID
import com.kieronquinn.app.ambientmusicmod.utils.ModuleStateCheck
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTriggerManager
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

abstract class InstallerModelStateCheckBottomSheetViewModel: BaseViewModel() {

    abstract val state: Flow<State>

    abstract fun onOverrideClicked()

    sealed class State {
        object AwaitingService: State()
        data class ServiceConnected(val rootService: IRootService): State()
        data class ResultReceived(val supported: AmbientSharedPreferences.GetModelSupported, val result: Int): State()
    }

}

class InstallerModelStateCheckBottomSheetViewModelImpl(context: Context): InstallerModelStateCheckBottomSheetViewModel() {

    private val intent = Intent(context, AmbientRootService::class.java)

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            setBinder(IRootService.Stub.asInterface(binder))
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            setBinder(null)
        }

    }

    private val _state = MutableSharedFlow<State>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).apply {
        viewModelScope.launch {
            emit(State.AwaitingService)
        }
    }

    override val state = _state.asSharedFlow().apply {
        viewModelScope.launch {
            collect {
                handleState(it)
            }
        }
    }

    override fun onOverrideClicked() {
        viewModelScope.launch {
            settings.getModelSupported = AmbientSharedPreferences.GetModelSupported.SUPPORTED
            settings.getModelLastResult = SoundTriggerManager.STATUS_MANUAL_OVERRIDE
            navigation.navigate(NavigationEvent.NavigateUp())
        }
    }

    private fun setBinder(rootService: IRootService?) {
        viewModelScope.launch {
            if(rootService == null){
                _state.emit(State.AwaitingService)
            }else{
                _state.emit(State.ServiceConnected(rootService))
            }
        }
    }

    private suspend fun handleState(state: State) = when(state) {
        is State.AwaitingService -> {
            withContext(Dispatchers.IO) {
                RootService.bind(intent, serviceConnection)
            }
        }
        is State.ServiceConnected -> {
            var result = withContext(Dispatchers.IO){
                state.rootService.getModelState(ParcelUuid(UUID.fromString(MODEL_UUID)))
            }
            var isResultOk = SoundTriggerManager.isResponseOk(result)

            //The Xposed module messes with the check when enabled, so we'll simply ignore the result if Xposed is running
            if(ModuleStateCheck.isModuleEnabled() && !isResultOk){
                isResultOk = true
                result = SoundTriggerManager.STATUS_XPOSED_OVERRIDE
            }

            val getModelSupported = if(isResultOk) AmbientSharedPreferences.GetModelSupported.SUPPORTED else AmbientSharedPreferences.GetModelSupported.UNSUPPORTED
            settings.getModelSupported = getModelSupported
            settings.getModelLastResult = result
            _state.emit(State.ResultReceived(getModelSupported, result))
        }
        is State.ResultReceived -> {
            // Do nothing
        }
    }

}