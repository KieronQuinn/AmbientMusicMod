package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecognitionMetadata(
    val recognitionTime: Long,
    val currentPos: Long,
    val remainingTime: Long
): Parcelable