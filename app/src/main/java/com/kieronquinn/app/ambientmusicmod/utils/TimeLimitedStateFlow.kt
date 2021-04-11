package com.kieronquinn.app.ambientmusicmod.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(InternalCoroutinesApi::class)
class TimeLimitedStateFlow<T>(private val lifecycleScope: CoroutineScope, private val timeout: Long, initialValue: T? = null) {

    private val stateFlow = MutableStateFlow(initialValue)
    private var cancelJob: Job? = null

    suspend fun emit(newValue: T?) {
        //Only update if required
        if(newValue == stateFlow.value) return
        cancelJob?.cancelAndJoin()
        stateFlow.emit(newValue)
        clearAfterTimeout()
    }

    suspend fun collect(flowCollector: FlowCollector<T?>) {
        return stateFlow.collect(flowCollector)
    }

    suspend fun clear(){
        cancelJob?.cancelAndJoin()
        stateFlow.emit(null)
    }

    private fun clearAfterTimeout(){
        cancelJob = lifecycleScope.launch {
            delay(timeout)
            stateFlow.emit(null)
            cancelJob = null
        }
    }

    fun asStateFlow() = stateFlow.asStateFlow()

}