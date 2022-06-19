package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.media.AudioRecordingConfiguration

fun AudioRecordingConfiguration.getClientUid(): Int? {
    return try {
        AudioRecordingConfiguration::class.java.getMethod("getClientUid").invoke(this) as Int
    }catch (e: SecurityException){
        //Don't have the permission for some reason, absorb
        null
    }
}