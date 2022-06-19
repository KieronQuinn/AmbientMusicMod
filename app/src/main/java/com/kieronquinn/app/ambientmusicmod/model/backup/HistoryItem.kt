package com.kieronquinn.app.ambientmusicmod.model.backup

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getBlobOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.google.gson.annotations.SerializedName

data class HistoryItem(
    @SerializedName(COLUMN_TIMESTAMP)
    val timestamp: Long,
    @SerializedName(COLUMN_HISTORY_ENTRY)
    val historyEntry: ByteArray,
    @SerializedName(COLUMN_TRACK_ID)
    val trackId: String,
    @SerializedName(COLUMN_ARTIST)
    val artist: String,
    @SerializedName(COLUMN_TITLE)
    val title: String,
    @SerializedName(COLUMN_FINGERPRINTS)
    val fingerprints: ByteArray?,
    @SerializedName(COLUMN_SHARDS_REGION)
    val shardsRegion: String?,
    @SerializedName(COLUMN_DOWNLOADED_SHARDS_VERSION)
    val downloadedShardsVersion: Int?,
    @SerializedName(COLUMN_CORE_SHARD_VERSION)
    val coreShardVersion: Int?
) {

    companion object {
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_HISTORY_ENTRY = "history_entry"
        private const val COLUMN_TRACK_ID = "track_id"
        private const val COLUMN_ARTIST = "artist"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_FINGERPRINTS = "fingerprints"
        private const val COLUMN_SHARDS_REGION = "shards_region"
        private const val COLUMN_DOWNLOADED_SHARDS_VERSION = "downloaded_shards_version"
        private const val COLUMN_CORE_SHARD_VERSION = "core_shard_version"

        @SuppressLint("Range")
        fun fromCursor(cursor: Cursor): HistoryItem = with(cursor) {
            return HistoryItem(
                getLong(getColumnIndex(COLUMN_TIMESTAMP)),
                getBlob(getColumnIndex(COLUMN_HISTORY_ENTRY)),
                getString(getColumnIndex(COLUMN_TRACK_ID)),
                getString(getColumnIndex(COLUMN_ARTIST)),
                getString(getColumnIndex(COLUMN_TITLE)),
                getBlobOrNull(getColumnIndex(COLUMN_FINGERPRINTS)),
                getStringOrNull(getColumnIndex(COLUMN_SHARDS_REGION)),
                getIntOrNull(getColumnIndex(COLUMN_DOWNLOADED_SHARDS_VERSION)),
                getIntOrNull(getColumnIndex(COLUMN_CORE_SHARD_VERSION))
            )
        }
    }

    fun toContentValues(): ContentValues {
        return ContentValues().apply {
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_HISTORY_ENTRY, historyEntry)
            put(COLUMN_TRACK_ID, trackId)
            put(COLUMN_ARTIST, artist)
            put(COLUMN_TITLE, title)
            put(COLUMN_FINGERPRINTS, fingerprints)
            put(COLUMN_SHARDS_REGION, shardsRegion)
            put(COLUMN_DOWNLOADED_SHARDS_VERSION, downloadedShardsVersion)
            put(COLUMN_CORE_SHARD_VERSION, coreShardVersion)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryItem

        if (timestamp != other.timestamp) return false
        if (!historyEntry.contentEquals(other.historyEntry)) return false
        if (trackId != other.trackId) return false
        if (artist != other.artist) return false
        if (title != other.title) return false
        if (fingerprints != null) {
            if (other.fingerprints == null) return false
            if (!fingerprints.contentEquals(other.fingerprints)) return false
        } else if (other.fingerprints != null) return false
        if (shardsRegion != other.shardsRegion) return false
        if (downloadedShardsVersion != other.downloadedShardsVersion) return false
        if (coreShardVersion != other.coreShardVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + historyEntry.contentHashCode()
        result = 31 * result + trackId.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (fingerprints?.contentHashCode() ?: 0)
        result = 31 * result + (shardsRegion?.hashCode() ?: 0)
        result = 31 * result + (downloadedShardsVersion ?: 0)
        result = 31 * result + (coreShardVersion ?: 0)
        return result
    }
}