package com.kieronquinn.app.ambientmusicmod.model.backup

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import com.google.gson.annotations.SerializedName

data class FavouritesItem(
    @SerializedName(COLUMN_TRACK_ID)
    val trackId: String,
    @SerializedName(COLUMN_TIMESTAMP)
    val timestamp: Long
) {

    companion object {
        private const val COLUMN_TRACK_ID = "track_id"
        private const val COLUMN_TIMESTAMP = "timestamp"

        @SuppressLint("Range")
        fun fromCursor(cursor: Cursor): FavouritesItem = with(cursor) {
            return FavouritesItem(
                getString(getColumnIndex(COLUMN_TRACK_ID)),
                getLong(getColumnIndex(COLUMN_TIMESTAMP))
            )
        }

    }

    fun toContentValues(): ContentValues {
        return ContentValues().apply {
            put(COLUMN_TRACK_ID, trackId)
            put(COLUMN_TIMESTAMP, timestamp)
        }
    }

}