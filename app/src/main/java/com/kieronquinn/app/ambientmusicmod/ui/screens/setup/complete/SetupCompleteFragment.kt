package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.complete

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupCompleteBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesBack
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.replaceColour
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupCompleteFragment: BoundFragment<FragmentSetupCompleteBinding>(FragmentSetupCompleteBinding::inflate), BackAvailable, ProvidesBack {

    private val viewModel by viewModel<SetupCompleteViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupLottie()
        setupClose()
    }

    private fun setupMonet() {
        val background = monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        binding.setupCompleteCard.backgroundTintList = ColorStateList.valueOf(background)
        val accent = monet.getAccentColor(requireContext())
        binding.setupCompleteClose.setTextColor(accent)
        binding.setupCompleteClose.overrideRippleColor(accent)
    }

    private fun setupLottie() = with(binding.setupCompleteLottie) {
        val accent = monet.getAccentColor(requireContext(), false)
        replaceColour("Background Circle (Blue)", "**", replaceWith = accent)
        replaceColour("Background(Blue)", "**", replaceWith = accent)
        playAnimation()
    }

    private fun setupClose() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.setupCompleteClose.onClicked().collect {
            viewModel.onCloseClicked()
        }
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackPressed()
        return true
    }

}