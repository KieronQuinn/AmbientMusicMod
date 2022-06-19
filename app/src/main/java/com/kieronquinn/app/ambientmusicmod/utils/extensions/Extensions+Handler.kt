package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.os.Handler
import android.os.Looper

fun runOnUiThread(block: () -> Unit) {
    Handler(Looper.getMainLooper()).post {
        block()
    }
}