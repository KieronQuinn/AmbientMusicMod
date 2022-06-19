package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.res.Resources
import androidx.annotation.ArrayRes

fun Resources.getResourceIdArray(@ArrayRes resourceId: Int): Array<Int> {
    val array = obtainTypedArray(resourceId)
    val items = mutableListOf<Int>()
    for(i in 0 until array.length()){
        items.add(array.getResourceId(i, 0))
    }
    array.recycle()
    return items.toTypedArray()
}