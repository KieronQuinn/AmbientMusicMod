package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun <T> Context.getSettingAsFlow(uri: Uri, converter: (Context) -> T) = callbackFlow<T?> {
    val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            trySend(converter(this@getSettingAsFlow))
        }
    }
    trySend(converter(this@getSettingAsFlow))
    contentResolver.safeRegisterContentObserver(uri, false, observer)
    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}

fun Context.secureStringConverter(name: String): (Context) -> String? {
    return { _: Context ->
        Settings_Secure_getStringSafely(contentResolver, name)
    }
}

fun Context.secureBooleanConverter(name: String): (Context) -> Boolean {
    return { _: Context ->
        Settings_Secure_getIntSafely(contentResolver, name, 0) == 1
    }
}

fun Settings_Secure_getIntSafely(contentResolver: ContentResolver, setting: String, default: Int): Int {
    return try {
        Settings.Secure.getInt(contentResolver, setting, default)
    }catch (e: Settings.SettingNotFoundException){
        default
    }
}

fun Settings_Secure_getStringSafely(contentResolver: ContentResolver, setting: String): String? {
    return try {
        Settings.Secure.getString(contentResolver, setting)
    }catch (e: Settings.SettingNotFoundException){
        null
    }
}

fun Settings_Global_getIntSafely(contentResolver: ContentResolver, setting: String, default: Int): Int {
    return try {
        Settings.Global.getInt(contentResolver, setting, default)
    }catch (e: Settings.SettingNotFoundException){
        default
    }
}