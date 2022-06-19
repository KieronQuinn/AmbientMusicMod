package com.kieronquinn.app.ambientmusicmod.model.update

import android.os.Parcelable
import com.kieronquinn.app.ambientmusicmod.model.github.GitHubRelease
import kotlinx.parcelize.Parcelize

@Parcelize
data class Release(
    val title: String,
    val tag: String,
    val versionName: String,
    val installedVersion: String?,
    val downloadUrl: String,
    val fileName: String,
    val gitHubUrl: String,
    val body: String
) : Parcelable

private const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"

fun GitHubRelease.toRelease(title: String, localVersion: String?): Release? {
    val versionName = versionName ?: return null
    val asset = assets?.firstOrNull { it.contentType == CONTENT_TYPE_APK } ?: return null
    val downloadUrl = asset.downloadUrl ?: return null
    val fileName = asset.fileName ?: return null
    val gitHubUrl = gitHubUrl ?: return null
    val body = body ?: return null
    return Release(title, tag!!, versionName, localVersion, downloadUrl, fileName, gitHubUrl, body)
}
