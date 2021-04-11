package com.kieronquinn.app.ambientmusicmod.utils.blur

import android.content.res.Resources
import android.view.View

/**
 *  Native blur is not supported on Android < 30
 */
class BlurUtilsAndroid26(resources: Resources): BlurUtils() {

    override val canBlur: Boolean = false

    override val maxBlurRadius: Float = 0f
    override val minBlurRadius: Float = 0f

    override fun applyBlur(dialogDecorView: View, appDecorView: View, radius: Int, onFailure: (() -> Unit)?) {
        // Do nothing
    }

}