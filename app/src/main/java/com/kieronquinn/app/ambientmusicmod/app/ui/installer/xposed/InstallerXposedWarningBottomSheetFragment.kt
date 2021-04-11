package com.kieronquinn.app.ambientmusicmod.app.ui.installer.xposed

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallerXposedWarningBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<InstallerXposedWarningBottomSheetViewModel>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.installer_warning_bottom_sheet_title)
        message(R.string.installer_warning_bottom_sheet_content)
        positiveButton(R.string.installer_warning_bottom_sheet_ignore){
            viewModel.onIgnoreClicked()
        }
        negativeButton(android.R.string.cancel)
    }

}