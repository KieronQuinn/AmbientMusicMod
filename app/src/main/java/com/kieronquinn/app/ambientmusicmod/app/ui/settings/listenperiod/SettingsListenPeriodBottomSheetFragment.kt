package com.kieronquinn.app.ambientmusicmod.app.ui.settings.listenperiod

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.service.GetModelStateForegroundService
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsListenPeriodBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<SettingsListenPeriodBottomSheetViewModel>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.settings_mod_job)
        listItemsSingleChoice(R.array.listen_periods, initialSelection = viewModel.currentSelectedItem, waitForPositiveButton = false, selection = { _, item, _ ->
            viewModel.setSelectedItem(item)
        })
        positiveButton(android.R.string.ok){
            viewModel.saveListenPeriod()
            requireContext().startService(Intent(requireContext(), GetModelStateForegroundService::class.java))
            it.dismiss()
        }
        negativeButton(android.R.string.cancel){
            it.dismiss()
        }
    }

}