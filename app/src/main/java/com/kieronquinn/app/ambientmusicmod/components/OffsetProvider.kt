package com.kieronquinn.app.ambientmusicmod.components

import android.annotation.SuppressLint
import android.content.Context
import com.kieronquinn.app.ambientmusicmod.BuildConfig

class OffsetProvider(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_offsets", Context.MODE_PRIVATE)

    fun getOffsetForId(id: String): Long? {
        val offset = sharedPreferences.getLong(id, -1L)
        return if(offset == -1L) null
        else offset
    }

    //This is always done from IO
    @SuppressLint("ApplySharedPref")
    fun setOffsetForId(id: String, offset: Long){
        sharedPreferences.edit().putLong(id, offset).commit()
    }

    class OffsetException(id: String): Exception("Offset not found for $id")

}