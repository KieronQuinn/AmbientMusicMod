package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.animation.Animator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun Animator.runAndJoin() = suspendCancellableCoroutine<Unit> {
    val listener = object: Animator.AnimatorListener {
        override fun onAnimationStart(p0: Animator?) {
            //No-op
        }

        override fun onAnimationEnd(p0: Animator?) {
            removeListener(this)
            it.resume(Unit)
        }

        override fun onAnimationCancel(p0: Animator?) {
            //No-op
        }

        override fun onAnimationRepeat(p0: Animator?) {
            //No-op
        }
    }
    addListener(listener)
    start()
    it.invokeOnCancellation {
        cancel()
        removeListener(listener)
    }
}