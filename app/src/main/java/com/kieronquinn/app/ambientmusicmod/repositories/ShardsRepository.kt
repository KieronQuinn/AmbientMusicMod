package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.shards.CachedShardManifest
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardManifest
import com.kieronquinn.app.ambientmusicmod.providers.ShardsProvider
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.LocalShardsState
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.RemoteShardsState
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardCountry
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardsState
import com.kieronquinn.app.ambientmusicmod.utils.extensions.contentReceiverAsFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.contentResolverAsTFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isArmv7
import com.kieronquinn.app.ambientmusicmod.utils.extensions.map
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface ShardsRepository {

    suspend fun isUpdateAvailable(clearCache: Boolean = false): Boolean
    fun getCurrentDownloads(): Flow<Int>
    fun getShardsState(clearCache: Boolean = false): Flow<ShardsState>

    enum class ShardDownloadState {
        DOWNLOADING, WAITING_FOR_NETWORK, WAITING_FOR_CHARGING
    }

    data class ShardsState(
        val local: LocalShardsState,
        val remote: RemoteShardsState?,
        val updateAvailable: Boolean,
        var downloadState: ShardDownloadState? = null
    )

    data class LocalShardsState(
        val versionCode: Int,
        val date: LocalDateTime,
        val selectedCountries: List<ShardCountry>,
        val trackCount: Int,
        val personalisedTrackCount: Int
    )

    data class RemoteShardsState(
        val versionCode: Int,
        val date: LocalDateTime,
        val url: String
    )

    /**
     *  List of countries in the Shards manifest. New countries must be added manually.
     */
    enum class ShardCountry(
        val code: String,
        @StringRes val countryName: Int,
        @DrawableRes val icon: Int
    ) {
        AR("ar", R.string.shard_country_argentina, R.drawable.ic_flag_ar),
        AU("au", R.string.shard_country_australia, R.drawable.ic_flag_au),
        BR("br", R.string.shard_country_brazil, R.drawable.ic_flag_br),
        CA("ca", R.string.shard_country_canada, R.drawable.ic_flag_ca),
        CH("ch", R.string.shard_country_switzerland, R.drawable.ic_flag_ch),
        DE("de", R.string.shard_country_germany, R.drawable.ic_flag_de),
        ES("es", R.string.shard_country_spain, R.drawable.ic_flag_es),
        FR("fr", R.string.shard_country_france, R.drawable.ic_flag_fr),
        GB("gb", R.string.shard_country_united_kingdom, R.drawable.ic_flag_gb),
        IE("ie", R.string.shard_country_ireland, R.drawable.ic_flag_ie),
        IN("in", R.string.shard_country_india, R.drawable.ic_flag_in),
        IT("it", R.string.shard_country_italy, R.drawable.ic_flag_it),
        JP("jp", R.string.shard_country_japan, R.drawable.ic_flag_jp),
        MX("mx", R.string.shard_country_mexico, R.drawable.ic_flag_mx),
        NL("nl", R.string.shard_country_netherlands, R.drawable.ic_flag_nl),
        RU("ru", R.string.shard_country_russia, R.drawable.ic_flag_ru),
        US("us,xa", R.string.shard_country_united_states, R.drawable.ic_flag_us);

        companion object {
            const val CORE_SHARED_FILENAME = "matcher_tah.leveldb"

            fun takeIfCountry(code: String): String? {
                return if(values().any { it.codes().contains(code.lowercase()) }) code else null
            }

            fun forCode(code: String): ShardCountry {
                return values().first { it.codes().contains(code.lowercase()) }
            }
        }

        fun codes(): List<String> {
            return if(code.contains(",")){
                listOf(code) + code.split(",")
            }else listOf(code)
        }
    }

}

