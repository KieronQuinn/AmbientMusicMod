package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerViewModel
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.RootFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentDatabaseViewerArtistsBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseViewerArtistsFragment: BaseFragment<FragmentDatabaseViewerArtistsBinding>(FragmentDatabaseViewerArtistsBinding::class), RootFragment {

    private val sharedViewModel by sharedViewModel<DatabaseSharedViewModel>()
    private val containerViewModel by sharedViewModel<DatabaseViewerSharedViewModel>()
    private val viewModel by viewModel<DatabaseViewerArtistsViewModel>()

    private val adapter by lazy {
        DatabaseViewerArtistsAdapter(requireContext(), emptyList()){
            containerViewModel.navigate(NavigationEvent.NavigateByDirections(
                DatabaseViewerArtistsFragmentDirections.actionDatabaseViewerArtistsFragmentToDatabaseViewerArtistTracksFragment(it)))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.databaseViewerArtistsRecyclerView){
            layoutManager = LinearLayoutManager(context)
            adapter = this@DatabaseViewerArtistsFragment.adapter
        }
        lifecycleScope.launchWhenResumed {
            launch {
                sharedViewModel.state.collect {
                    if (it is DatabaseSharedViewModel.State.Loaded) viewModel.onMainLoaded(it.tracks)
                }
            }
            launch {
                viewModel.state.collect {
                    handleState(it)
                }
            }
            launch {
                containerViewModel.searchTerm.collect {
                    viewModel.onSearchTermChanged(it)
                }
            }
        }
    }

    private fun handleState(state: DatabaseViewerArtistsViewModel.State) = when(state){
        is DatabaseViewerArtistsViewModel.State.Loading -> {
            binding.databaseViewerLoading.isVisible = true
            binding.databaseViewerArtistsEmpty.isVisible = false
            binding.databaseViewerArtistsRecyclerView.isVisible = false
        }
        is DatabaseViewerArtistsViewModel.State.Loaded -> {
            adapter.artists = state.artists
            adapter.notifyDataSetChanged()
            binding.databaseViewerLoading.isVisible = false
            binding.databaseViewerArtistsEmpty.isVisible = false
            binding.databaseViewerArtistsRecyclerView.isVisible = true
        }
        is DatabaseViewerArtistsViewModel.State.Empty -> {
            binding.databaseViewerArtistsEmpty.isVisible = true
            binding.databaseViewerLoading.isVisible = false
            binding.databaseViewerArtistsRecyclerView.isVisible = false
        }
    }

}