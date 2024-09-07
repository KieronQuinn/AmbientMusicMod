package com.kieronquinn.app.ambientmusicmod.ui.base

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
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
import com.kieronquinn.app.ambientmusicmod.utils.extensions.collapsedState
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getLegacyWorkaroundNavBarHeight
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getRememberedAppBarCollapsed
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getTopFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isLandscape
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onDestinationChanged
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.rememberAppBarCollapsed
import com.kieronquinn.app.ambientmusicmod.utils.extensions.setOnBackPressedCallback
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.app.ambientmusicmod.utils.monetcompat.MonetElevationOverlayProvider
import com.kieronquinn.monetcompat.extensions.toArgb
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
        val legacyWorkaround = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
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
        setBackgroundColor(monet.getBackgroundColor(context))
        val color = if(requireContext().isDarkMode){
            monet.getMonetColors().neutral2[800]?.toArgb()
        }else{
            monet.getMonetColors().neutral2[100]?.toArgb()
        } ?: monet.getBackgroundColor(requireContext())
        val indicatorColor = if(requireContext().isDarkMode){
            monet.getMonetColors().accent2[700]?.toArgb()
        }else{
            monet.getMonetColors().accent2[200]?.toArgb()
        }
        setBackgroundColor(ColorUtils.setAlphaComponent(color, 235))
        itemActiveIndicatorColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(indicatorColor ?: Color.TRANSPARENT, Color.TRANSPARENT)
        )
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
        whenResumed {
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

    private fun setupCollapsedState() = whenResumed {
        appBar.collapsedState().collect {
            navHostFragment.getTopFragment()?.rememberAppBarCollapsed(it)
        }
    }

    private fun setupToolbar() = with(toolbar) {
        whenResumed {
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

    @SuppressLint("RestrictedApi")
    private fun setupBack() {
        val callback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@handleOnBackPressed
                }
                (this@BaseContainerFragment as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@handleOnBackPressed
                }
                if(!navController.popBackStack() && !viewModel.onParentBackPressed()) {
                    requireActivity().finish()
                }
            }
        }
        navController.setOnBackPressedCallback(callback)
        navController.enableOnBackPressed(shouldBackDispatcherBeEnabled())
        navController.setOnBackPressedDispatcher(requireActivity().onBackPressedDispatcher)
        whenResumed {
            navController.onDestinationChanged().collect {
                navController.enableOnBackPressed(shouldBackDispatcherBeEnabled())
            }
        }
    }

    private fun shouldBackDispatcherBeEnabled(): Boolean {
        val top = navHostFragment.getTopFragment()
        return top is ProvidesBack
    }

    private fun setupAppBar() = with(appBar) {
        addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            fragment.updatePadding(bottom = appBarLayout.totalScrollRange + verticalOffset)
        }
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

    private fun setupNavigation() = whenResumed {
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