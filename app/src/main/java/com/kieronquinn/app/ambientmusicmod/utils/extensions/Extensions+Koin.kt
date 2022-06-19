package com.kieronquinn.app.ambientmusicmod.utils.extensions

import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback

fun Scope.runOnClose(block: () -> Unit) {
    registerCallback(object: ScopeCallback {
        override fun onScopeClose(scope: Scope) {
            block()
        }
    })
}