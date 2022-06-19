package com.kieronquinn.app.ambientmusicmod.utils.extensions

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation

suspend inline fun <T> suspendCoroutineWithTimeout(timeout: Long, crossinline block: (Continuation<T>) -> Unit ) : T? {
    var finalValue : T? = null
    withTimeoutOrNull(timeout) {
        finalValue = suspendCancellableCoroutine(block = block)
    }
    return finalValue
}

suspend inline fun <T> suspendCancellableCoroutineWithTimeout(timeout: Long, crossinline block: (CancellableContinuation<T>) -> Unit ) : T? {
    var finalValue : T? = null
    withTimeoutOrNull(timeout) {
        finalValue = suspendCancellableCoroutine(block = block)
    }
    return finalValue
}