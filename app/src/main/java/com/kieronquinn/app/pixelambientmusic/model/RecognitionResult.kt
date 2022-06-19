package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecognitionResult(
    val trackName: String,
    val artist: String,
    val recognitionSource: RecognitionSource,
    val players: Array<String>,
    val googleId: String?,
    var audio: ShortArray?
): Parcelable {

    fun stripAudioIfNeeded(includeAudio: Boolean): RecognitionResult {
        if(!includeAudio) audio = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecognitionResult

        if (trackName != other.trackName) return false
        if (artist != other.artist) return false
        if (recognitionSource != other.recognitionSource) return false
        if (!players.contentEquals(other.players)) return false
        if (googleId != other.googleId) return false
        if (audio != null) {
            if (other.audio == null) return false
            if (!audio.contentEquals(other.audio)) return false
        } else if (other.audio != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackName.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + recognitionSource.hashCode()
        result = 31 * result + players.contentHashCode()
        result = 31 * result + (googleId?.hashCode() ?: 0)
        result = 31 * result + (audio?.contentHashCode() ?: 0)
        return result
    }


}
