package com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced.customamplification

import android.os.Bundle
import android.text.TextWatcher
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.databinding.BsAdvancedCustomAmplificationBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsAdvancedCustomAmplificationBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<SettingsAdvancedCustomAmplificationBottomSheetViewModel>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.show {
        title(R.string.settings_advanced_custom_amplification)
        message(R.string.settings_advanced_custom_amplification_desc)
        customView(R.layout.bs_advanced_custom_amplification)
        val binding = BsAdvancedCustomAmplificationBinding.bind(getCustomView())
        with(binding){
            bsAdvancedCustomAmplificationTextInput.typeface = ResourcesCompat.getFont(context, R.font.google_sans_medium)
            lifecycleScope.launch {
                viewModel.amplification.take(1).collect {
                    bsAdvancedCustomAmplificationTextEdit.text?.apply {
                        clear()
                        append(it?.toString() ?: "")
                    }
                }
                bsAdvancedCustomAmplificationTextEdit.addTextChangedListener {
                    viewModel.setAmplification(it?.toString() ?: "")
                }
                viewModel.errorState.collect {
                    bsAdvancedCustomAmplificationTextInput.isErrorEnabled = it
                    bsAdvancedCustomAmplificationTextInput.error = if(it) getString(R.string.bs_advanced_custom_amplification_error) else null
                    getActionButton(WhichButton.POSITIVE).isEnabled = !it
                }
            }
        }
        positiveButton(android.R.string.ok){
            viewModel.saveAmplification()
        }
        negativeButton(android.R.string.cancel)
    }

}