package com.kieronquinn.app.ambientmusicmod.utils.extensions

import java.nio.ByteBuffer

fun ShortArray.toByteArray(): ByteArray {
    return ByteBuffer.allocate(size * 2).apply {
        asShortBuffer().put(this@toByteArray)
    }.array()
}

fun ShortArray.applyGain(gain: Float): ShortArray {
    if (isNotEmpty()) {
        for (i in indices) {
            this[i] = (this[i] * gain).toInt().coerceAtMost(Short.MAX_VALUE.toInt()).toShort()
        }
    }
    return this
}