package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.shizuku

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupShizukuBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.shizuku.SetupShizukuViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupShizukuFragment: BoundFragment<FragmentSetupShizukuBinding>(FragmentSetupShizukuBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupShizukuViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupState()
        setupMonet()
        setupGet()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.setupShizukuGet.run {
            setTextColor(accent)
            iconTint = ColorStateList.valueOf(accent)
            overrideRippleColor(accent)
        }
        binding.setupShizukuLoadingProgress.applyMonet()
    }

    private fun setupGet() = whenResumed {
        binding.setupShizukuGet.onClicked().collect {
            viewModel.onGetShizukuClicked()
        }
    }

    private fun setupInsets() {
        binding.setupShizukuScroll.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            )
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State){
        when(state){
            State.Loading -> {
                binding.setupShizukuLoading.isVisible = true
                binding.setupShizukuError.isVisible = false
            }
            is State.Failed -> {
                binding.setupShizukuLoading.isVisible = false
                binding.setupShizukuError.isVisible = true
                binding.setupShizukuErrorLabel.setText(state.state.title)
                binding.setupShizukuErrorLabelSub.setText(state.state.message)
                binding.setupShizukuGet.setText(state.state.button)
                binding.setupShizukuGet.setIconResource(state.state.buttonIcon)
            }
            State.Success -> {
                viewModel.moveToNext()
            }
        }
    }

}