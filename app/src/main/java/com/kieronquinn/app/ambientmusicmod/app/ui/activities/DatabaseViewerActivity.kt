package com.kieronquinn.app.ambientmusicmod.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.NavigationComponent
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BackProvidingFragment
import com.kieronquinn.app.ambientmusicmod.databinding.ActivityDatabaseViewerBinding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applySystemWindowInsetsPadding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.handleNavigationEvent
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isRootFragment
import dev.chrisbanes.insetter.Insetter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DatabaseViewerActivity: AppCompatActivity() {

    private val navigation by inject<NavigationComponent>()

    private val navHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    private val backIcon by lazy {
        ContextCompat.getDrawable(this, R.drawable.ic_back)
    }

    private lateinit var binding: ActivityDatabaseViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Insetter.setEdgeToEdgeSystemUiFlags(window.decorView, true)
        binding = ActivityDatabaseViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.applySystemWindowInsetsPadding(top = true)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            binding.toolbarTitle.text = destination.label
        }
        lifecycleScope.launchWhenCreated {
            navigation.navigationBus.collect {
                navController.handleNavigationEvent(this@DatabaseViewerActivity, it)
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            handleBack()
        }
        binding.toolbar.navigationIcon = backIcon
    }

    private fun handleBack(){
        val childFragment = navHostFragment.childFragmentManager.fragments.first()
        if(childFragment is BackProvidingFragment){
            if(childFragment.onBackPressed()) return
        }
        when {
            navHostFragment.isRootFragment -> {
                finish()
                ActivityNavigator.applyPopAnimationsToPendingTransition(this)
            }
            else -> {
                navController.navigateUp()
            }
        }
    }

    override fun onBackPressed() {
        handleBack()
    }

}