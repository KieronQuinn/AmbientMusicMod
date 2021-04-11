package com.kieronquinn.app.ambientmusicmod.utils.blur

import android.content.res.Resources
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import androidx.annotation.RequiresApi
import com.kieronquinn.app.ambientmusicmod.R

/**
 *  Uses the native RenderEffect API on Android S
 */
@RequiresApi(31)
class BlurUtilsAndroid31(resources: Resources): BlurUtils() {

    override val canBlur: Boolean = false

    override val minBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.min_window_blur_radius_31).toFloat()
    }

    override val maxBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.max_window_blur_radius_31).toFloat()
    }

    override fun applyBlur(dialogDecorView: View, appDecorView: View, radius: Int, onFailure: (() -> Unit)?) {
        if(radius == 0){
            appDecorView.setRenderEffect(null)
        }else {
            val renderEffect = RenderEffect.createBlurEffect(radius.toFloat(), radius.toFloat(), Shader.TileMode.MIRROR)
            appDecorView.setRenderEffect(renderEffect)
        }
    }

}