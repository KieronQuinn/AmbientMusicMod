package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Context.onKeyguardStateChanged() = callbackFlow {
    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            trySend(Unit)
        }
    }
    registerReceiverCompat(
        receiver,
        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
    )
    awaitClose {
        unregisterReceiver(receiver)
    }
}