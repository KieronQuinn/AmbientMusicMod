package com.kieronquinn.app.ambientmusicmod.app.ui.pasincompatible

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.afollestad.materialdialogs.MaterialDialog
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices

class PixelAmbientServicesIncompatibleBottomSheetFragment: BaseBottomSheetDialogFragment() {

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.pixel_ambient_services_incompatible_title)
        message(R.string.pixel_ambient_services_incompatible_content)
        positiveButton(R.string.pixel_ambient_services_incompatible_app_info){
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME}")
            })
        }
        negativeButton(R.string.close)
    }

}