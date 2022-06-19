package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SettingsState(
    val mainEnabled: Boolean,
    val onDemandEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val bannerMessage: BannerMessage?,
    val lastRecognisedSong: LastRecognisedSong?
): Parcelable

@Parcelize
data class LastRecognisedSong(
    val track: String,
    val artist: String,
    val timestamp: Long,
    val recognitionSource: RecognitionSource
): Parcelable

enum class BannerMessage {
    PERMISSIONS_NEEDED, //Local
    DOWNLOADING, //Service
    NO_INTERNET, //Local
    SEARCH_BUTTON_BEING_SET_UP, //Service
    WAITING_FOR_UNMETERED_INTERNET, //Local
    DO_NOT_DISTURB_ENABLED, //Local
    APP_USING_DEVICE_AUDIO, //Local
    APP_RECORDING_AUDIO, //Local
    GOOGLE_APP_INVALID, //Local
    MICROPHONE_DISABLED //Shizuku
}
