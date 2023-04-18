package com.kieronquinn.app.ambientmusicmod.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.utils.room.GsonConverter
import java.io.File

@Database(entities = [
    ShardTrack::class
], version = 1, exportSchema = false)
@TypeConverters(GsonConverter::class)
abstract class ShardTrackCacheDatabase: RoomDatabase() {

    companion object {
        fun getDatabase(context: Context): ShardTrackCacheDatabase {
            return Room.databaseBuilder(
                context,
                ShardTrackCacheDatabase::class.java,
                context.getShardsDatabase().absolutePath
            ).build()
        }

        fun exists(context: Context): Boolean {
            return context.getShardsDatabase().exists()
        }

        private fun Context.getShardsDatabase(): File {
            return File(cacheDir, "shard_track_cache.db")
        }
    }

    abstract fun shardTrackDao(): ShardTrackDao

}

