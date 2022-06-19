package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.artisttracks

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesBack
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesTitle
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class TracklistArtistTracksFragment: GenericTracklistFragment<ShardTrack>(), BackAvailable, ProvidesTitle, ProvidesBack {

    override val adapter by lazy {
        TracklistArtistTracksAdapter(binding.tracklistGenericRecyclerview, ::onOnDemandClicked, emptyList())
    }

    override val viewModel by viewModel<TracklistArtistTracksViewModel>()

    private val args by navArgs<TracklistArtistTracksFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setArtist(args.artist)
    }

    override fun getTitle(): CharSequence {
        return args.artist
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackPressed()
        return true
    }

}