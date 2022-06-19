package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository.GetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

abstract class GenericTracklistViewModel<T>: ViewModel() {

    abstract val state: StateFlow<State<T>>

    sealed class State<T> {
        data class Loading<T>(val progress: Int, val indeterminate: Boolean): State<T>()
        data class Loaded<T>(val list: List<T>): State<T>()
    }

    abstract suspend fun createList(tracks: List<ShardTrack>): List<T>
    abstract fun filterList(items: List<T>, searchTerm: String): List<T>

    abstract val searchText: StateFlow<String>
    abstract val searchShowClear: StateFlow<Boolean>
    abstract fun setSearchText(text: CharSequence)

}

abstract class GenericTracklistViewModelImpl<T>(
    shardsListRepository: ShardsListRepository
): GenericTracklistViewModel<T>() {

    override val searchText = MutableStateFlow("")

    private val tracks = shardsListRepository.tracks.mapLatest {
        when(it){
            is GetState.Querying -> State.Loading(0, true)
            is GetState.Loading -> State.Loading(it.getProgress(), false)
            is GetState.Merging -> State.Loading(100, true)
            is GetState.Loaded -> State.Loaded(createList(it.tracks))
        }
    }.flowOn(Dispatchers.IO)

    override val state by lazy {
        combine(tracks, searchText) { tracks, searchText ->
            when(tracks){
                is State.Loaded -> State.Loaded(filterList(tracks.list, searchText))
                else -> tracks
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading(0, true))
    }

    override val searchShowClear = searchText.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun GetState.Loading.getProgress(): Int {
        return ((current / total.toFloat()) * 100).roundToInt()
    }

    override fun setSearchText(text: CharSequence) {
        viewModelScope.launch {
            searchText.emit(text.toString())
        }
    }

}