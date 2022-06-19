package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.animation.ValueAnimator
import com.google.android.material.progressindicator.BaseProgressIndicator

fun BaseProgressIndicator<*>.animateProgress(start: Int, duration: Long): ValueAnimator {
    progress = start
    return ValueAnimator.ofInt(start, 100).apply {
        setDuration(duration)
        addUpdateListener {
            progress = it.animatedValue as Int
        }
    }
}