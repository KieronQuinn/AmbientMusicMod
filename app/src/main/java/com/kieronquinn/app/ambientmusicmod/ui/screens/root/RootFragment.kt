package com.kieronquinn.app.ambientmusicmod.ui.screens.root

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.setupWithNavigation
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentRootBinding
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivityViewModel
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.firstNotNull
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenCreated
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class RootFragment: BoundFragment<FragmentRootBinding>(FragmentRootBinding::inflate) {

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_root) as NavHostFragment
    }

    private val navigation by inject<RootNavigation>()
    private val activityViewModel by sharedViewModel<MainActivityViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        setupNavigation()
        whenCreated {
            setupStartDestination(
                activityViewModel.startDestination.firstNotNull(), savedInstanceState
            )
        }
    }

    private fun setupStartDestination(id: Int, savedInstanceState: Bundle?) {
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph_root)
        graph.setStartDestination(id)
        navHostFragment.navController.setGraph(graph, savedInstanceState)
    }

    private fun setupNavigation() = whenResumed {
        launch {
            navHostFragment.setupWithNavigation(navigation)
        }
    }

}