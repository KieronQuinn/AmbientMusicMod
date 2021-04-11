package com.kieronquinn.app.ambientmusicmod.app.ui.database

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.LevelDBParser
import com.kieronquinn.app.ambientmusicmod.components.OffsetProvider
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.components.superpacks.Superpacks
import com.kieronquinn.app.ambientmusicmod.components.superpacks.getCoreMatcherFile
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import com.kieronquinn.app.ambientmusicmod.utils.extensions.broadcastReceiverFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

abstract class DatabaseSharedViewModel: BaseViewModel() {

    abstract val state: Flow<State>

    abstract fun reload(force: Boolean = false)

    sealed class State {
        object Idle: State()
        object StartLoading: State()
        data class Loading(val progress: Int): State()
        object Sorting: State()
        data class Loaded(val tracks: List<Track>): State()
        object IncompleteDatabases: State()
    }
    
}

class DatabaseSharedViewModelImpl(private val context: Context): DatabaseSharedViewModel() {

    private val offsetProvider by inject<OffsetProvider>()

    private val _state = MutableStateFlow<State>(State.Idle)
    override val state = _state.asStateFlow().apply {
        viewModelScope.launch {
            collect {
                if(it is State.StartLoading) loadTracks()
            }
        }
    }

    private fun loadTracks() = viewModelScope.launch(Dispatchers.IO) {
        Log.d("LoadBus", "loadTracks")
        val tracks = ArrayList<Track>()
        val startTime = System.currentTimeMillis()
        val success = Superpacks.forEachSuperpack(context, getCoreMatcherFile()){ file, index, count ->
            _state.emit(State.Loading((index / count.toFloat() * 100).roundToInt()))
            val offset = offsetProvider.getOffsetForId(file.name) ?: return@forEachSuperpack false
            tracks.addAll(LevelDBParser.parseMatcherFile(file, offset))
            true
        }
        if(success){
            _state.emit(State.Sorting)
            val sortedTracks = tracks.distinctBy { it.formattedKey }.sortedBy { it.track.toLowerCase(Locale.getDefault()) }
            _state.emit(State.Loaded(sortedTracks))
            Log.d("LevelDB", "Found ${sortedTracks.size} distinct tracks in ${System.currentTimeMillis() - startTime}ms")
        }
        else _state.emit(State.IncompleteDatabases)
    }

    override fun reload(force: Boolean) {
        viewModelScope.launch {
            if(_state.value !is State.Loaded || force) {
                _state.emit(State.StartLoading)
            }
        }
    }
    
}