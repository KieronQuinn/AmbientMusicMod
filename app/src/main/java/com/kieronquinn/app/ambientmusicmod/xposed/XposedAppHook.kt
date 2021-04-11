package com.kieronquinn.app.ambientmusicmod.xposed

import android.app.AndroidAppHelper
import android.content.Context
import com.kieronquinn.app.ambientmusicmod.components.XposedSharedPreferences
import com.kieronquinn.app.ambientmusicmod.xposed.debug.XLog
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class XposedAppHook {

    val context by lazy {
        AndroidAppHelper.currentApplication() as Context
    }

    val sharedPrefs by lazy {
        XposedSharedPreferences(context)
    }

    abstract val packageName: String
    abstract val appName: String

    abstract fun onAppHooked(lpparam: XC_LoadPackage.LoadPackageParam)

}