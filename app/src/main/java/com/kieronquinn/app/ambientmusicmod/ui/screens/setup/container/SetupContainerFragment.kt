package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.container

import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.SetupNavigation
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupContainerBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseContainerFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesBack
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupContainerFragment: BaseContainerFragment<FragmentSetupContainerBinding>(FragmentSetupContainerBinding::inflate), BackAvailable, ProvidesBack {

    override val viewModel by viewModel<SetupContainerViewModel>()
    override val navigation by inject<SetupNavigation>()

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_setup) as NavHostFragment
    }

    //No bottom navigation during setup
    override val bottomNavigation: BottomNavigationView? = null

    override val appBar
        get() = binding.setupAppBar
    override val toolbar
        get() = binding.setupToolbar
    override val collapsingToolbar
        get() = binding.setupCollapsingToolbar
    override val fragment
        get() = binding.navHostFragmentSetup

    override fun onBackPressed(): Boolean {
        return false
    }
    
}