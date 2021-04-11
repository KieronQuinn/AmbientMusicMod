package com.kieronquinn.app.ambientmusicmod.app.ui.container

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.NavigationComponent
import com.kieronquinn.app.ambientmusicmod.components.github.UpdateChecker
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.ScrollableFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentContainerBinding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AmbientContainerFragment: BaseFragment<FragmentContainerBinding>(FragmentContainerBinding::class) {

    private val navigation by inject<NavigationComponent>()
    private val viewModel by viewModel<AmbientContainerViewModel>()
    private val sharedViewModel by sharedViewModel<AmbientContainerSharedViewModel>()

    private val navArgs by navArgs<AmbientContainerFragmentArgs>()
    private val updateChecker by inject<UpdateChecker>()

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    private val googleSansMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_medium)
    }

    private val bottomNavHeight by lazy {
        requireContext().resources.getDimension(R.dimen.bottom_navigation_height)
    }

    private val mainAppLinkMargin by lazy {
        requireContext().resources.getDimension(R.dimen.app_link_margin_bottom).toInt()
    }

    private val fabMargin by lazy {
        requireContext().resources.getDimension(R.dimen.padding_24).toInt()
    }

    private val fabMarginNoSettings by lazy {
        requireContext().resources.getDimension(R.dimen.fab_margin_bottom).toInt()
    }

    private val hideSettings
        get() = navArgs.startDestination == R.id.installerFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph_main).apply {
            startDestination = navArgs.startDestination
        }
        navController.graph = navGraph
        lifecycleScope.launch {
            launch {
                navigation.navigationBus.collect {
                    navController.handleNavigationEvent(requireContext(), it)
                }
            }
            launch {
                viewModel.shouldShowMainAppLink.collect {
                    binding.mainAppLink.root.post {
                        if (it) {
                            binding.mainAppLink.root.setOnClickListener {
                                viewModel.onMainAppLinkClicked()
                            }
                            binding.mainAppLink.root.slideIn {
                                sharedViewModel.setAppLinkHeight(binding.mainAppLink.root.height.toFloat())
                            }
                        } else {
                            binding.mainAppLink.root.setOnClickListener(null)
                            binding.mainAppLink.root.slideOut {
                                sharedViewModel.setAppLinkHeight(0f)
                            }
                        }
                    }
                }
            }
            launch {
                viewModel.currentPage.collect {
                    if(it?.isDialog != true && it?.previousPage?.isDialog != true) {
                        binding.appBar.setExpanded(true)
                        scrollChildToTop()
                    }
                }
            }
            launch {
                sharedViewModel.shouldShowFab.collect {
                    if(it is AmbientContainerSharedViewModel.FabVisibility.Shown){
                        binding.fabBuild.setFabState(it)
                        delay(250L)
                        binding.fabBuild.show()
                    }else{
                        binding.fabBuild.hide()
                    }
                }
            }
            launch {
                viewModel.currentPage.collect {
                    sharedViewModel.setPage(it?.id ?: 0)
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            withContext(Dispatchers.IO){
                updateChecker.clearCachedDownloads(requireContext())
            }
            updateChecker.getLatestRelease().collect {
                if(it != null) viewModel.showUpdateDialog(it)
            }
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val isDialog = destination.label?.isNotBlank() != true

            if(!isDialog) {
                binding.collapsingToolbar.title = destination.label
            }

            viewModel.onPageChanged(AmbientContainerViewModelImpl.Page(destination.id, isDialog))
        }
        with(binding.collapsingToolbar){
            setExpandedTitleTypeface(googleSansMedium)
            setCollapsedTitleTypeface(googleSansMedium)
        }
        with(binding){
            appBar.applySystemWindowInsetsPadding(top = true)
            if(hideSettings){
                bottomNavigation.isVisible = false
                touchBlocker.isVisible = false
                fabBuild.onApplyWindowInsets { view, windowInsetsCompat, viewState ->
                    val bottomInset = windowInsetsCompat.getStandardBottomInsets()
                    fabBuild.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = bottomInset + fabMarginNoSettings }
                }
            }else{
                bottomNavigation.onApplyWindowInsets { view, windowInsetsCompat, viewState ->
                    val bottomInset = windowInsetsCompat.getStandardBottomInsets()
                    val newHeight = bottomInset + bottomNavHeight.toInt()
                    view.updateLayoutParams<CoordinatorLayout.LayoutParams> { height = newHeight }
                    view.updatePadding(bottom = bottomInset)
                    touchBlocker.updateLayoutParams<CoordinatorLayout.LayoutParams> { height = newHeight }
                    mainAppLink.root.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = mainAppLinkMargin + bottomInset }
                    navHostFragment.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = newHeight }
                    fabBuild.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = newHeight + fabMargin }
                }
                bottomNavigation.setOnNavigationItemSelectedListener {
                    when (it.itemId) {
                        R.id.menu_settings -> AmbientContainerViewModel.NavigationTab.SETTINGS
                        R.id.menu_installer -> AmbientContainerViewModel.NavigationTab.INSTALLER
                        else -> null
                    }?.let {
                        viewModel.onBottomNavigationItemSelected(it, navController)
                    }
                    true
                }
            }
            toolbarBack.setOnClickListener {
                navController.navigateUp()
            }
            fabBuild.setOnClickListener {
                sharedViewModel.onFabClicked()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback {
            if(navHostFragment.isRootFragment){
                requireActivity().finish()
            }else {
                navController.navigateUp()
            }
        }
        navHostFragment.childFragmentManager.addOnBackStackChangedListener {
            checkBackStackAndAddBack()
        }
        binding.appBar.setExpanded(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE)
        binding.overflowMenu.setOnClickListener { showMenu(it as ImageButton) }
        updateChecker.getLatestRelease()
    }

    private fun scrollChildToTop(){
        view?.post {
            (navHostFragment.childFragmentManager.fragments.firstOrNull() as? ScrollableFragment)?.scrollToTop()
        }
    }

    private fun checkBackStackAndAddBack(){
        binding.toolbarBack.isVisible = !navHostFragment.isRootFragment
    }

    private fun ExtendedFloatingActionButton.setFabState(fabVisibility: AmbientContainerSharedViewModel.FabVisibility.Shown){
        when(fabVisibility.compatibilityState.toEnum()){
            AmbientContainerSharedViewModel.CompatibilityState.COMPATIBLE, AmbientContainerSharedViewModel.CompatibilityState.NO_XPOSED -> {
                //Blue
                setIconResource(R.drawable.ic_build)
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent))
            }
            AmbientContainerSharedViewModel.CompatibilityState.NOT_COMPATIBLE -> {
                //Yellow
                setIconResource(R.drawable.ic_warning)
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fab_color_warning))
            }
        }
    }

    private fun showMenu(menuButton: ImageButton){
        PopupMenu(menuButton.context, menuButton).apply {
            menuInflater.inflate(R.menu.overflow_menu_main, menu)
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.oss_licenses -> viewModel.onOSSLicencesClicked()
                }
                true
            }
        }.show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getMainAppInstallState()
        checkBackStackAndAddBack()
    }

}