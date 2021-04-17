package com.kieronquinn.app.ambientmusicmod.components

import android.content.SharedPreferences
import com.kieronquinn.app.ambientmusicmod.utils.extensions.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class BaseSharedPreferences {

    abstract fun shared(key: String, default: String): ReadWriteProperty<Any?, String>
    abstract fun shared(key: String, default: Int): ReadWriteProperty<Any?, Int>
    abstract fun shared(key: String, default: Boolean): ReadWriteProperty<Any?, Boolean>
    abstract fun shared(key: String, default: Double): ReadWriteProperty<Any?, Double>
    abstract fun shared(key: String, default: Long): ReadWriteProperty<Any?, Long>
    abstract fun shared(key: String, default: Float): ReadWriteProperty<Any?, Float>

    inline fun <reified T : Enum<T>> shared(key: String, default: Enum<T>): ReadWriteProperty<Any?, T> {
        return when(this){
            is AppSharedPreferences -> sharedEnum(key, default)
            is XposedSharedPreferences -> sharedEnum(key, default)
            else -> throw NotImplementedError("Unknown shared prefs type")
        }
    }

    abstract fun sendUpdateIntent()

    abstract val sharedPreferences: SharedPreferences

}