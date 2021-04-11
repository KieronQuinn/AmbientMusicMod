package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.tracks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerViewModel
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.RootFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentDatabaseViewerTracksBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseViewerTracksFragment: BaseFragment<FragmentDatabaseViewerTracksBinding>(FragmentDatabaseViewerTracksBinding::class), RootFragment {

    private val sharedViewModel by sharedViewModel<DatabaseSharedViewModel>()
    private val containerViewModel by sharedViewModel<DatabaseViewerSharedViewModel>()
    private val viewModel by viewModel<DatabaseViewerTracksViewModel>()

    private val adapter by lazy {
        DatabaseViewerTracksAdapter(requireContext(), emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.databaseViewerTracksRecyclerView){
            layoutManager = LinearLayoutManager(context)
            adapter = this@DatabaseViewerTracksFragment.adapter
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

    private fun handleState(state: DatabaseViewerTracksViewModel.State) = when(state){
        is DatabaseViewerTracksViewModel.State.Loading -> {
            binding.databaseViewerLoading.isVisible = true
            binding.databaseViewerTracksEmpty.isVisible = false
            binding.databaseViewerTracksRecyclerView.isVisible = false
        }
        is DatabaseViewerTracksViewModel.State.Loaded -> {
            adapter.tracks = state.tracks
            adapter.notifyDataSetChanged()
            binding.databaseViewerLoading.isVisible = false
            binding.databaseViewerTracksEmpty.isVisible = false
            binding.databaseViewerTracksRecyclerView.isVisible = true
        }
        is DatabaseViewerTracksViewModel.State.Empty -> {
            binding.databaseViewerTracksEmpty.isVisible = true
            binding.databaseViewerLoading.isVisible = false
            binding.databaseViewerTracksRecyclerView.isVisible = false
        }
    }

}