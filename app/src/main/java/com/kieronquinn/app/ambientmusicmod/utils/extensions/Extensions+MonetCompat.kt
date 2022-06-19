package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.Context
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.toArgb

fun MonetCompat.getColorSurface(context: Context): Int {
    return if(context.isDarkMode){
        getMonetColors().neutral1[900]!!.toArgb()
    }else{
        getMonetColors().neutral1[10]!!.toArgb()
    }
}

fun MonetCompat.getColorOnSurface(context: Context): Int {
    return if(context.isDarkMode){
        getMonetColors().neutral1[100]!!.toArgb()
    }else{
        getMonetColors().neutral1[900]!!.toArgb()
    }
}

fun MonetCompat.getColorOnSurfaceVariant(context: Context): Int {
    return if(context.isDarkMode){
        getMonetColors().neutral2[200]!!.toArgb()
    }else{
        getMonetColors().neutral2[700]!!.toArgb()
    }
}