package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentLockscreenCustomTextColourCustomBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom.LockScreenCustomTextColourCustomViewModel.CustomColour.ColourSource
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class LockScreenCustomTextColourCustomFragment: BoundFragment<FragmentLockscreenCustomTextColourCustomBinding>(FragmentLockscreenCustomTextColourCustomBinding::inflate) {

    private val viewModel by viewModel<LockScreenCustomTextColourCustomViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.isNestedScrollingEnabled = false
        setupInsets()
        setupMonet()
        setupInput()
        setupWheel()
        setupApply()
    }

    private fun setupInsets() {
        binding.root.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupMonet() {
        binding.customColourApply.applyMonet()
        binding.customColourInput.applyMonet()
        binding.customColourEdit.applyMonet()
    }

    private fun setupInput() {
        binding.customColourEdit.setText(Integer.toHexString(viewModel.colour.value.colour))
        binding.customColourEdit.filters = arrayOf(hexColorFilter(), InputFilter.LengthFilter(6))
        var isUpdating = false
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.colour.collect {
                val colour = if(it.source == ColourSource.WHEEL || it.source == ColourSource.SETTINGS){
                    it.colour.toHexColor(false)
                }else return@collect
                isUpdating = true
                binding.customColourEdit.run {
                    setText(colour)
                    setSelection(length())
                }
                isUpdating = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.customColourEdit.onChanged { isUpdating }.collect {
                viewModel.setColourFromInput(it?.toString() ?: "")
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.customColourEdit.onTouchUp().collect {
                delay(250L)
                binding.root.scrollToView(binding.customColourInput)
            }
        }
    }

    private fun setupWheel() {
        binding.customColourPicker.setInitialColor(viewModel.colour.value.colour)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.colour.collect {
                val colour = if(it.source == ColourSource.INPUT || it.source == ColourSource.SETTINGS){
                    it.colour
                }else return@collect
                binding.customColourPicker.setColor(colour)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.customColourPicker.onChanged().collect {
                viewModel.setColourFromWheel(it)
            }
        }
    }

    private fun setupApply() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.customColourApply.onClicked().collect {
            viewModel.onApplyClicked()
        }
    }

}