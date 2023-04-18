package com.kieronquinn.app.ambientmusicmod.ui.screens.container

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentContainerBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseContainerFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContainerFragment: BaseContainerFragment<FragmentContainerBinding>(FragmentContainerBinding::inflate) {

    override val viewModel by viewModel<ContainerViewModel>()
    override val navigation by inject<ContainerNavigation>()

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    override val bottomNavigation
        get() = binding.containerBottomNavigation
    override val appBar
        get() = binding.containerAppBar
    override val toolbar
        get() = binding.containerToolbar
    override val collapsingToolbar
        get() = binding.containerCollapsingToolbar
    override val fragment
        get() = binding.navHostFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUpdateBadge()
    }

    private fun setupUpdateBadge() {
        handleUpdateBadge(viewModel.updateAvailable.value)
        whenResumed {
            viewModel.updateAvailable.collect {
                handleUpdateBadge(it)
            }
        }
    }

    private fun handleUpdateBadge(isShown: Boolean) {
        bottomNavigation.getOrCreateBadge(R.id.nav_graph_updates).run {
            isVisible = isShown
        }
    }

}