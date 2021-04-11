package com.kieronquinn.app.ambientmusicmod.app.ui.splash

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashFragment: BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::class) {

    private val viewModel by viewModel<SplashViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.avdSplashBg.clipToOutline = true
        lifecycleScope.launch {
            viewModel.destination.collect {
                navigateToPage(it)
            }
        }
        lifecycleScope.launchWhenResumed {
            (binding.avdSplashBg.drawable as AnimatedVectorDrawable).start()
            delay(600)
            binding.avdSplashFg.run {
                visibility = View.VISIBLE
                (drawable as AnimatedVectorDrawable).start()
            }
            delay(1000)
            viewModel.setAnimationCompleted()
        }
    }

    private fun navigateToPage(page: Int){
        try {
            findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToAmbientContainerFragment(page))
        }catch (e: IllegalArgumentException){
            //Destination unknown
        }
    }

}