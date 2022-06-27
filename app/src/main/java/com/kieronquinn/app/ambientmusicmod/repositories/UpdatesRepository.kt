package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import com.google.gson.Gson
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.model.github.GitHubRelease
import com.kieronquinn.app.ambientmusicmod.model.update.CachedGitHubRelease
import com.kieronquinn.app.ambientmusicmod.providers.GitHubProvider
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.Instant

interface UpdatesRepository {

    val containerCheckUpdatesBus: Flow<Long>

    fun getUpdatesFolder(context: Context): File
    fun clearUpdatesFolder(folder: File)

    suspend fun containerCheckForUpdates()

    suspend fun isAnyUpdateAvailable(): Boolean

    suspend fun getPAMUpdateState(
        ignoreLocal: Boolean = false,
        ignoreCache: Boolean = false
    ): UpdateState

    suspend fun getAMMUpdateState(
        ignoreCache: Boolean = false
    ): UpdateState

    sealed class UpdateState {
        data class NotInstalled(
            val remoteVersion: String,
            val release: GitHubRelease
        ): UpdateState()
        object FailedToFetchUpdate: UpdateState()
        object FailedToFetchInitial: UpdateState()
        data class UpToDate(
            val localVersion: String
        ): UpdateState()
        data class UpdateAvailable(
            val localVersion: String,
            val remoteVersion: String,
            val release: GitHubRelease
        ): UpdateState()
    }

}

class UpdatesRepositoryImpl(context: Context): UpdatesRepository {

    companion object {
        private val CACHE_TIMEOUT = Duration.ofHours(12).toMillis()
    }

    private val packageManager = context.packageManager

    private val updatesCacheDir = File(context.cacheDir, "updates").apply {
        mkdirs()
    }

    private val gson = Gson()

    private val pamProvider = GitHubProvider.getGitHubProvider("NowPlaying")
    private val ammProvider = GitHubProvider.getGitHubProvider("AmbientMusicMod")

    override val containerCheckUpdatesBus = MutableStateFlow(System.currentTimeMillis())

    override fun getUpdatesFolder(context: Context): File {
        return File(context.externalCacheDir, "updates").apply {
            mkdirs()
        }
    }

    override fun clearUpdatesFolder(folder: File) {
        if(!folder.exists()) return //Already deleted
        folder.listFiles()?.forEach { it.delete() }
    }

    override suspend fun containerCheckForUpdates() {
        containerCheckUpdatesBus.emit(System.currentTimeMillis())
    }

    override suspend fun getPAMUpdateState(
        ignoreLocal: Boolean,
        ignoreCache: Boolean
    ): UpdateState = withContext(
        Dispatchers.IO
    ) {
        val localAsiVersion = try {
            packageManager.getPackageInfo(PACKAGE_NAME_PAM, 0).versionName
        }catch (e: Exception){
            null
        }
        getRelease(localAsiVersion, pamProvider, "pam", ignoreCache)
    }

    override suspend fun getAMMUpdateState(ignoreCache: Boolean): UpdateState = withContext(
        Dispatchers.IO
    ) {
        getRelease(BuildConfig.TAG_NAME, ammProvider, "amm", ignoreCache)
    }

    override suspend fun isAnyUpdateAvailable(): Boolean {
        val pam = getPAMUpdateState()
        val amm = getAMMUpdateState()
        return when {
            pam is UpdateState.UpdateAvailable -> true
            amm is UpdateState.UpdateAvailable -> true
            pam is UpdateState.NotInstalled -> true
            amm is UpdateState.NotInstalled -> true
            else -> false
        }
    }

    private fun getRelease(
        localVersion: String?,
        provider: GitHubProvider,
        repository: String,
        ignoreCache: Boolean = false
    ): UpdateState {
        val cachedRelease = if(ignoreCache) null else getUpdateCache(repository)
        val remoteRelease = cachedRelease ?: provider.getCurrentRelease()?.also {
            it.cacheRelease(repository)
        }
        val remoteVersion = remoteRelease?.tag
        return when {
            localVersion != null && remoteRelease != null && remoteVersion != null &&
                    remoteVersion != localVersion -> {
                UpdateState.UpdateAvailable(
                    localVersion,
                    remoteVersion,
                    remoteRelease
                )
            }
            localVersion == null && remoteRelease !=  null && remoteVersion != null -> {
                UpdateState.NotInstalled(remoteVersion, remoteRelease)
            }
            localVersion != null && remoteVersion != null && localVersion == remoteVersion -> {
                UpdateState.UpToDate(localVersion)
            }
            remoteVersion == null && localVersion != null -> {
                UpdateState.FailedToFetchUpdate
            }
            else -> UpdateState.FailedToFetchInitial
        }
    }

    private fun GitHubProvider.getCurrentRelease(): GitHubRelease? {
        val releases = try {
            getReleases().execute().body()
        }catch (e: Exception){
            null
        } ?: return null
        return releases.firstOrNull()
    }

    private fun getUpdateCache(repository: String): GitHubRelease? {
        val file = File(updatesCacheDir, repository)
        if(!file.exists()) return null
        val cachedRelease = try {
            gson.fromJson(file.readText(), CachedGitHubRelease::class.java)
        }catch (e: Exception){
            null
        } ?: return null
        val cacheAge = Duration.between(
            Instant.ofEpochMilli(cachedRelease.timestamp), Instant.now()
        ).toMillis()
        if(cacheAge > CACHE_TIMEOUT) return null
        return cachedRelease.release
    }

    private fun GitHubRelease.cacheRelease(repository: String) {
        val file = File(updatesCacheDir, repository)
        val cachedRelease = gson.toJson(CachedGitHubRelease(System.currentTimeMillis(), this))
        file.writeText(cachedRelease)
    }

}