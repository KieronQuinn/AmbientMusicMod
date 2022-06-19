package com.kieronquinn.app.ambientmusicmod.utils.extensions

import org.json.JSONArray

fun JSONArray.toStringArray(): Array<String> {
    return ArrayList<String>().apply {
        for(i in 0 until length()){
            add(getString(i))
        }
    }.toTypedArray()
}