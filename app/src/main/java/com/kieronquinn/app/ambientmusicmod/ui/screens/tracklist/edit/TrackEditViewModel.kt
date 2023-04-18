package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.TracklistNavigation
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TrackEditViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val saveErrorBus: Flow<Unit>

    abstract fun setupWithTrack(track: ShardTrack)
    abstract fun setTrackName(trackName: String)
    abstract fun setArtist(artist: String)

    abstract fun onSaveClicked()
    abstract fun onDeleteClicked()
    abstract fun onBackPressed()

    sealed class State {
        object Loading: State()
        object Saving: State()
        data class Loaded(
            val shardTrack: ShardTrack,
            val trackName: String,
            val artist: String,
            val trackNameError: Boolean,
            val artistError: Boolean
        ): State()
    }

}

class TrackEditViewModelImpl(
    private val shardsListRepository: ShardsListRepository,
    private val navigation: TracklistNavigation
): TrackEditViewModel() {

    private val shardTrack = MutableStateFlow<ShardTrack?>(null)

    private val trackName = MutableStateFlow<String?>(null)
    private val artistName = MutableStateFlow<String?>(null)

    private val trackNameError = MutableStateFlow(false)
    private val artistError = MutableStateFlow(false)

    private val isSaving = MutableStateFlow(false)

    private val track = combine(
        shardTrack,
        trackName
    ) { base, custom ->
        custom ?: base?.trackName
    }

    private val artist = combine(
        shardTrack,
        artistName
    ) { base, custom ->
        custom ?: base?.artist
    }

    private val error = combine(
        trackNameError,
        artistError
    ) { track, artist ->
        Pair(track, artist)
    }

    override val state = combine(
        shardTrack.filterNotNull(),
        track.filterNotNull(),
        artist.filterNotNull(),
        error,
        isSaving
    ) { shard, track, artist, error, saving ->
        if (!saving) {
            State.Loaded(shard, track, artist, error.first, error.second)
        } else {
            State.Saving
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val saveErrorBus = MutableSharedFlow<Unit>()

    override fun setupWithTrack(track: ShardTrack) {
        viewModelScope.launch {
            this@TrackEditViewModelImpl.shardTrack.emit(track)
        }
    }

    override fun setTrackName(trackName: String) {
        viewModelScope.launch {
            this@TrackEditViewModelImpl.trackName.emit(trackName.trim())
            trackNameError.emit(trackName.isBlank())
        }
    }

    override fun setArtist(artist: String) {
        viewModelScope.launch {
            this@TrackEditViewModelImpl.artistName.emit(artist.trim())
            artistError.emit(artist.isBlank())
        }
    }

    override fun onSaveClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            val track = state.trackName
            val artist = state.artist
            var isError = false
            if(track.isBlank()){
                trackNameError.emit(true)
                isError = true
            }
            if(artist.isBlank()){
                artistError.emit(true)
                isError = true
            }
            if(isError) return@launch
            isSaving.emit(true)
            val result = shardsListRepository.updateLinearTrack(
                state.shardTrack,
                track,
                artist
            )
            isSaving.emit(false)
            if(result){
                navigation.navigateBack()
            }else{
                saveErrorBus.emit(Unit)
            }
        }
    }

    override fun onDeleteClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            val shardTrack = state.shardTrack
            isSaving.emit(true)
            val result = shardsListRepository.deleteLinearTrack(shardTrack)
            isSaving.emit(false)
            if(result){
                navigation.navigateBack()
            }else{
                saveErrorBus.emit(Unit)
            }
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}