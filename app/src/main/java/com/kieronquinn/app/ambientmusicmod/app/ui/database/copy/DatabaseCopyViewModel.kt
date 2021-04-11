package com.kieronquinn.app.ambientmusicmod.app.ui.database.copy

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.LevelDBParser
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.OffsetProvider
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.components.superpacks.Superpacks
import com.kieronquinn.app.ambientmusicmod.components.superpacks.getCoreMatcherFile
import com.kieronquinn.app.ambientmusicmod.components.superpacks.getSuperpacksFileUri
import com.kieronquinn.app.ambientmusicmod.components.superpacks.getTempSuperpacksFile
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.inject
import kotlin.math.roundToInt

abstract class DatabaseCopyViewModel: BaseViewModel() {

    abstract val state: Flow<CopyState>
    abstract val closeBus: Flow<Boolean>

    abstract fun copyStarted()
    abstract fun copyFinished()
    abstract fun cancelClicked()

    abstract fun closeToViewer()

    sealed class CopyState(open val loadingState: LoadingState) {
        object AwaitingStart: CopyState(LoadingState.Indeterminate)
        object Copying: CopyState(LoadingState.Indeterminate)
        object CopyComplete: CopyState(LoadingState.Indeterminate)
        object ParseStart: CopyState(LoadingState.Indeterminate)
        data class ParsingDatabases(val currentDatabase: Int, val databaseCount: Int): CopyState(LoadingState.Progress((currentDatabase / databaseCount.toFloat() * 100).roundToInt()))
        object Done: CopyState(LoadingState.Hidden)
        data class Error(val errorType: ErrorType): CopyState(LoadingState.Hidden)
    }

    sealed class ErrorType(val errorRes: Int) {
        object FailedToStart: ErrorType(R.string.database_viewer_no_response)
        object UnzipFailed: ErrorType(R.string.database_viewer_unzip_error)
        data class ParseFailed(val file: String): ErrorType(R.string.database_viewer_read_offset_error)
    }

    sealed class LoadingState {
        object Hidden: LoadingState()
        object Indeterminate: LoadingState()
        data class Progress(val progress: Int): LoadingState()
    }

}

class DatabaseCopyViewModelImpl(private val context: Context): DatabaseCopyViewModel() {

    companion object {
        private const val BROADCAST_TIMEOUT = 8000L
    }

    private val offsetProvider by inject<OffsetProvider>()

    private val startBus = MutableSharedFlow<Unit>()

    private val _closeBus = MutableSharedFlow<Boolean>()
    override val closeBus: Flow<Boolean> = _closeBus.asSharedFlow()

    private val _state = MutableStateFlow<CopyState>(CopyState.AwaitingStart)
    override val state = _state.asSharedFlow().apply {
        viewModelScope.launch {
            collect {
                when(it){
                    is CopyState.AwaitingStart -> {
                        registerStartReceiver()
                        startCopy()
                    }
                    is CopyState.CopyComplete -> extractSuperpacks()
                    is CopyState.ParseStart -> parseDatabases()
                    is CopyState.Done -> viewModelScope.launch { _closeBus.emit(true) }
                }
            }
        }
    }

    private fun registerStartReceiver() = viewModelScope.launch {
        withTimeoutOrNull(BROADCAST_TIMEOUT) {
            startBus.take(1).collect {
                if(_state.value is CopyState.AwaitingStart) _state.emit(CopyState.Copying)
            }
        } ?: run {
            if(_state.value is CopyState.AwaitingStart) {
                _state.emit(CopyState.Error(ErrorType.FailedToStart))
            }
        }
    }

    private fun startCopy() {
        viewModelScope.launch {
            context.sendSecureBroadcast(Intent(PixelAmbientServices.INTENT_ACTION_SEND_SUPERPACKS).apply {
                putExtra(PixelAmbientServices.INTENT_SEND_SUPERPACKS_EXTRA_OUTPUT_URI, context.getSuperpacksFileUri())
            })
        }
    }

    override fun copyStarted() {
        viewModelScope.launch {
            startBus.emit(Unit)
        }
    }

    override fun copyFinished() {
        viewModelScope.launch {
            _state.emit(CopyState.CopyComplete)
        }
    }

    override fun cancelClicked() {
        viewModelScope.launch {
            sendCancelIntent()
            clearCache()
            navigation.navigate(NavigationEvent.NavigateUp())
        }
    }

    private fun sendCancelIntent(){
        context.sendSecureBroadcast(Intent(PixelAmbientServices.INTENT_ACTION_SEND_SUPERPACKS_CANCEL))
    }

    private fun clearCache(){
        viewModelScope.launch(Dispatchers.IO) {
            context.getTempSuperpacksFile().delete()
        }
    }

    private fun extractSuperpacks(){
        viewModelScope.launch(Dispatchers.IO){
            if(Superpacks.extractSuperpacks(context)){
                _state.emit(CopyState.ParseStart)
            }else {
                _state.emit(CopyState.Error(ErrorType.UnzipFailed))
            }
        }
    }

    private fun parseDatabases(){
        viewModelScope.launch(Dispatchers.IO) {
            val success = Superpacks.forEachSuperpack(context, getCoreMatcherFile()){ file, index, count ->
                val knownOffset = offsetProvider.getOffsetForId(file.name)
                if(knownOffset != null){
                    return@forEachSuperpack true
                }
                _state.emit(CopyState.ParsingDatabases(index, count))
                val offset = LevelDBParser.findMatcherFileEndOffset(file)
                if(offset != null){
                    offsetProvider.setOffsetForId(file.name, offset)
                    true
                }else{
                    _state.emit(CopyState.Error(ErrorType.ParseFailed(file.name)))
                    false
                }
            }
            Log.d("LevelDB", "Parse finished, success $success")
            if(success) {
                _state.emit(CopyState.Done)
            } //Error already handled in forEach
        }
    }

    override fun closeToViewer() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateUp(R.id.databaseViewerFragment))
        }
    }

}