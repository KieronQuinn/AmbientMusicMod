package com.kieronquinn.app.ambientmusicmod.model.recognition

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_GSB
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isAppInstalled

sealed class Player(
    open val url: String,
    @DrawableRes val icon: Int,
    @StringRes val name: Int,
    @ColorRes val chipColour: Int,
    @ColorRes val chipTextColour: Int,
    val packageName: String
) {

    companion object {

        private const val PREFIX_SPOTIFY = "android-app://com.spotify.music"
        private const val PREFIX_YOUTUBE = "https://youtube.com"
        private const val PREFIX_YOUTUBE_MUSIC = "https://music.youtube.com"
        private const val PREFIX_APPLE_MUSIC = "https://music.apple.com"
        private const val PREFIX_DEEZER = "https://www.deezer.com"
        private const val PREFIX_GOOGLE = "/g/"

        fun getPlayers(
            context: Context,
            urls: Array<String>,
            googleId: String?,
            trackName: String,
            artist: String
        ): List<Player> {
            val players = ArrayList<Player>()
            val packageManager = context.packageManager
            if(googleId?.isValid() == true && packageManager.isAppInstalled(PACKAGE_NAME_GSB)){
                players.add(Assistant(googleId, trackName, artist))
            }
            urls.forEach {
                getPlayerForUrl(it)?.let { player ->
                    if(!packageManager.isAppInstalled(player.packageName)) return@forEach
                    players.add(player)
                }
            }
            return players
        }

        private fun getPlayerForUrl(url: String): Player? {
            return when {
                url.startsWith(PREFIX_APPLE_MUSIC) -> AppleMusic(url)
                url.startsWith(PREFIX_DEEZER) -> Deezer(url)
                url.startsWith(PREFIX_SPOTIFY) -> Spotify(url)
                url.startsWith(PREFIX_YOUTUBE) -> YouTube(url)
                url.startsWith(PREFIX_YOUTUBE_MUSIC) -> YouTubeMusic(url)
                else -> null
            }
        }

        /**
         *  /m/ URLs are no longer valid for Assistant, so Chips should not be shown for them
         */
        private fun String.isValid(): Boolean {
            return startsWith(PREFIX_GOOGLE)
        }
    }

    open fun getIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
    }

    data class Assistant(
        override val url: String,
        val trackName: String,
        val artist: String
    ): Player(
        url,
        R.drawable.ic_chip_player_assistant,
        R.string.recognition_chip_assistant,
        R.color.recognition_chip_assistant,
        R.color.recognition_chip_text_assistant,
        PACKAGE_NAME_GSB
    ) {

        override fun getIntent(): Intent {
            return Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH").apply {
                putExtra("android.soundsearch.extra.RECOGNIZED_TRACK_MID", url)
                putExtra("android.soundsearch.extra.RECOGNIZED_TITLE", trackName)
                putExtra("android.soundsearch.extra.RECOGNIZED_ARTIST", artist)
            }
        }

    }

    data class Spotify(override val url: String): Player(
        url,
        R.drawable.ic_chip_player_spotify,
        R.string.recognition_chip_spotify,
        R.color.recognition_chip_spotify,
        R.color.recognition_chip_text_spotify,
        "com.spotify.music"
    )

    data class YouTube(override val url: String): Player(
        url,
        R.drawable.ic_chip_player_youtube,
        R.string.recognition_chip_youtube,
        R.color.recognition_chip_youtube,
        R.color.recognition_chip_text_youtube,
        "com.google.android.youtube"
    )

    data class YouTubeMusic(override val url: String): Player(
        url,
        R.drawable.ic_chip_player_youtube_music,
        R.string.recognition_chip_youtube_music,
        R.color.recognition_chip_youtube_music,
        R.color.recognition_chip_text_youtube_music,
        "com.google.android.apps.youtube.music"
    )

    data class AppleMusic(override val url: String): Player(
        url,
        R.drawable.ic_chip_player_apple_music,
        R.string.recognition_chip_apple_music,
        R.color.recognition_chip_apple_music,
        R.color.recognition_chip_text_apple_music,
        "com.apple.android.music"
    )

    data class Deezer(override val url: String): Player(
        url,
        R.drawable.ic_chip_player_deezer,
        R.string.recognition_chip_deezer,
        R.color.recognition_chip_deezer,
        R.color.recognition_chip_text_deezer,
        "deezer.android.app"
    )

}
