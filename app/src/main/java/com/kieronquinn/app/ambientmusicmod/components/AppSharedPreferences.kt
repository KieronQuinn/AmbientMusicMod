package com.kieronquinn.app.ambientmusicmod.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.utils.extensions.ReadWriteProperty
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices

//These calls are from background threads and need to await changes so commit is required
@SuppressLint("ApplySharedPref")
class AppSharedPreferences(private val context: Context) : AmbientSharedPreferences() {

    override val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)
    }

    override fun shared(key: String, default: String) = ReadWriteProperty({
        sharedPreferences.getString(key, default) ?: default
    }, {
        sharedPreferences.edit().putString(key, it).commit()
    })

    override fun shared(key: String, default: Int) = ReadWriteProperty({
        sharedPreferences.getInt(key, default)
    }, {
        sharedPreferences.edit().putInt(key, it).commit()
    })

    override fun shared(key: String, default: Boolean) = ReadWriteProperty({
        sharedPreferences.getBoolean(key, default)
    }, {
        sharedPreferences.edit().putBoolean(key, it).commit()
    })

    //The following two, while their types are supported by SharedPreferences, are not by the SharedPrefsProvider

    override fun shared(key: String, default: Float) = ReadWriteProperty({
        sharedPreferences.getString(key, default.toString())?.toFloatOrNull() ?: default
    }, {
        sharedPreferences.edit().putString(key, it.toString()).commit()
    })

    override fun shared(key: String, default: Long) = ReadWriteProperty({
        sharedPreferences.getString(key, default.toString())?.toLongOrNull() ?: default
    }, {
        sharedPreferences.edit().putString(key, it.toString()).commit()
    })

    override fun shared(key: String, default: Double) = ReadWriteProperty({
        sharedPreferences.getString(key, default.toString())?.toDoubleOrNull() ?: default
    }, {
        sharedPreferences.edit().putString(key, it.toString()).commit()
    })

    override fun sendUpdateIntent() {
        context.sendSecureBroadcast(Intent(INTENT_ACTION_SETTINGS_CHANGED).apply {
            `package` = PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
        })
    }

}

