package com.kieronquinn.app.ambientmusicmod.model.recognition

import android.os.Parcelable
import com.kieronquinn.app.ambientmusicmod.utils.ObfuscatedNames
import de.robv.android.xposed.XposedHelpers
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TrackMetadata(val artist: String, val track: String): Parcelable {

    companion object {
        @ObfuscatedNames("mapping before isEmpty checks, below com.google.intelligence.sense.ambientmusic.AOD_CLICK")
        fun fromObfuscatedMusicRecognitionResult(obfuscatedMusicRecognitionResult: Any): TrackMetadata? {
            val track = XposedHelpers.getObjectField(obfuscatedMusicRecognitionResult, "d") as? String ?: return null
            val artist = XposedHelpers.getObjectField(obfuscatedMusicRecognitionResult, "e") as? String ?: return null
            return TrackMetadata(
                artist,
                track
            )
        }
    }

}