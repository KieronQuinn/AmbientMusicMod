package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecognitionFailure(
    val failureReason: RecognitionFailureReason,
    val source: RecognitionSource,
    var audio: ShortArray?
): Parcelable {

    fun stripAudioIfNeeded(includeAudio: Boolean): RecognitionFailure {
        if(!includeAudio) audio = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecognitionFailure

        if (failureReason != other.failureReason) return false
        if (source != other.source) return false
        if (audio != null) {
            if (other.audio == null) return false
            if (!audio.contentEquals(other.audio)) return false
        } else if (other.audio != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = failureReason.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (audio?.contentHashCode() ?: 0)
        return result
    }
}