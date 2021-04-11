package com.kieronquinn.app.ambientmusicmod.xposed.apps

import com.kieronquinn.app.ambientmusicmod.utils.extensions.MethodHook
import com.kieronquinn.app.ambientmusicmod.xposed.XposedAppHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*

/**
 *  Because Device Personalisation Services also has Ambient Music in it on some devices, and continues to try to mess with the model *even when related services are disabled*,
 *  this hook breaks its ability to mess with it by changing the model UUID to something random. Because the services are disabled, this shouldn't do anything bad,
 *  other than cause some suppressed errors.
 */
class DevicePersonalisationServices: XposedAppHook() {

    override val appName: String = "Device Personalisation Services"
    override val packageName: String = "com.google.android.as"

    private val dpsUuids = arrayOf(
        "6ac81359-2dc2-4fea-a0a0-bd378ed6da4f",
        "9f6ad62a-1f0b-11e7-87c5-40a8f03d3f15",
        "12caddb1-acdb-4dce-8cb0-2e95a2313aee"
    )

    override fun onAppHooked(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(UUID::class.java, "fromString", String::class.java, MethodHook {
            val uuid = it.args[0] as? String
            if(dpsUuids.contains(uuid)){
                // Intentionally break model loading
                it.result = UUID.randomUUID()
            }
        })
    }

}