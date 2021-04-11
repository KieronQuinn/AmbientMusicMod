package com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.troubleshooting

import androidx.annotation.StringRes
import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment

class SettingsManualTriggerTroubleshootingBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val arguments by navArgs<SettingsManualTriggerTroubleshootingBottomSheetFragmentArgs>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.bs_manual_trigger_troubleshooting_title)
        message(arguments.troubleshootingType.contentRes)
        withOk()
    }

    enum class TroubleshootingType(@StringRes val contentRes: Int){
        TYPE_NOT_STARTED(R.string.bs_manual_trigger_troubleshooting_type_not_started),
        TYPE_NO_MUSIC(R.string.bs_manual_trigger_troubleshooting_type_no_music),
        TYPE_NOT_MUSIC(R.string.bs_manual_trigger_troubleshooting_type_not_music),
        TYPE_UNKNOWN(R.string.bs_manual_trigger_troubleshooting_type_unknown),
        TYPE_NO_RESULT(R.string.bs_manual_trigger_troubleshooting_type_no_result)
    }

}