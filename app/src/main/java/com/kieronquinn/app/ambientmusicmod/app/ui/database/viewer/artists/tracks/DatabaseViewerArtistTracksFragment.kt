package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists.tracks

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentDatabaseViewerArtistTracksBinding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applySystemWindowInsetsPadding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseViewerArtistTracksFragment: BaseFragment<FragmentDatabaseViewerArtistTracksBinding>(FragmentDatabaseViewerArtistTracksBinding::class) {

    private val sharedViewModel by sharedViewModel<DatabaseSharedViewModel>()
    private val viewModel by viewModel<DatabaseViewerArtistTracksViewModel>()

    private val navArgs by navArgs<DatabaseViewerArtistTracksFragmentArgs>()

    private val adapter by lazy {
        DatabaseViewerArtistTracksAdapter(requireContext(), emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenResumed {
            launch {
                viewModel.state.collect {
                    handleState(it)
                }
            }
            launch {
                sharedViewModel.state.collect {
                    if(it is DatabaseSharedViewModel.State.Loaded) viewModel.onMainLoaded(it.tracks, navArgs.artist)
                }
            }
        }
        with(binding.databaseViewerTracksRecyclerView){
            layoutManager = LinearLayoutManager(context)
            adapter = this@DatabaseViewerArtistTracksFragment.adapter
            applySystemWindowInsetsPadding(bottom = true)
        }
    }

    private fun handleState(state: DatabaseViewerArtistTracksViewModel.State) = when(state) {
        is DatabaseViewerArtistTracksViewModel.State.Loading -> {
            binding.databaseViewerLoading.isVisible = true
            binding.databaseViewerTracksRecyclerView.isVisible = false
            binding.databaseViewerTracksEmpty.isVisible = false
        }
        is DatabaseViewerArtistTracksViewModel.State.Loaded -> {
            binding.databaseViewerLoading.isVisible = false
            adapter.tracks = state.tracks
            adapter.notifyDataSetChanged()
            binding.databaseViewerTracksRecyclerView.isVisible = true
            binding.databaseViewerTracksEmpty.isVisible = false
        }
    }

}