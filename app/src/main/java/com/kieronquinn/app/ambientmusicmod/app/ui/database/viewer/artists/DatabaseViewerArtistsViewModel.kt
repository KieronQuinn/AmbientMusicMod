package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.model.database.Artist
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

abstract class DatabaseViewerArtistsViewModel: BaseViewModel() {

    abstract val state: Flow<State>
    abstract val searchTerm: Flow<String>

    abstract fun onMainLoaded(tracks: List<Track>)
    abstract fun onSearchTermChanged(searchTerm: String)

    sealed class State {
        object Loading: State()
        data class Loaded(val artists: List<Artist>): State()
        object Empty: State()
    }

}

class DatabaseViewerArtistsViewModelImpl: DatabaseViewerArtistsViewModel() {

    private val _searchTerm = MutableStateFlow("")
    override val searchTerm = _searchTerm.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Loading)
    override val state = combine(_state, searchTerm){ state, searchTerm ->
        withContext(Dispatchers.IO) {
            if(state is State.Loaded && !searchTerm.isNullOrBlank()) {
                val artists = state.artists.filter {
                    it.name.toLowerCase(Locale.getDefault())
                        .contains(searchTerm.toLowerCase(Locale.getDefault()))
                }.sortedBy { it.name.toLowerCase(Locale.getDefault()).trim() }
                if (artists.isEmpty()) {
                    State.Empty
                } else {
                    State.Loaded(artists)
                }
            }else if(state is State.Loaded){
                val artists = state.artists.sortedBy { it.name.toLowerCase(Locale.getDefault()).trim() }
                State.Loaded(artists)
            }else state
        }
    }

    override fun onMainLoaded(tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            val artists = tracks.map { Artist(it.artist) }.distinctBy { it.name.toLowerCase(Locale.getDefault()) }
            _state.emit(State.Loaded(artists))
        }
    }

    override fun onSearchTermChanged(searchTerm: String) {
        viewModelScope.launch {
            _searchTerm.emit(searchTerm)
        }
    }

}