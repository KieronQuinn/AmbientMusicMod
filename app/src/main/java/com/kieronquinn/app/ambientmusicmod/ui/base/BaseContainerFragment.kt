package com.kieronquinn.app.ambientmusicmod.ui.base

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.BaseNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.setupWithNavigation
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.app.ambientmusicmod.utils.monetcompat.MonetElevationOverlayProvider
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class BaseContainerFragment<V: ViewBinding>(inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): BoundFragment<V>(inflate) {

    abstract val navigation: BaseNavigation
    abstract val bottomNavigation: BottomNavigationView?
    abstract val collapsingToolbar: CollapsingToolbarLayout
    abstract val appBar: AppBarLayout
    abstract val toolbar: Toolbar
    abstract val fragment: FragmentContainerView
    abstract val navHostFragment: NavHostFragment
    abstract val viewModel: BaseContainerViewModel

    private val googleSansMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    protected val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    private val elevationOverlayProvider by lazy {
        MonetElevationOverlayProvider(requireContext())
    }

    private val headerBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.bottom_nav_elevation)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStack()
        setupCollapsedState()
        setupNavigation()
        setupCollapsingToolbar()
        setupToolbar()
        setupBack()
        setupAppBar()
        bottomNavigation?.let {
            it.setupBottomNavigation()
            NavigationUI.setupWithNavController(it, navController)
        }
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
    }

    private fun BottomNavigationView.setupBottomNavigation() {
        val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            context.getLegacyWorkaroundNavBarHeight()
        } else 0
        onApplyInsets { view, insets ->
            val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
            val bottomInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom + legacyWorkaround
            view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = bottomNavHeight + bottomInsets
            }
            view.updatePadding(bottom = bottomInsets)
        }
        applyMonet(setBackgroundColor = true, md3Style = true)
        setBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(headerBackground, 235))
    }

    @SuppressLint("RestrictedApi")
    private fun setupCollapsingToolbar() = with(collapsingToolbar) {
        setBackgroundColor(monet.getBackgroundColor(requireContext()))
        setContentScrimColor(monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(requireContext()))
        setExpandedTitleTypeface(googleSansMedium)
        setCollapsedTitleTypeface(googleSansMedium)
        lineSpacingMultiplier = 1.1f
    }

    private fun setupStack() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            navController.onDestinationChanged().collect {
                onTopFragmentChanged(
                    navHostFragment.getTopFragment() ?: return@collect, it
                )
            }
        }
        onTopFragmentChanged(
            navHostFragment.getTopFragment() ?: return,
            navController.currentDestination ?: return
        )
    }

    private fun setupCollapsedState() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        appBar.collapsedState().collect {
            navHostFragment.getTopFragment()?.rememberAppBarCollapsed(it)
        }
    }

    private fun setupToolbar() = with(toolbar) {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onNavigationIconClicked().collect {
                (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                (this@BaseContainerFragment as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                viewModel.onBackPressed()
            }
        }
    }

    private fun setupBack() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                if(it.onBackPressed()) return@addCallback
            }
            (this@BaseContainerFragment as? ProvidesBack)?.let {
                if(it.onBackPressed()) return@addCallback
            }
            if(!navController.popBackStack() && !viewModel.onParentBackPressed()) {
                requireActivity().finish()
            }
        }
    }

    private fun setupAppBar() = with(appBar) {
        addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            fragment.updatePadding(bottom = appBarLayout.totalScrollRange + verticalOffset)
        })
    }

    open fun onTopFragmentChanged(topFragment: Fragment, currentDestination: NavDestination){
        val backIcon = if(topFragment is BackAvailable || this is BackAvailable){
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_back)
        } else null
        if(topFragment is ProvidesOverflow){
            setupMenu(topFragment)
        }else{
            setupMenu(null)
        }
        if(topFragment is LockCollapsed || requireContext().isLandscape()) {
            appBar.setExpanded(false)
        }else {
            appBar.setExpanded(!topFragment.getRememberedAppBarCollapsed())
        }
        appBar.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            height = if(topFragment !is NoToolbar){
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            }else 0
        }
        (topFragment as? ProvidesTitle)?.let {
            val label = it.getTitle()
            if(label == null || label.isBlank()) return@let
            collapsingToolbar.title = label
            toolbar.title = label
        } ?: run {
            val label = currentDestination.label
            if(label == null || label.isBlank()) return@run
            collapsingToolbar.title = label
            toolbar.title = label
        }
        toolbar.navigationIcon = backIcon
    }

    private fun setupNavigation() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        launch {
            navHostFragment.setupWithNavigation(navigation)
        }
    }

    private fun setupMenu(menuProvider: ProvidesOverflow?){
        val menu = toolbar.menu
        val menuInflater = MenuInflater(requireContext())
        menu.clear()
        menuProvider?.inflateMenu(menuInflater, menu)
        toolbar.setOnMenuItemClickListener {
            menuProvider?.onMenuItemSelected(it) ?: false
        }
    }

}