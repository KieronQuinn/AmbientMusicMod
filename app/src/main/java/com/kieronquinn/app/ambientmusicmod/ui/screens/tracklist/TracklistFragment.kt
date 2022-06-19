package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist

import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.TracklistNavigation
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentTracklistBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseContainerFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesBack
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TracklistFragment: BaseContainerFragment<FragmentTracklistBinding>(FragmentTracklistBinding::inflate), BackAvailable, ProvidesBack {

    override val viewModel by viewModel<TracklistViewModel>()
    override val navigation by inject<TracklistNavigation>()

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_tracklist) as NavHostFragment
    }

    override val bottomNavigation
        get() = binding.tracklistBottomNavigation
    override val appBar
        get() = binding.tracklistAppBar
    override val toolbar
        get() = binding.tracklistToolbar
    override val collapsingToolbar
        get() = binding.tracklistCollapsingToolbar
    override val fragment
        get() = binding.navHostFragmentTracklist

    override fun onBackPressed(): Boolean {
        return viewModel.onParentBackPressed()
    }

}