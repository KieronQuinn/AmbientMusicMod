package com.kieronquinn.app.ambientmusicmod.utils.extensions

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun ExtendedFloatingActionButton.collapse() = suspendCoroutine<Unit> {
    if(!isExtended) {
        it.resume(Unit)
        return@suspendCoroutine
    }
    val callback = object: ExtendedFloatingActionButton.OnChangedCallback() {
        override fun onShrunken(extendedFab: ExtendedFloatingActionButton?) {
            super.onShrunken(extendedFab)
            it.resume(Unit)
        }
    }
    shrink(callback)
}