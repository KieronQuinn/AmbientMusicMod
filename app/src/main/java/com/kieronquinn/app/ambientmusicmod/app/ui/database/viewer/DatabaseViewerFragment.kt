package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BackProvidingFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.RootFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentDatabaseViewerBinding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class DatabaseViewerFragment: BaseFragment<FragmentDatabaseViewerBinding>(FragmentDatabaseViewerBinding::class), RootFragment, BackProvidingFragment {

    private val sharedViewModel by sharedViewModel<DatabaseSharedViewModel>()
    private val viewModel by viewModel<DatabaseViewerViewModel>()
    private val innerSharedViewModel by sharedViewModel<DatabaseViewerSharedViewModel>()

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_database_viewer) as NavHostFragment
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    private val bottomNavHeight by lazy {
        requireContext().resources.getDimension(R.dimen.bottom_navigation_height)
    }

    private val textListener = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun afterTextChanged(p0: Editable?) {}

        override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
            innerSharedViewModel.setSearchTerm(text?.toString() ?: "")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenResumed {
            launch {
                innerSharedViewModel.loadBus.collect {
                    Log.d("LoadBus", "ping")
                    sharedViewModel.reload()
                }
            }
            launch {
                sharedViewModel.state.collect {
                    handleState(it)
                }
            }
            launch {
                innerSharedViewModel.searchTerm.collect {
                    onSearchTermChanged(it)
                }
            }
            launch {
                viewModel.selectedScreenId.collect {
                    onScreenIdChanged(it)
                }
            }
            launch {
                innerSharedViewModel.navigationBus.collect {
                    navController.handleNavigationEvent(requireContext(), it)
                }
            }
            launch {
                viewModel.shouldShowControls.collect {
                    binding.bottomNavigation.isVisible = it
                    binding.searchContainer.isVisible = it
                }
            }
            launch {
                Log.d("XASuperpacks", "start collect")
                viewModel.shouldShowUpdateBanner.collect {
                    Log.d("XASuperpacks", "update available $it")
                    binding.databaseViewerUpdate.root.isVisible = it
                }
            }
            viewModel.sendSuperpacksUpdateCheckBroadcast()
        }
        binding.searchClear.setOnClickListener {
            innerSharedViewModel.setSearchTerm("")
            binding.searchBox.text.clear()
        }
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            viewModel.setSelectedScreen(it.itemId)
            true
        }
        binding.bottomNavigation.onApplyWindowInsets { view, windowInsetsCompat, viewState ->
            val bottomInset = windowInsetsCompat.getStandardBottomInsets()
            val newHeight = bottomInset + bottomNavHeight.toInt()
            view.updateLayoutParams<ConstraintLayout.LayoutParams> { height = newHeight }
            view.updatePadding(bottom = bottomInset)
            binding.touchBlocker.updateLayoutParams<ConstraintLayout.LayoutParams> { height = newHeight }
        }
        binding.databaseViewerUpdate.root.setOnClickListener {
            viewModel.onUpdateBannerClicked()
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            viewModel.setCurrentDestination(destination.id)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.searchBox.addTextChangedListener(textListener)
    }

    override fun onPause() {
        super.onPause()
        binding.searchBox.removeTextChangedListener(textListener)
    }

    private fun handleState(state: DatabaseSharedViewModel.State) = when(state) {
        is DatabaseSharedViewModel.State.Loading -> {
            binding.navHostFragmentDatabaseViewer.isVisible = false
            binding.databaseViewerLoading.isVisible = true
            binding.databaseViewerLoadingProgressIndeterminate.isVisible = false
            binding.databaseViewerLoadingProgress.progress = state.progress
            binding.searchContainer.isVisible = false
            binding.bottomNavigation.isVisible = false
        }
        is DatabaseSharedViewModel.State.Loaded -> {
            binding.navHostFragmentDatabaseViewer.isVisible = true
            binding.databaseViewerLoading.isVisible = false
            binding.searchContainer.isVisible = true
            binding.bottomNavigation.isVisible = true
            viewModel.onLoaded()
        }
        is DatabaseSharedViewModel.State.Sorting, DatabaseSharedViewModel.State.Idle, DatabaseSharedViewModel.State.StartLoading -> {
            binding.databaseViewerLoading.isVisible = true
            binding.databaseViewerLoadingProgress.visibility = View.INVISIBLE
            binding.databaseViewerLoadingProgressIndeterminate.isVisible = true
            binding.searchContainer.isVisible = false
            binding.bottomNavigation.isVisible = false
        }
        is DatabaseSharedViewModel.State.IncompleteDatabases -> {
            Log.d("DatabaseViewer", "incomplete databases")
            //TODO
        }
    }

    private fun onSearchTermChanged(searchTerm: String){
        binding.searchClear.isVisible = !searchTerm.isNullOrEmpty()
    }

    private fun onScreenIdChanged(id: Int){
        if(id == 0) return
        when(id){
            R.id.menu_tracks -> {
                if(!navController.popBackStack(R.id.databaseViewerTracksFragment, false)) {
                    innerSharedViewModel.navigate(NavigationEvent.NavigateToDestination(R.id.action_global_databaseViewerTracksFragment))
                }
            }
            R.id.menu_artists -> {
                if(!navController.popBackStack(R.id.databaseViewerArtistsFragment, false)) {
                    innerSharedViewModel.navigate(NavigationEvent.NavigateToDestination(R.id.action_global_databaseViewerArtistsFragment))
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if(navHostFragment.isRootFragment) return false
        return navController.navigateUp()
    }

}