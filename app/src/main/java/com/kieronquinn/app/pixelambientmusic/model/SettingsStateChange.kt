package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SettingsStateChange(
    val mainEnabled: Boolean? = null,
    val onDemandEnabled: Boolean? = null
): Parcelable