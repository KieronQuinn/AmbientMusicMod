package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecognitionCallbackMetadata(
    val recognitionSource: RecognitionSource,
    val includeAudio: Boolean
): Parcelable