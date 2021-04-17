package com.kieronquinn.app.ambientmusicmod.components

import android.annotation.SuppressLint
import android.content.Context
import com.kieronquinn.app.ambientmusicmod.BuildConfig

class OffsetProvider(context: Context) {

    companion object {
        //matcher_tah.leveldb comes from the module and therefore the offset is static, no need to find it every time.
        const val OFFSET_MATCHER_TAH = 2023152L
    }

    private val sharedPreferences = context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_offsets", Context.MODE_PRIVATE)

    fun getOffsetForId(id: String): Long? {
        if(id == "matcher_tah.leveldb") return OFFSET_MATCHER_TAH
        val offset = sharedPreferences.getLong(id, -1L)
        return if(offset == -1L) null
        else offset
    }

    //This is always done from IO
    @SuppressLint("ApplySharedPref")
    fun setOffsetForId(id: String, offset: Long){
        sharedPreferences.edit().putLong(id, offset).commit()
    }

}