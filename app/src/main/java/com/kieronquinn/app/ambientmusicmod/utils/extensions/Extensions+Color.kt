package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.Context
import androidx.core.content.ContextCompat

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}