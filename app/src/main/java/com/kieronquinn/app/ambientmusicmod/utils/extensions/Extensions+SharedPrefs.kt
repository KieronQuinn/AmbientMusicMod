package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import com.kieronquinn.app.ambientmusicmod.BuildConfig

val Fragment.sharedPreferences
    get() = context?.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)

val Context.sharedPreferences: SharedPreferences?
    get() = getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)

val Context.legacySharedPreferences: SharedPreferences?
    get() = getSharedPreferences("${BuildConfig.APPLICATION_ID}_preferences", Context.MODE_PRIVATE)

