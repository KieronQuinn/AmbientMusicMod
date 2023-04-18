package com.kieronquinn.app.ambientmusicmod.utils.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Lifecycle.runOnDestroy(block: () -> Unit) {
    addObserver(object: LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            block()
        }
    })
}

fun LifecycleOwner.whenResumed(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            block()
        }
    }
}

fun LifecycleOwner.whenCreated(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
            block()
        }
    }
}