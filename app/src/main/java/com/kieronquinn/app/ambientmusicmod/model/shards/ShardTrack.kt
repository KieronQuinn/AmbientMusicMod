package com.kieronquinn.app.ambientmusicmod.model.shards

data class ShardTrack(
    val dbId: String,
    val id: String,
    val trackName: String,
    val artist: String,
    val googleId: String,
    val playerUrls: Array<String>,
    val album: String?,
    val year: Int?,
    val isLinear: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShardTrack

        if (dbId != other.dbId) return false
        if (id != other.id) return false
        if (trackName != other.trackName) return false
        if (artist != other.artist) return false
        if (googleId != other.googleId) return false
        if (!playerUrls.contentEquals(other.playerUrls)) return false
        if (album != other.album) return false
        if (year != other.year) return false
        if (isLinear != other.isLinear) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dbId.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + trackName.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + googleId.hashCode()
        result = 31 * result + playerUrls.contentHashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (year ?: 0)
        result = 31 * result + isLinear.hashCode()
        return result
    }
}
