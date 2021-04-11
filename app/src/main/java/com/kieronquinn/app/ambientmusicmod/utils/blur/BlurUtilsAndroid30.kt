package com.kieronquinn.app.ambientmusicmod.utils.blur

import android.app.ActivityManager
import android.content.res.Resources
import android.os.Build
import android.view.SurfaceControl
import android.view.View
import androidx.annotation.RequiresApi
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getBoolean
import java.lang.Exception

/**
 *  Native blur implementation based off BlurUtils in SystemUI, which can be enabled for the notification shade + power menu
 *  with `resetprop ro.surface_flinger.supports_background_blur 1` in a post-fs-data Magisk script
 *
 *  For our use, we will ignore the supports_background_blur prop, but allow the user to disable it with the sf.disable_blurs prop
 *  and not enable it if the graphics does not support it (isHighEndGfx != true)
 */

@RequiresApi(Build.VERSION_CODES.R)
class BlurUtilsAndroid30(resources: Resources): BlurUtils() {

    override val canBlur: Boolean
        get() = supportsBlursOnWindows()

    override val minBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.min_window_blur_radius).toFloat()
    }

    override val maxBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.max_window_blur_radius).toFloat()
    }

    private val getViewRootImpl by lazy {
        try {
            View::class.java.getMethod("getViewRootImpl")
        }catch (e: NoSuchMethodException){
            null
        }
    }

    private val viewRootImpl by lazy {
        try {
            Class.forName("android.view.ViewRootImpl")
        }catch (e: ClassNotFoundException){
            null
        }
    }

    private val getSurfaceControl by lazy {
        try {
            viewRootImpl?.getMethod("getSurfaceControl")
        }catch (e: NoSuchMethodException) {
            null
        }
    }

    private val setBackgroundBlurRadius by lazy {
        try {
            SurfaceControl.Transaction::class.java.getMethod("setBackgroundBlurRadius", SurfaceControl::class.java, Integer.TYPE)
        }catch (e: NoSuchMethodException) {
            null
        }
    }

    private val blurDisabledSysProp by lazy {
        SystemProperties_getBoolean("persist.sys.sf.disable_blurs", false)
    }

    private val isHighEndGfx by lazy {
        try {
            ActivityManager::class.java.getMethod("isHighEndGfx").invoke(null) as? Boolean ?: false
        }catch (e: Exception){
            false
        }
    }

    /**
     *  Applies a blur using the hidden SurfaceControl.Transaction.setBackgroundBlurRadius()
     *  If the blur call fails, the failure block will be invoked
     */
    override fun applyBlur(dialogDecorView: View, appDecorView: View, radius: Int, onFailure: (() -> Unit)?) {
        if(!supportsBlursOnWindows()) return
        runCatching {
            val viewRootImpl = getViewRootImpl?.invoke(dialogDecorView) ?: return
            val surfaceControl = getSurfaceControl?.invoke(viewRootImpl) as? SurfaceControl ?: return
            val transaction = SurfaceControl.Transaction()
            setBackgroundBlurRadius?.invoke(transaction, surfaceControl, radius)
            transaction.apply()
        }.onFailure {
            onFailure?.invoke()
        }
    }

    private fun supportsBlursOnWindows(): Boolean {
        return !blurDisabledSysProp && isHighEndGfx
    }

}