package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.IServiceConnection
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import android.util.Log
import androidx.core.os.BuildCompat

fun IActivityManager.bindServiceInstanceCompat(
    context: Context,
    serviceConnection: IServiceConnection,
    thread: IApplicationThread,
    token: IBinder?,
    intent: Intent,
    flags: Int
): Int {
    try {
        val packageName = Context::class.java.getMethod("getOpPackageName")
            .invoke(context) as String
        val userHandle = Context::class.java.getMethod("getUser").invoke(context) as UserHandle
        val identifier =
            UserHandle::class.java.getMethod("getIdentifier").invoke(userHandle) as Int
        Intent::class.java.getMethod("prepareToLeaveProcess", Context::class.java)
            .invoke(intent, context)
        return bindServiceInstanceCompat(
            thread,
            token,
            intent,
            null,
            serviceConnection,
            flags,
            null,
            packageName,
            identifier
        )
    } catch (e: Exception) {
        Log.e("ServiceBind", "Error binding service", e)
        return 0
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun IActivityManager.bindServiceInstanceCompat(
    caller: IApplicationThread?,
    token: IBinder?,
    service: Intent?,
    resolvedType: String?,
    connection: IServiceConnection?,
    flags: Int,
    instanceName: String?,
    callingPackage: String?,
    userId: Int
): Int {
    return if (BuildCompat.isAtLeastT()) {
        bindServiceInstance(
            caller,
            token,
            service,
            resolvedType,
            connection,
            flags,
            instanceName,
            callingPackage,
            userId
        )
    } else {
        bindIsolatedService(
            caller,
            token,
            service,
            resolvedType,
            connection,
            flags,
            instanceName,
            callingPackage,
            userId
        )
    }
}