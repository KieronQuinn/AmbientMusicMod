package com.kieronquinn.app.ambientmusicmod.utils.extensions

import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

fun RecyclerView.shouldShrinkFab() = callbackFlow {
    val listener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            trySend(dy > 0)
        }
    }
    addOnScrollListener(listener)
    awaitClose {
        removeOnScrollListener(listener)
    }
}.distinctUntilChanged().debounce(TAP_DEBOUNCE)