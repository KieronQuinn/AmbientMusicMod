package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.contents

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment

class SettingsDeveloperOptionsLogContentsBottomSheetFragment: BaseBottomSheetDialogFragment() {

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.developer_options_log_contents_title)
        message(R.string.developer_options_log_contents_content)
        withOk()
    }

}