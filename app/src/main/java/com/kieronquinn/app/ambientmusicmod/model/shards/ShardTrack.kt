package com.kieronquinn.app.ambientmusicmod.model.shards

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class ShardTrack(
    @PrimaryKey
    @ColumnInfo(name = "db_id")
    val dbId: String,
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "track_name")
    val trackName: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "google_id")
    val googleId: String,
    @ColumnInfo(name = "player_urls")
    val playerUrls: Array<String>,
    @ColumnInfo(name = "album")
    val album: String?,
    @ColumnInfo(name = "year")
    val year: Int?,
    @ColumnInfo(name = "is_linear")
    val isLinear: Boolean,
    @ColumnInfo(name = "database")
    val database: String?
): Parcelable {

    fun sharedName(): String {
        return "$trackName:$artist:$isLinear"
    }

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
