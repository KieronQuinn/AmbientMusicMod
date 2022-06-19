package com.kieronquinn.app.ambientmusicmod.model.shards

import com.google.gson.annotations.SerializedName

data class ShardManifest(
    @SerializedName("url")
    val url: String,
    @SerializedName("versionCode")
    val versionCode: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("time")
    val time: String
)