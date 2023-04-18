package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.gain

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSettingsAdvancedGainBottomSheetBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onChanged
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class SettingsAdvancedGainBottomSheetFragment: BaseBottomSheetFragment<FragmentSettingsAdvancedGainBottomSheetBinding>(FragmentSettingsAdvancedGainBottomSheetBinding::inflate) {

    private val viewModel by viewModel<SettingsAdvancedGainBottomSheetViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSlider()
        setupPositive()
        setupNegative()
        setupNeutral()
        setupMonet()
        setupInsets(view)
    }

    private fun setupPositive() = whenResumed {
        binding.settingsAdvancedGainPositive.onClicked().collect {
            viewModel.onPositiveClicked()
        }
    }

    private fun setupNegative() = whenResumed {
        binding.settingsAdvancedGainNegative.onClicked().collect {
            viewModel.onNegativeClicked()
        }
    }

    private fun setupNeutral() = whenResumed {
        binding.settingsAdvancedGainNeutral.onClicked().collect {
            viewModel.onNeutralClicked()
        }
    }

    private fun setupMonet() {
        binding.settingsAdvancedGainSlider.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.settingsAdvancedGainPositive.setTextColor(accent)
        binding.settingsAdvancedGainPositive.overrideRippleColor(accent)
        binding.settingsAdvancedGainNegative.setTextColor(accent)
        binding.settingsAdvancedGainNegative.overrideRippleColor(accent)
        binding.settingsAdvancedGainNeutral.setTextColor(accent)
        binding.settingsAdvancedGainNeutral.overrideRippleColor(accent)
    }

    private fun setupInsets(view: View) {
        binding.root.onApplyInsets { _, insets ->
            val bottomPadding = resources.getDimension(R.dimen.margin_16).toInt()
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottomInset + bottomPadding)
        }
    }

    private fun setupSlider() = with(binding.settingsAdvancedGainSlider) {
        setLabelFormatter {
            val value = String.format(Locale.getDefault(), "%.1f", it)
            getString(R.string.settings_advanced_gain_formatter, value)
        }
        handleSliderChange(viewModel.gain.value)
        whenResumed {
            viewModel.gain.collect {
                handleSliderChange(it)
            }
        }
        whenResumed {
            onChanged().collect {
                viewModel.setGain(it)
            }
        }
    }

    private fun handleSliderChange(value: Float) {
        binding.settingsAdvancedGainSlider.value = value
    }

}