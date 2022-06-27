package com.kieronquinn.app.ambientmusicmod.model.update

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.ambientmusicmod.model.github.GitHubRelease

data class CachedGitHubRelease(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("release")
    val release: GitHubRelease
)