package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

fun View.slideOut(callback: () -> Unit){
    AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom).apply {
        onEnd {
            isVisible = false
            callback.invoke()
        }
    }.also {
        startAnimation(it)
    }
}

fun View.slideIn(callback: () -> Unit){
    isVisible = true
    AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom).apply {
        onEnd {
            callback.invoke()
        }
    }.also {
        startAnimation(it)
    }
}

fun Animation.onEnd(callback: () -> Unit){
    setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            callback.invoke()
        }
    })
}

inline fun <T> ValueAnimator.asFlow(crossinline setup: (ValueAnimator) -> Unit): Flow<T> = callbackFlow {
    setup.invoke(this@asFlow)
    addUpdateListener {
        offer(it.animatedValue as T)
    }
    start()
    awaitClose { cancel() }
}