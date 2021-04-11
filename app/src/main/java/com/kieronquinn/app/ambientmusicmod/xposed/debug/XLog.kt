package com.kieronquinn.app.ambientmusicmod.xposed.debug

import android.content.Context
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.components.XposedSharedPreferences

object XLog {

    private var isDebugModeEnabled: Boolean? = null

    fun attach(context: Context){
        try {
            val sharedPreferences = XposedSharedPreferences(context)
            isDebugModeEnabled = sharedPreferences.developerEnableLogging
        }catch (e: Exception){
            //Don't crash system
        }
    }

    fun d(message: String, alwaysLog: Boolean = false){
        if(isDebugModeEnabled == true || alwaysLog) Log.d("AmbientMusicMod", message)
    }

}