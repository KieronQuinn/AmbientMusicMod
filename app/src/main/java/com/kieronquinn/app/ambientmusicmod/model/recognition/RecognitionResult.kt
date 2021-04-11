package com.kieronquinn.app.ambientmusicmod.model.recognition

import android.os.Parcelable
import com.kieronquinn.app.ambientmusicmod.utils.ObfuscatedNames
import de.robv.android.xposed.XposedHelpers
import kotlinx.android.parcel.Parcelize

sealed class RecognitionResult(open val recognitionResponse: RecognitionResponse, open var retryTime: Long = 0): Parcelable {

    @Parcelize
    data class GenericResult(override val recognitionResponse: RecognitionResponse, override var retryTime: Long = 0): RecognitionResult(recognitionResponse, retryTime){
        override fun toString(): String {
            return "GenericResult with response $recognitionResponse"
        }
    }

    @Parcelize
    data class MusicRecognitionResult(override val recognitionResponse: RecognitionResponse, val trackMetadata: TrackMetadata, override var retryTime: Long = 0): RecognitionResult(recognitionResponse){
        override fun toString(): String {
            return "MusicRecognitionResult with response $recognitionResponse and a recognised track of: ${trackMetadata.artist} - ${trackMetadata.track}"
        }
    }

    @Parcelize
    data class Error(override val recognitionResponse: RecognitionResponse = RecognitionResponse.UNKNOWN_RECOGNITION_STATUS, override var retryTime: Long = 0): RecognitionResult(recognitionResponse){
        override fun toString(): String {
            return "Error result with response $recognitionResponse"
        }
    }

    companion object {
        @ObfuscatedNames("update toRecognitionResponse first, then look at mapping in class containing <MusicRecognitionHandler.java>")
        fun fromObfuscated(obfuscatedObject: Any): RecognitionResult {
            val recognitionResponse = XposedHelpers.callMethod(obfuscatedObject, "a").toRecognitionResponse()
            if(recognitionResponse == RecognitionResponse.MUSIC_RECOGNIZED) {
                @ObfuscatedNames("immediate == null check after checking the result against RecognitionResponse enum")
                val recognitionAttemptResult = XposedHelpers.callMethod(obfuscatedObject, "b") ?: return Error(
                    recognitionResponse
                )
                @ObfuscatedNames("search for <MusicRecognitionResult>, two methods above the toString()")
                val musicRecognitionResult = XposedHelpers.callMethod(recognitionAttemptResult, "i")
                val trackMetadata = TrackMetadata.fromObfuscatedMusicRecognitionResult(
                    musicRecognitionResult
                )
                    ?: return Error(
                        recognitionResponse
                    )
                return MusicRecognitionResult(
                    recognitionResponse,
                    trackMetadata
                )
            }else{
                return GenericResult(
                    recognitionResponse
                )
            }
        }
    }

}