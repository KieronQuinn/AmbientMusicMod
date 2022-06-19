package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks

import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistViewModelImpl

abstract class TracklistTracksViewModel(
    shardsListRepository: ShardsListRepository
): GenericTracklistViewModelImpl<ShardTrack>(shardsListRepository)

class TracklistTracksViewModelImpl(
    shardsListRepository: ShardsListRepository
): TracklistTracksViewModel(shardsListRepository) {

    override suspend fun createList(tracks: List<ShardTrack>): List<ShardTrack> {
        return tracks.sortedBy { it.trackName.lowercase() }
    }

    override fun filterList(items: List<ShardTrack>, searchTerm: String): List<ShardTrack> {
        if(searchTerm.isBlank()) return items
        return items.filter {
            it.artist.lowercase().trim().contains(searchTerm.lowercase().trim()) ||
                    it.trackName.lowercase().trim().contains(searchTerm.lowercase().trim())
        }
    }

}