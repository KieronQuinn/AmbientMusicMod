package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.landing

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupLandingBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.Root
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupLandingFragment: BoundFragment<FragmentSetupLandingBinding>(FragmentSetupLandingBinding::inflate), Root {

    private val viewModel by viewModel<SetupLandingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyMonet()
        setupGetStarted()
    }

    private fun applyMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.setupLandingGetStarted.run {
            setTextColor(accent)
            overrideRippleColor(accent)
        }
    }

    private fun setupGetStarted() {
        whenResumed {
            binding.setupLandingGetStarted.onClicked().collect {
                viewModel.onGetStartedClicked()
            }
        }
    }

}