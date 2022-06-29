package com.kieronquinn.app.ambientmusicmod.utils.context

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.service.ShizukuService.Companion.ROOT_PACKAGE
import com.kieronquinn.app.ambientmusicmod.service.ShizukuService.Companion.ROOT_UID
import com.kieronquinn.app.ambientmusicmod.service.ShizukuService.Companion.SHELL_UID

class ShellContext(context: Context, private val isRoot: Boolean) : ContextWrapper(context) {

    override fun getOpPackageName(): String {
        return "uid:${if(isRoot) ROOT_UID else SHELL_UID}"
    }

    @SuppressLint("NewApi")
    override fun getAttributionSource(): AttributionSource {
        val uid = if (isRoot) ROOT_UID else SHELL_UID
        return AttributionSource.Builder(uid)
            .setPackageName(if(isRoot) "android" else ROOT_PACKAGE).build().also {
                Log.d("RootMRM", "Getting attribution source, returning UID ${it.uid}")
            }
    }
}