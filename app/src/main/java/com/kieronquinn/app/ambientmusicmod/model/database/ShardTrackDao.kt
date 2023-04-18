package com.kieronquinn.app.ambientmusicmod.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack

@Dao
interface ShardTrackDao {

    @Query("select * from ShardTrack")
    fun getAll(): List<ShardTrack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(shardTracks: List<ShardTrack>)

    @Query("delete from ShardTrack")
    fun clear()

}