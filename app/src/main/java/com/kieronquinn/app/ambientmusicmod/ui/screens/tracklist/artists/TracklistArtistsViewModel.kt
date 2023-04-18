package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.TracklistNavigation
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.TracklistArtistsViewModel.ShardArtist
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistViewModelImpl
import kotlinx.coroutines.launch

abstract class TracklistArtistsViewModel(
    shardsListRepository: ShardsListRepository
): GenericTracklistViewModelImpl<ShardArtist>(shardsListRepository) {

    data class ShardArtist(val name: String, val tracks: List<ShardTrack>)

    abstract fun onArtistClicked(name: String)

}

class TracklistArtistsViewModelImpl(
    shardsListRepository: ShardsListRepository,
    private val navigation: TracklistNavigation
): TracklistArtistsViewModel(shardsListRepository) {

    companion object {
        private const val SEARCH_TERM_ONDEMAND = "ondemand"
    }

    override suspend fun createList(tracks: List<ShardTrack>): List<ShardArtist> {
        return tracks.groupBy { it.artist }.map { ShardArtist(it.key, it.value) }.sortedBy {
            it.name.lowercase()
        }
    }

    override fun filterList(items: List<ShardArtist>, searchTerm: String): List<ShardArtist> {
        if(searchTerm.isBlank()) return items
        val linearOnly = searchTerm.equals(SEARCH_TERM_ONDEMAND, true)
        return if(linearOnly){
            items.filter {
                it.tracks.any { track -> track.isLinear }
            }.map {
                //Include only the linear tracks so the count is correct
                val linearTracks = it.tracks.filter { track -> track.isLinear }
                it.copy(tracks = linearTracks)
            }
        }else{
            items.filter {
                it.name.lowercase().trim().contains(searchTerm.lowercase().trim())
            }
        }
    }

    override fun onArtistClicked(name: String) {
        viewModelScope.launch {
            navigation.navigate(TracklistArtistsFragmentDirections.actionTracklistArtistsFragmentToTracklistArtistTracksFragment(name))
        }
    }

}