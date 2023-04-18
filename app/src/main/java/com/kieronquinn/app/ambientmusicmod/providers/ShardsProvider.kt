package com.kieronquinn.app.ambientmusicmod.providers

import com.kieronquinn.app.ambientmusicmod.model.shards.ShardManifest
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ShardsProvider {

    companion object {
        fun getShardsProvider(): ShardsProvider = Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShardsProvider::class.java)
    }

    @GET("KieronQuinn/AmbientMusicManifest/main/release.json")
    fun getShardsManifest(): Call<ShardManifest>

    @GET("KieronQuinn/AmbientMusicManifest/main/release_v3.json")
    fun getShardsManifestV3(): Call<ShardManifest>

}