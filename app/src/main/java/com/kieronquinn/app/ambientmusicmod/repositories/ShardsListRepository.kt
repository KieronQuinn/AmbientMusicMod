package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.net.Uri
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository.GetState
import com.kieronquinn.app.ambientmusicmod.utils.extensions.map
import com.kieronquinn.app.ambientmusicmod.utils.extensions.runOnClose
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeQuery
import com.kieronquinn.app.ambientmusicmod.utils.extensions.toStringArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.koin.core.scope.Scope

interface ShardsListRepository {

    val tracks: StateFlow<GetState>

    sealed class GetState {
        object Querying: GetState()
        data class Loading(val current: Int, val total: Int): GetState()
        object Merging: GetState()
        data class Loaded(val tracks: List<ShardTrack>): GetState()
    }

}

class ShardsListRepositoryImpl(
    context: Context,
    private val deviceConfigRepository: DeviceConfigRepository,
    koinScope: Scope
): ShardsListRepository {

    companion object {
        private const val AUTHORITY = "com.google.android.as.pam.ambientmusic.leveldbprovider"
        private const val SCHEME = "content"
        private const val METHOD_LIST = "list"
        private const val METHOD_GET = "get"
        private const val METHOD_LINEAR = "linear"
        private const val TYPE_LINEAR_NORMAL = "linear.db"
        private const val TYPE_LINEAR_V3 = "linear_v3.db"

        private const val COLUMN_DB_ID = "db_id"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TRACK_NAME = "track_name"
        private const val COLUMN_ARTIST = "artist"
        private const val COLUMN_GOOGLE_ID = "google_id"
        private const val COLUMN_PLAYERS = "players"
        private const val COLUMN_ALBUM = "album"
        private const val COLUMN_YEAR = "year"
    }

    private val scope = MainScope()

    init {
        koinScope.runOnClose {
            scope.cancel()
        }
    }

    private val contentResolver = context.contentResolver

    override val tracks = flow {
        val shards = getShards()
        val count = shards.size + 1
        val tracks = shards.mapIndexed { index, name ->
            emit(GetState.Loading(index + 1, count))
            getTracksForShard(name)
        }.toMutableList()
        emit(GetState.Loading(shards.size, count))
        tracks.add(getLinear())
        emit(GetState.Merging)
        emit(GetState.Loaded(tracks.merge()))
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, GetState.Querying)

    private fun getShards(): List<String> {
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_LIST)
        }.build()
        val cursor = contentResolver.safeQuery(
            uri, null, null, null, null
        ) ?: return emptyList()
        val shards = cursor.map {
            it.getString(0)
        }
        cursor.close()
        return shards
    }

    private fun getTracksForShard(name: String): List<ShardTrack> {
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_GET)
            appendPath(name)
        }.build()
        return getTracksForUri(uri, false)
    }

    private suspend fun getLinear(): List<ShardTrack> {
        val linearName = if(deviceConfigRepository.nnfpv3Enabled.get()){
            TYPE_LINEAR_V3
        }else{
            TYPE_LINEAR_NORMAL
        }
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_LINEAR)
            appendPath(linearName)
        }.build()
        return getTracksForUri(uri, true)
    }

    private fun getTracksForUri(uri: Uri, isLinear: Boolean): List<ShardTrack> {
        val cursor = contentResolver.safeQuery(
            uri, null, null, null, null
        ) ?: return emptyList()
        val columnDbId = cursor.getColumnIndex(COLUMN_DB_ID)
        val columnId = cursor.getColumnIndex(COLUMN_ID)
        val columnTrackName = cursor.getColumnIndex(COLUMN_TRACK_NAME)
        val columnArtist = cursor.getColumnIndex(COLUMN_ARTIST)
        val columnGoogleId = cursor.getColumnIndex(COLUMN_GOOGLE_ID)
        val columnPlayers = cursor.getColumnIndex(COLUMN_PLAYERS)
        val columnAlbum = cursor.getColumnIndex(COLUMN_ALBUM)
        val columnYear = cursor.getColumnIndex(COLUMN_YEAR)
        return cursor.map {
            ShardTrack(
                it.getString(columnDbId),
                it.getString(columnId),
                it.getString(columnTrackName),
                it.getString(columnArtist),
                it.getString(columnGoogleId),
                it.getString(columnPlayers).parsePlayers(),
                it.getString(columnAlbum),
                it.getInt(columnYear),
                isLinear
            )
        }.also {
            cursor.close()
        }
    }

    private fun String.parsePlayers(): Array<String> {
        if(isEmpty()) return emptyArray()
        return JSONArray(this).toStringArray()
    }

    private fun List<List<ShardTrack>>.merge(): List<ShardTrack> {
        //Merge the lists, then group by the shared ID and create the best track
        return flatten().groupBy {
            it.dbId
        }.map {
            it.value.createBest()
        }
    }

    private fun List<ShardTrack>.createBest(): ShardTrack {
        return ShardTrack(
            first().dbId,
            first().id,
            first().trackName,
            first().artist,
            first().googleId,
            firstOrNull { it.playerUrls.isNotEmpty() }?.playerUrls ?: emptyArray(),
            firstOrNull { it.album != null }?.album,
            firstOrNull { it.year != null }?.year,
            any { it.isLinear }
        )
    }

}