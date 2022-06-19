package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class RecognitionFailureReason: Parcelable {
    @Parcelize
    object NoMatch: RecognitionFailureReason()
    @Parcelize
    object SkippedOnCall: RecognitionFailureReason()
    @Parcelize
    object SkippedSystemUserNotInForeground: RecognitionFailureReason()
    @Parcelize
    object SkippedMusicPlaying: RecognitionFailureReason()
    @Parcelize
    object SkippedAudioRecordFailed: RecognitionFailureReason()
    @Parcelize
    data class MusicRecognitionError(val errorCode: Int): RecognitionFailureReason()
    @Parcelize
    data class Busy(val otherRunningRecognitionSource: RecognitionSource): RecognitionFailureReason()
}