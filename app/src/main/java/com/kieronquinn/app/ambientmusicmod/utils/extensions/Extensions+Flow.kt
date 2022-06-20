package com.kieronquinn.app.ambientmusicmod.utils.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

suspend fun <T> Flow<T>.await(check: (T) -> Boolean): T {
    return first {
        check(it)
    }
}

fun <T> Flow<T>.autoClearAfterBy(delay: suspend (T) -> Long?): Flow<T?> = channelFlow {
    val original = this@autoClearAfterBy
    original.collectLatest { newValue ->
        send(newValue)
        val delayTime = delay(newValue) ?: return@collectLatest
        delay(delayTime)
        send(null)
    }
}

suspend fun <T> Flow<T?>.firstNotNull(): T {
    return first { it != null }!!
}