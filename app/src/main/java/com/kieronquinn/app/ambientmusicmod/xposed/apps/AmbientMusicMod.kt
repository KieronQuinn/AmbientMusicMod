package com.kieronquinn.app.ambientmusicmod.xposed.apps

import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.utils.extensions.MethodReplacement
import com.kieronquinn.app.ambientmusicmod.xposed.XposedAppHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

//Self hooks to check whether the module is enabled
class AmbientMusicMod: XposedAppHook() {

    override val appName: String = "Ambient Music Mod"
    override val packageName: String = BuildConfig.APPLICATION_ID

    override fun onAppHooked(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod("com.kieronquinn.app.ambientmusicmod.utils.ModuleStateCheck", lpparam.classLoader, "isModuleEnabled", MethodReplacement {
            it.result = true
            true
        })
    }

}