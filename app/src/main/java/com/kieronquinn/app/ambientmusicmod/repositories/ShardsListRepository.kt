package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.kieronquinn.app.ambientmusicmod.model.database.ShardTrackCacheDatabase
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsListRepository.GetState
import com.kieronquinn.app.ambientmusicmod.utils.extensions.contentReceiverAsFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isArmv7
import com.kieronquinn.app.ambientmusicmod.utils.extensions.map
import com.kieronquinn.app.ambientmusicmod.utils.extensions.runOnClose
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeDelete
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeQuery
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeUpdate
import com.kieronquinn.app.ambientmusicmod.utils.extensions.toStringArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.koin.core.scope.Scope

interface ShardsListRepository {

    val tracks: StateFlow<GetState>

    suspend fun updateLinearTrack(
        shardTrack: ShardTrack,
        trackName: String,
        artist: String
    ): Boolean
    suspend fun deleteLinearTrack(shardTrack: ShardTrack): Boolean

    sealed class GetState {
        object Querying: GetState()
        data class Loading(val current: Int, val total: Int): GetState()
        object Merging: GetState()
        data class Loaded(val tracks: List<ShardTrack>): GetState()
    }

}

class ShardsListRepositoryImpl(
    private val settingsRepository: SettingsRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val context: Context,
    koinScope: Scope
): ShardsListRepository {

    companion object {
        private const val AUTHORITY = "com.google.android.as.pam.ambientmusic.leveldbprovider"
        private const val SCHEME = "content"
        private const val METHOD_LIST = "list"
        private const val METHOD_GET = "get"
        private const val METHOD_LINEAR = "linear"
        private const val METHOD_HASH = "hash"
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
        private const val COLUMN_DATABASE = "database"
    }

    private val scope = MainScope()

    init {
        koinScope.runOnClose {
            scope.cancel()
        }
    }

    private val contentResolver = context.contentResolver
    private val shardsCacheDatabase = ShardTrackCacheDatabase.getDatabase(context)
    private val shardsTrackDao = shardsCacheDatabase.shardTrackDao()
    private val databaseLock = Mutex()

    override val tracks = getTracks()
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, GetState.Querying)

    private val forceLinearChangeBus = MutableStateFlow(System.currentTimeMillis())
    private val linearChangeBus = context.contentReceiverAsFlow(ShardsRepositoryImpl.URI_LINEAR)
        .map { System.currentTimeMillis() }
        .stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val linearChange = combine(
        forceLinearChangeBus,
        linearChangeBus
    ) { _, _ ->
        System.currentTimeMillis()
    }

    private fun getTracks() = flow {
        if(canUseLocalCache()){
            val cached = shardsCacheDatabase.shardTrackDao().getAll()
            getLinear().collect {
                emit(GetState.Loaded(cached + it))
            }
        }else{
            combine(
                getAndCacheTracks(),
                getLinear()
            ) { tracks, linear ->
                if(tracks is GetState.Loaded) {
                    emit(GetState.Loaded(tracks.tracks + linear))
                }else emit(tracks)
            }.collect()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun canUseLocalCache(): Boolean {
        val localHash = settingsRepository.shardsCacheHash.get()
            .takeIf { it.isNotBlank() }?.toIntOrNull() ?: return false
        if(!ShardTrackCacheDatabase.exists(context)) return false
        val remoteHash = getRemoteHash() ?: return false
        return localHash == remoteHash
    }

    private fun getRemoteHash(): Int? {
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_HASH)
        }.build()
        val cursor = contentResolver.safeQuery(
            uri, null, null, null, null
        ) ?: return null
        val hash = cursor.map {
            it.getInt(0)
        }.firstOrNull()
        cursor.close()
        return hash
    }

    private fun getAndCacheTracks() = flow {
        val shards = getShards()
        val count = shards.size + 1
        val tracks = shards.mapIndexed { index, name ->
            emit(GetState.Loading(index + 1, count))
            getTracksForShard(name)
        }.toMutableList()
        emit(GetState.Loading(shards.size, count))
        emit(GetState.Merging)
        val mergedTracks = tracks.merge()
        val remoteHash = getRemoteHash()
        //Now Playing may be out of date and not return a hash, in which case do not cache
        if(remoteHash != null) {
            mergedTracks.cacheTracks()
            settingsRepository.shardsCacheHash.set(remoteHash.toString())
        }
        emit(GetState.Loaded(mergedTracks))
    }.flowOn(Dispatchers.IO)

    private suspend fun List<ShardTrack>.cacheTracks() = databaseLock.withLock {
        shardsTrackDao.clear()
        shardsTrackDao.insert(this)
    }

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

    private fun getLinear() = linearChange.mapLatest {
        val linearName = if (isArmv7) {
            TYPE_LINEAR_NORMAL
        } else {
            TYPE_LINEAR_V3
        }
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_LINEAR)
            appendPath(linearName)
        }.build()
        getTracksForUri(uri, true)
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
        val columnDatabase = cursor.getColumnIndex(COLUMN_DATABASE)
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
                isLinear,
                if(columnDatabase != -1){
                    it.getString(columnDatabase)
                }else null
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
        //Merge the lists, then group by the shared ID and create the best track, then filter
        return flatten().groupBy {
            it.dbId
        }.map {
            it.value.createBest()
        }.distinctBy {
            it.sharedName()
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
            any { it.isLinear },
            first().database
        )
    }

    override suspend fun updateLinearTrack(
        shardTrack: ShardTrack,
        trackName: String,
        artist: String
    ) = withContext(Dispatchers.IO) {
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_LINEAR)
        }.build()
        val result = contentResolver.safeUpdate(
            uri,
            ContentValues().apply {
                put(COLUMN_TRACK_NAME, trackName)
                put(COLUMN_ARTIST, artist)
            },
            (!isArmv7).toString(),
            arrayOf(shardTrack.dbId)
        ) == 1
        //Delay to allow time to apply and detach
        delay(1000L)
        shizukuServiceRepository.runWithService {
            it.forceStopNowPlaying()
        }
        //Delay to allow a moment to restart
        delay(1000L)
        forceLinearChangeBus.emit(System.currentTimeMillis())
        result
    }

    override suspend fun deleteLinearTrack(
        shardTrack: ShardTrack
    ) = withContext(Dispatchers.IO) {
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_LINEAR)
        }.build()
        val result = contentResolver.safeDelete(
            uri,
            (!isArmv7).toString(),
            arrayOf(shardTrack.dbId)
        ) == 1
        //Delay to allow time to apply and detach
        delay(1000L)
        shizukuServiceRepository.runWithService {
            it.forceStopNowPlaying()
        }
        //Delay to allow a moment to restart
        delay(1000L)
        forceLinearChangeBus.emit(System.currentTimeMillis())
        result
    }

}