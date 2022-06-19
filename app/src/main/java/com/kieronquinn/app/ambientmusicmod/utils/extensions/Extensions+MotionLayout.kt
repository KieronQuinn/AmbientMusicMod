package com.kieronquinn.app.ambientmusicmod.utils.extensions

import androidx.annotation.IdRes
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 *  Emits a triple of (startId, endId, progress) of a transition
 */
fun MotionLayout.progressCallback() = callbackFlow {
    val callback = object: MotionLayout.TransitionListener {
        override fun onTransitionStarted(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int
        ) {
            //No-op
        }

        override fun onTransitionChange(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int,
            progress: Float
        ) {
            trySend(Triple(startId, endId, progress))
        }

        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            //No-op
        }

        override fun onTransitionTrigger(
            motionLayout: MotionLayout?,
            triggerId: Int,
            positive: Boolean,
            progress: Float
        ) {
            //No-op
        }
    }
    addTransitionListener(callback)
    awaitClose {
        removeTransitionListener(null)
    }
}

fun MotionLayout.onComplete() = callbackFlow {
    val callback = object: MotionLayout.TransitionListener {
        override fun onTransitionStarted(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int
        ) {
            //No-op
        }

        override fun onTransitionChange(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int,
            progress: Float
        ) {
            //No-op
        }

        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            trySend(currentId)
        }

        override fun onTransitionTrigger(
            motionLayout: MotionLayout?,
            triggerId: Int,
            positive: Boolean,
            progress: Float
        ) {
            //No-op
        }
    }
    addTransitionListener(callback)
    awaitClose {
        removeTransitionListener(null)
    }
}

suspend fun MotionLayout.runTransition(@IdRes id: Int) = suspendCoroutine<Boolean> {
    if(getTransition(id) == null) {
        it.resume(false)
    }
    var hasResumed = false
    setTransition(id)
    transitionToEnd {
        if(!hasResumed) {
            hasResumed = true
            it.resume(true)
        }
    }
}