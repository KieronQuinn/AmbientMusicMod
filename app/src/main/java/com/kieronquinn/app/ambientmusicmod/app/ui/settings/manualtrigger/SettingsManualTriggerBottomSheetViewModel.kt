package com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.troubleshooting.SettingsManualTriggerTroubleshootingBottomSheetFragment
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.model.recognition.RecognitionResult
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getTempInputFile
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getTempInputFileUri
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

abstract class SettingsManualTriggerBottomSheetViewModel: BaseViewModel() {

    abstract val state: Flow<State>

    abstract fun onStartReceived()
    abstract fun onResultReceived(intent: Intent?)
    abstract fun onPlaybackClicked()
    abstract fun onTroubleshootingClicked(troubleshootingType: SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType)
    abstract fun deleteCachedInput()

    sealed class State {
        object AwaitingStart: State()
        object Started: State()
        object Running: State()
        object AwaitingResult: State()
        data class Complete(val recognitionResult: RecognitionResult, val outputExists: Boolean): State()
        data class Error(val errorType: ErrorType): State()
    }

    enum class ErrorType {
        FAILED_TO_START,
        NO_RESPONSE,
        BAD_RESPONSE
    }

}

fun SettingsManualTriggerBottomSheetViewModel.ErrorType.toTroubleshootingType(): SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType {
    return when(this){
        SettingsManualTriggerBottomSheetViewModel.ErrorType.NO_RESPONSE -> SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType.TYPE_NO_RESULT
        SettingsManualTriggerBottomSheetViewModel.ErrorType.BAD_RESPONSE -> SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType.TYPE_UNKNOWN
        SettingsManualTriggerBottomSheetViewModel.ErrorType.FAILED_TO_START -> SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType.TYPE_NOT_STARTED
    }
}

class SettingsManualTriggerBottomSheetViewModelImpl(private val context: Context): SettingsManualTriggerBottomSheetViewModel() {

    companion object {
        private const val TIMEOUT = 8000L
        private const val RECOGNITION_RUNTIME = 8000L
    }

    private val tempInputFile = context.getTempInputFile()

    private val _state: MutableStateFlow<State> = MutableStateFlow<State>(State.AwaitingStart).apply {
        viewModelScope.launch {
            collect {
                when(it){
                    is State.AwaitingStart -> {
                        registerStartReceiver()
                        sendGetModelStateBroadcast()
                    }
                    is State.Started -> {
                        Log.d("ModelResponse", "Started")
                        registerFinishReceiver()
                        emit(State.Running)
                        delay(RECOGNITION_RUNTIME)
                        if(!isRunning) return@collect
                        emit(State.AwaitingResult)
                    }
                }
            }
        }
    }

    private val isRunning: Boolean
        get() = _state.value is State.Running || _state.value is State.AwaitingResult || _state.value is State.AwaitingStart || _state.value is State.Started

    override val state: Flow<State> = _state.asStateFlow()

    private val startBus = MutableSharedFlow<Unit>()
    private val resultBus = MutableSharedFlow<RecognitionResult?>()

    private fun registerStartReceiver() = viewModelScope.launch {
        withTimeoutOrNull(TIMEOUT) {
            startBus.take(1).collect {
                Log.d("ModelResponse", "Start bus ${_state.value}")
                if(_state.value is State.AwaitingStart) _state.emit(State.Started)
            }
        } ?: _state.emit(State.Error(ErrorType.FAILED_TO_START))
    }

    private fun registerFinishReceiver() = viewModelScope.launch {
        withTimeoutOrNull(RECOGNITION_RUNTIME + TIMEOUT) {
            resultBus.take(1).collect { recognitionResult ->
                if(!isRunning) return@collect
                if(recognitionResult != null){
                    //Check if input dump file exists as well
                    _state.emit(State.Complete(recognitionResult, tempInputFile.exists()))
                }else{
                    _state.emit(State.Error(ErrorType.BAD_RESPONSE))
                }
            }
        } ?: run {
            Log.d("ModelResponse", "Response timeout ${isRunning}")
            if(!isRunning) return@run
            _state.emit(State.Error(ErrorType.NO_RESPONSE))
        }
    }

    private fun sendGetModelStateBroadcast() {
        Log.d("ModelResponse", "Sending get state broadcast")
        context.sendSecureBroadcast(Intent(PixelAmbientServices.INTENT_ACTION_GET_MODEL_STATE_MANUAL).apply {
            putExtra(PixelAmbientServices.INTENT_GET_MODEL_STATE_EXTRA_OUTPUT_URI, context.getTempInputFileUri())
            `package` = PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
        })
    }

    override fun onStartReceived() {
        viewModelScope.launch {
            startBus.emit(Unit)
        }
    }

    override fun onResultReceived(intent: Intent?) {
        viewModelScope.launch {
            val recognitionResult = intent?.getParcelableExtra<RecognitionResult>(PixelAmbientServices.INTENT_RECOGNITION_RESULT_EXTRA_RESULT)
            resultBus.emit(recognitionResult)
        }
    }

    override fun onPlaybackClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsManualTriggerBottomSheetFragmentDirections.actionSettingsManualTriggerBottomSheetFragmentToSettingsManualTriggerPlaybackBottomSheetFragment()))
        }
    }

    override fun onTroubleshootingClicked(troubleshootingType: SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType) {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(
                SettingsManualTriggerBottomSheetFragmentDirections.actionSettingsManualTriggerBottomSheetFragmentToSettingsManualTriggerTroubleshootingBottomSheetFragment(
                    troubleshootingType)))
        }
    }

    override fun deleteCachedInput() {
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                if(tempInputFile.exists()) tempInputFile.delete()
            }
        }
    }


}