class ShardsRepositoryImpl(
    private val gson: Gson,
    private val context: Context,
    private val deviceConfigRepository: DeviceConfigRepository
): ShardsRepository {

    companion object {
        private val REGEX_INDEX_URLS = setOf(
            "(.*):https://storage.googleapis.com/music-iq-db/updatable_db_v3/(.*)-(.*)/manifest.json"
                .toRegex(),
            "(.*):https://storage.googleapis.com/music-iq-db/updatable_ytm_db/(.*)-(.*)/manifest.json"
                .toRegex()
        )

        private const val AUTHORITY = "com.google.android.as.pam.ambientmusic.leveldbprovider"
        private const val SCHEME = "content"
        private const val METHOD_COUNT = "count"
        private const val METHOD_COUNT_LEVELDB = "leveldb"
        private const val METHOD_COUNT_LINEAR = "linear"
        private const val METHOD_DOWNLOAD_STATE = "downloadstate"
        private const val TYPE_LINEAR_NORMAL = "linear.db"
        private const val TYPE_LINEAR_V3 = "linear_v3.db"

        private val CACHE_TIMEOUT = Duration.ofHours(12).toMillis()
        private const val CACHE_FILENAME = "shards_manifest_v3"

        private val URI_DOWNLOAD_STATE = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_DOWNLOAD_STATE)
        }.build()

        val URI_LINEAR = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_COUNT_LINEAR)
        }.build()
    }

    private val contentResolver = context.contentResolver
    private val shardsProvider = ShardsProvider.getShardsProvider()
    private val scope = MainScope()

    private val updatesCacheDir = File(context.cacheDir, "updates").apply {
        mkdirs()
    }

    private val linearChangeBus = context.contentReceiverAsFlow(URI_LINEAR)
        .map { System.currentTimeMillis() }
        .stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    override fun getCurrentDownloads() = context.contentResolverAsTFlow(URI_DOWNLOAD_STATE) {
        val cursor = contentResolver.safeQuery(
            URI_DOWNLOAD_STATE, null, null, null, null
        ) ?: return@contentResolverAsTFlow 0
        val count = cursor.map {
            it.getInt(0)
        }.firstOrNull() ?: 0
        count.also {
            cursor.close()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun isUpdateAvailable(clearCache: Boolean): Boolean = withContext(Dispatchers.IO) {
        val localShard = deviceConfigRepository.indexManifest.get().getUrlData()
        val remoteShard = getRemoteShards(clearCache).first()
        remoteShard != null && remoteShard.versionCode != localShard.first
    }

    private suspend fun getDatabaseTracksCount(): Int = withContext(Dispatchers.IO) {
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_COUNT)
            appendPath(METHOD_COUNT_LEVELDB)
        }.build()
        val cursor = contentResolver.safeQuery(
            uri, null, null, null, null
        ) ?: return@withContext 0
        val count = cursor.map {
            it.getInt(0)
        }.firstOrNull() ?: 0
        count.also {
            cursor.close()
        }
    }

    private fun getLinearTracksCount() = linearChangeBus.mapLatest {
        val type = if (isArmv7) {
            TYPE_LINEAR_NORMAL
        } else {
            TYPE_LINEAR_V3
        }
        val uri = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path(METHOD_COUNT)
            appendPath(METHOD_COUNT_LINEAR)
            appendPath(type)
        }.build()
        val cursor = contentResolver.safeQuery(
            uri, null, null, null, null
        ) ?: return@mapLatest 0
        val count = cursor.map {
            it.getInt(0)
        }.firstOrNull() ?: 0
        count.also {
            cursor.close()
        }
    }.flowOn(Dispatchers.IO)

    private val localShards = combine(
        deviceConfigRepository.deviceCountry.asFlow(),
        deviceConfigRepository.extraLanguages.asFlow(),
        deviceConfigRepository.indexManifest.asFlow(),
        getLinearTracksCount()
    ) { _, _, index, personalisedTrackCount ->
        val primaryCountry = getPrimaryCountry()
        //If the primary is on automatic and changes, it may invalidate the extras
        val extraCountries = getExtraCountries()
            .filterNot { it == primaryCountry }
            .distinct()
        val trackCount = getDatabaseTracksCount()
        val urlData = index.getUrlData()
        LocalShardsState(
            urlData.first,
            urlData.second,
            listOf(primaryCountry) + extraCountries,
            trackCount,
            personalisedTrackCount
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun getPrimaryCountry(): ShardCountry {
        return ShardCountry.forCode(deviceConfigRepository.getPrimaryLanguage())
    }

    private suspend fun getExtraCountries(): List<ShardCountry> {
        return deviceConfigRepository.getExtraLanguages().map {
            ShardCountry.forCode(it)
        }
    }

    private fun getRemoteShards(ignoreCache: Boolean) = flow {
        val shards = try {
            val cached = if(ignoreCache) null else getShardsCache()
            cached ?: shardsProvider.getCompatibleShardsManifest().execute().body()?.also {
                it.cacheManifest()
            }
        }catch (e: Exception){
            null
        } ?: run {
            emit(null)
            return@flow
        }
        val urlData = shards.url.getUrlData()
        emit(RemoteShardsState(
            urlData.first,
            urlData.second,
            shards.url
        ))
    }.flowOn(Dispatchers.IO)

    private fun ShardsProvider.getCompatibleShardsManifest(): Call<ShardManifest> {
        return if (isArmv7) {
            getShardsManifest()
        } else {
            getShardsManifestV3()
        }
    }

    override fun getShardsState(clearCache: Boolean): Flow<ShardsState> = flow {
        emit(combine(
            localShards, getRemoteShards(clearCache)
        ) { local, remote ->
            val updateAvailable = remote != null && local.versionCode != remote.versionCode
            ShardsState(local, remote, updateAvailable)
        }.first())
    }

    private fun String.getUrlData(): Pair<Int, LocalDateTime> {
        val result = parseShardsUrl() ?: throw RuntimeException("Invalid index URL: $this")
        val version = result.groupValues[1].toInt()
        val date = result.groupValues[2]
        val time = result.groupValues[3]
        val parsedDate = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.systemDefault())
            .parse(date + time)
        val localDate = Instant.from(parsedDate).let {
            LocalDateTime.ofInstant(it, ZoneId.systemDefault())
        }
        return Pair(version, localDate)
    }

    private fun String.parseShardsUrl(): MatchResult? {
        REGEX_INDEX_URLS.forEach {
            if(it.matches(this)){
                return it.find(this)
            }
        }
        return null
    }

    private fun getShardsCache(): ShardManifest? {
        val file = File(updatesCacheDir, CACHE_FILENAME)
        if(!file.exists()) return null
        val cachedShardManifest = try {
            gson.fromJson(file.readText(), CachedShardManifest::class.java)
        }catch (e: Exception){
            null
        } ?: return null
        val cacheAge = Duration.between(
            Instant.ofEpochMilli(cachedShardManifest.timestamp), Instant.now()
        ).toMillis()
        if(cacheAge > CACHE_TIMEOUT) return null
        return cachedShardManifest.shardManifest
    }

    private fun ShardManifest.cacheManifest() {
        val file = File(updatesCacheDir, CACHE_FILENAME)
        val cachedRelease = gson.toJson(CachedShardManifest(System.currentTimeMillis(), this))
        file.writeText(cachedRelease)
    }

}