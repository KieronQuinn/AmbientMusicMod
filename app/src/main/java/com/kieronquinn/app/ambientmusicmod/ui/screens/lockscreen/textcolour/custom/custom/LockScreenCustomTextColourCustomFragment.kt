package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentLockscreenCustomTextColourCustomBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom.LockScreenCustomTextColourCustomViewModel.CustomColour.ColourSource
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.ambientmusicmod.utils.extensions.hexColorFilter
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onChanged
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onTouchUp
import com.kieronquinn.app.ambientmusicmod.utils.extensions.scrollToView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.setColor
import com.kieronquinn.app.ambientmusicmod.utils.extensions.toHexColor
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.delay
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
        whenResumed {
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
        whenResumed {
            binding.customColourEdit.onChanged { isUpdating }.collect {
                viewModel.setColourFromInput(it?.toString() ?: "")
            }
        }
        whenResumed {
            binding.customColourEdit.onTouchUp().collect {
                delay(250L)
                binding.root.scrollToView(binding.customColourInput)
            }
        }
    }

    private fun setupWheel() {
        binding.customColourPicker.setInitialColor(viewModel.colour.value.colour)
        whenResumed {
            viewModel.colour.collect {
                val colour = if(it.source == ColourSource.INPUT || it.source == ColourSource.SETTINGS){
                    it.colour
                }else return@collect
                binding.customColourPicker.setColor(colour)
            }
        }
        whenResumed {
            binding.customColourPicker.onChanged().collect {
                viewModel.setColourFromWheel(it)
            }
        }
    }

    private fun setupApply() = whenResumed {
        binding.customColourApply.onClicked().collect {
            viewModel.onApplyClicked()
        }
    }

}