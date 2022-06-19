package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath

fun LottieAnimationView.filterColour(
    vararg keyPath: String,
    @ColorInt filter: Int
) {
    addValueCallback(KeyPath(*keyPath), LottieProperty.COLOR_FILTER) {
        PorterDuffColorFilter(
            filter,
            PorterDuff.Mode.SRC_ATOP
        )
    }
}

fun LottieAnimationView.replaceColour(vararg keyPath: String, replaceWith: Int) {
    addValueCallback(KeyPath(*keyPath), LottieProperty.COLOR) {
        replaceWith
    }
}