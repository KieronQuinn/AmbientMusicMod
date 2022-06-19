package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks

import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class TracklistTracksFragment: GenericTracklistFragment<ShardTrack>() {

    override val adapter by lazy {
        TracklistTracksAdapter(binding.tracklistGenericRecyclerview, ::onOnDemandClicked, emptyList())
    }

    override val viewModel by viewModel<TracklistTracksViewModel>()

}