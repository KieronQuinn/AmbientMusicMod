package com.kieronquinn.app.ambientmusicmod.components.github

import android.content.Context
import android.os.Parcelable
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File

class UpdateChecker {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        private const val BASE_URL = "https://api.github.com/repos/KieronQuinn/AmbientMusicMod/"
    }

    var updateAvailable = MutableStateFlow(false)
    var hasDismissedDialog = false

    fun getLatestRelease() = callbackFlow {
        updateAvailable.value = false
        withContext(Dispatchers.IO){
            getReleaseList()?.let { gitHubReleaseResponse ->
                val currentTag = gitHubReleaseResponse.tagName
                if(currentTag != null && currentTag != BuildConfig.TAG_NAME){
                    //New update available!
                    val asset = gitHubReleaseResponse.assets?.firstOrNull { it.name?.endsWith(".apk") == true }
                    val releaseUrl = asset?.browserDownloadUrl?.replace("/download/", "/tag/")?.apply {
                        substring(0, lastIndexOf("/"))
                    }
                    val name = gitHubReleaseResponse.name ?: run {
                        offer(null)
                        return@let
                    }
                    val body = gitHubReleaseResponse.body ?: run {
                        offer(null)
                        return@let
                    }
                    val publishedAt = gitHubReleaseResponse.publishedAt ?: run {
                        offer(null)
                        return@let
                    }
                    offer(Update(name, body, publishedAt, asset?.browserDownloadUrl ?: "https://github.com/KieronQuinn/AmbientMusicMod/releases", asset?.name ?: "AmbientMusicMod.apk", releaseUrl ?: "https://github.com/KieronQuinn/AmbientMusicMod/releases"))
                    updateAvailable.value = true
                }
            } ?: run {
                offer(null)
            }
        }
        awaitClose {  }
    }

    fun clearCachedDownloads(context: Context){
        File(context.externalCacheDir, "updates").deleteRecursively()
    }

    private fun getReleaseList(): GitHubReleaseResponse? {
        val service: GitHubService = retrofit.create(GitHubService::class.java)
        runCatching {
            service.getReleaseList().execute().body()
        }.onSuccess {
            return it
        }.onFailure {
            return null
        }
        return null
    }

    interface GitHubService {
        @GET("releases/latest")
        fun getReleaseList(): Call<GitHubReleaseResponse>
    }

    @Parcelize
    data class Update(val name: String, val changelog: String, val timestamp: String, val assetUrl: String, val assetName: String, val releaseUrl: String): Parcelable

}