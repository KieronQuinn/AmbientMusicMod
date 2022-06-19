package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists

import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.TracklistArtistsViewModel.ShardArtist
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class TracklistArtistsFragment: GenericTracklistFragment<ShardArtist>() {

    override val adapter by lazy {
        TracklistArtistsAdapter(
            binding.tracklistGenericRecyclerview, viewModel::onArtistClicked, emptyList()
        )
    }

    override val viewModel by viewModel<TracklistArtistsViewModel>()

}