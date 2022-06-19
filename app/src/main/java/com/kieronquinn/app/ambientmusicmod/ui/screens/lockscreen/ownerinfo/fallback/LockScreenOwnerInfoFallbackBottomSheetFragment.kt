package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.fallback

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentLockscreenOwnerInfoFallbackBottomSheetBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onChanged
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LockScreenOwnerInfoFallbackBottomSheetFragment: BaseBottomSheetFragment<FragmentLockscreenOwnerInfoFallbackBottomSheetBinding>(FragmentLockscreenOwnerInfoFallbackBottomSheetBinding::inflate) {

    private val viewModel by viewModel<LockScreenOwnerInfoFallbackBottomSheetViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInput()
        setupSave()
        setupCancel()
        setupReset()
        setupMonet()
    }

    private fun setupInput() = with(binding.lockscreenOwnerInfoFallbackEdit) {
        setText(viewModel.ownerInfo.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onChanged().collect { viewModel.onOwnerInfoChanged(it?.toString() ?: "") }
        }
    }

    private fun setupSave() = viewLifecycleOwner.lifecycleScope.launch {
        binding.lockscreenOwnerInfoFallbackPositive.onClicked().collect {
            viewModel.onSaveClicked()
        }
    }

    private fun setupCancel() = viewLifecycleOwner.lifecycleScope.launch {
        binding.lockscreenOwnerInfoFallbackNegative.onClicked().collect {
            viewModel.onCancelClicked()
        }
    }

    private fun setupReset() = viewLifecycleOwner.lifecycleScope.launch {
        binding.lockscreenOwnerInfoFallbackNeutral.onClicked().collect {
            viewModel.onResetClicked()
        }
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.lockscreenOwnerInfoFallbackPositive.setTextColor(accent)
        binding.lockscreenOwnerInfoFallbackPositive.overrideRippleColor(accent)
        binding.lockscreenOwnerInfoFallbackNegative.setTextColor(accent)
        binding.lockscreenOwnerInfoFallbackNegative.overrideRippleColor(accent)
        binding.lockscreenOwnerInfoFallbackNeutral.setTextColor(accent)
        binding.lockscreenOwnerInfoFallbackNeutral.overrideRippleColor(accent)
        binding.lockscreenOwnerInfoFallbackInput.applyMonet()
        binding.lockscreenOwnerInfoFallbackEdit.applyMonet()
    }

}