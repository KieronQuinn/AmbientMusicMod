package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists.tracks

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

abstract class DatabaseViewerArtistTracksViewModel: BaseViewModel() {

    abstract val state: Flow<State>

    abstract fun onMainLoaded(tracks: List<Track>, artist: String)

    sealed class State {
        object Loading: State()
        data class Loaded(val tracks: List<Track>): State()
    }

}

class DatabaseViewerArtistTracksViewModelImpl: DatabaseViewerArtistTracksViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    override val state = _state.asStateFlow()

    override fun onMainLoaded(tracks: List<Track>, artist: String) {
        viewModelScope.launch {
            _state.emit(State.Loaded(tracks.filter { it.artist == artist }))
        }
    }

}