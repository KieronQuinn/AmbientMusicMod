package com.kieronquinn.app.ambientmusicmod.xposed

import com.kieronquinn.app.ambientmusicmod.xposed.apps.AmbientMusicMod
import com.kieronquinn.app.ambientmusicmod.xposed.apps.Android
import com.kieronquinn.app.ambientmusicmod.xposed.apps.DevicePersonalisationServices
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Xposed: IXposedHookLoadPackage {

    private val appList = arrayOf(
        AmbientMusicMod(),
        Android(),
        DevicePersonalisationServices(),
        PixelAmbientServices()
    )

    /**
     *  Xposed entry point when an app launches and is hooked. Simply fires off to the implementation if it exists.
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        for (app in appList) {
            if (app.packageName == lpparam.packageName) {
                app.onAppHooked(lpparam)
            }
        }
    }

}