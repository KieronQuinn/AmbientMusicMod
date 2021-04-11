package com.kieronquinn.app.ambientmusicmod.components

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty

interface BaseSharedPreferences {

    fun shared(key: String, default: String): ReadWriteProperty<Any?, String>
    fun shared(key: String, default: Int): ReadWriteProperty<Any?, Int>
    fun shared(key: String, default: Boolean): ReadWriteProperty<Any?, Boolean>
    fun shared(key: String, default: Double): ReadWriteProperty<Any?, Double>
    fun shared(key: String, default: Long): ReadWriteProperty<Any?, Long>
    fun shared(key: String, default: Float): ReadWriteProperty<Any?, Float>

    fun sendUpdateIntent()

    val sharedPreferences: SharedPreferences

}