package com.kieronquinn.app.ambientmusicmod.model.shards

import com.google.gson.annotations.SerializedName

data class CachedShardManifest(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("shard_manifest")
    val shardManifest: ShardManifest
)