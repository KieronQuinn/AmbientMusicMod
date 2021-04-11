package com.kieronquinn.app.ambientmusicmod.app.ui.settings.amplification

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.databinding.BsGainSliderBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class SettingsAmplificationBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<SettingsAmplificationBottomSheetViewModel>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.settings_mod_gain)
        message(R.string.settings_mod_gain_desc)
        customView(R.layout.bs_gain_slider)
        val binding = BsGainSliderBinding.bind(getCustomView())
        with(binding.bsGainSlider){
            setLabelFormatter {
                getString(R.string.slider_amplification_label, (it * 100).roundToInt())
            }
            value = viewModel.currentGain
            addOnChangeListener { _, progress, _ ->
                viewModel.setGainByProgress(progress)
            }
        }
        positiveButton(android.R.string.ok){
            viewModel.saveGain()
            it.dismiss()
        }
        negativeButton(android.R.string.cancel){
            it.dismiss()
        }
    }

}