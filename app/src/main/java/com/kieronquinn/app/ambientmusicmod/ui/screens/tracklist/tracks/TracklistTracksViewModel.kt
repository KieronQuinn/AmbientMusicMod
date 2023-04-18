package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks

import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.TracklistNavigation
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistViewModelImpl
import kotlinx.coroutines.launch

abstract class TracklistTracksViewModel(
    shardsListRepository: ShardsListRepository
): GenericTracklistViewModelImpl<ShardTrack>(shardsListRepository) {

    abstract fun onTrackClicked(track: ShardTrack)

}

class TracklistTracksViewModelImpl(
    private val tracklistNavigation: TracklistNavigation,
    shardsListRepository: ShardsListRepository
): TracklistTracksViewModel(shardsListRepository) {

    companion object {
        private const val SEARCH_TERM_ONDEMAND = "ondemand"
    }

    override suspend fun createList(tracks: List<ShardTrack>): List<ShardTrack> {
        return tracks.sortedBy { it.trackName.lowercase() }
    }

    override fun filterList(items: List<ShardTrack>, searchTerm: String): List<ShardTrack> {
        if(searchTerm.isBlank()) return items
        val linearOnly = searchTerm.equals(SEARCH_TERM_ONDEMAND, true)
        return if(linearOnly) {
            items.filter { it.isLinear }
        }else{
            items.filter {
                it.trackName.lowercase().trim().contains(searchTerm.lowercase().trim())
                        || it.album?.lowercase()?.contains(searchTerm.lowercase().trim()) ?: false
            }
        }
    }

    override fun onTrackClicked(track: ShardTrack) {
        viewModelScope.launch {
            //Collisions mean we have to form this manually
            tracklistNavigation.navigate(
                R.id.action_tracklistTracksFragment_to_trackInfoBottomSheetFragment2,
                bundleOf(
                    "track" to track,
                    "from_artists" to false
                )
            )
        }
    }

}