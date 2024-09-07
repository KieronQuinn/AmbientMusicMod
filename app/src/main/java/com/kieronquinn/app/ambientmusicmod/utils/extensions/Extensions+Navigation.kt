package com.kieronquinn.app.ambientmusicmod.utils.extensions

import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun NavController.onDestinationChanged() = callbackFlow {
    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        trySend(destination)
    }
    addOnDestinationChangedListener(listener)
    awaitClose {
        removeOnDestinationChangedListener(listener)
    }
}.debounce(TAP_DEBOUNCE)

fun NavController.setOnBackPressedCallback(callback: OnBackPressedCallback) {
    NavController::class.java.getDeclaredField("onBackPressedCallback").apply {
        isAccessible = true
    }.set(this, callback)
}