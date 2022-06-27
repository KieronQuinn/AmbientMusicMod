package com.kieronquinn.app.ambientmusicmod.providers

import com.kieronquinn.app.ambientmusicmod.model.github.GitHubRelease
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface GitHubProvider {

    companion object {
        fun getGitHubProvider(repository: String): GitHubProvider = Retrofit.Builder()
            .baseUrl("https://api.github.com/repos/KieronQuinn/$repository/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubProvider::class.java)
    }

    @GET("releases")
    fun getReleases(): Call<Array<GitHubRelease>>

}