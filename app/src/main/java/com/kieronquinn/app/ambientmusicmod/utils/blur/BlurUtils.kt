package com.kieronquinn.app.ambientmusicmod.utils.blur

import android.view.View

abstract class BlurUtils {

    companion object {
        private fun lerp(start: Float, stop: Float, amount: Float): Float {
            return start + (stop - start) * amount
        }
    }

    abstract val minBlurRadius: Float
    abstract val maxBlurRadius: Float

    abstract fun applyBlur(dialogDecorView: View, appDecorView: View, radius: Int, onFailure: (() -> Unit)? = null)

    abstract val canBlur: Boolean

    fun blurRadiusOfRatio(ratio: Float): Int {
        return if (ratio == 0.0f) 0 else lerp(minBlurRadius, maxBlurRadius, ratio).toInt()
    }

}