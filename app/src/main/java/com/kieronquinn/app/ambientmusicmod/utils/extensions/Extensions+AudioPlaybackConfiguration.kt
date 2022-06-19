package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.media.AudioPlaybackConfiguration

fun AudioPlaybackConfiguration.getClientUid(): Int? {
    return try {
        AudioPlaybackConfiguration::class.java.getMethod("getClientUid").invoke(this) as Int
    }catch (e: SecurityException){
        //Don't have the permission for some reason, absorb
        null
    }
}