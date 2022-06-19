package com.kieronquinn.app.pixelambientmusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PermissionResult(
    val requiredPermissions: List<String>,
    val shouldShowPermissionRationale: Boolean
): Parcelable
