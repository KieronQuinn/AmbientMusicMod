package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.tracks

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

abstract class DatabaseViewerTracksViewModel: BaseViewModel() {

    abstract val state: Flow<State>
    abstract val searchTerm: Flow<String>

    abstract fun onMainLoaded(tracks: List<Track>)
    abstract fun onSearchTermChanged(searchTerm: String)

    sealed class State {
        object Loading: State()
        data class Loaded(val tracks: List<Track>): State()
        object Empty: State()
    }

}

class DatabaseViewerTracksViewModelImpl: DatabaseViewerTracksViewModel() {

    private val _searchTerm = MutableStateFlow("")
    override val searchTerm = _searchTerm.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Loading)
    override val state = combine(_state, searchTerm){ state, searchTerm ->
        withContext(Dispatchers.IO) {
            if(state is State.Loaded && !searchTerm.isNullOrBlank()){
                val tracks = state.tracks.filter { it.track.toLowerCase(Locale.getDefault()).contains(searchTerm.toLowerCase(Locale.getDefault())) }
                if(tracks.isEmpty()){
                    State.Empty
                }else{
                    State.Loaded(tracks)
                }
            }else state
        }
    }

    override fun onMainLoaded(tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.emit(State.Loaded(tracks))
        }
    }

    override fun onSearchTermChanged(searchTerm: String) {
        viewModelScope.launch {
            _searchTerm.emit(searchTerm)
        }
    }

}
