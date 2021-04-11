package com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.playback.fileinfo

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment

class SettingsManualTriggerPlaybackFileInfoBottomSheetFragment: BaseBottomSheetDialogFragment() {

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.bs_manual_trigger_playback_file_info_title)
        message(R.string.bs_manual_trigger_playback_file_info_content)
        withOk()
    }

}