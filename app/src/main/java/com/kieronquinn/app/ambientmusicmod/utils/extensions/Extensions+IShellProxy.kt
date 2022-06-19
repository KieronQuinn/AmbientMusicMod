package com.kieronquinn.app.ambientmusicmod.utils.extensions

import com.android.internal.policy.IKeyguardDismissCallback
import com.kieronquinn.app.ambientmusicmod.IShellProxy

fun IShellProxy.dismissKeyguard(onSuccess: () -> Unit, message: String) {
    val callback = object: IKeyguardDismissCallback.Stub() {
        override fun onDismissSucceeded() {
            onSuccess()
        }

        override fun onDismissCancelled() {
            //No-op
        }

        override fun onDismissError() {
            //No-op
        }
    }.asBinder()
    dismissKeyguard(callback, message)
}