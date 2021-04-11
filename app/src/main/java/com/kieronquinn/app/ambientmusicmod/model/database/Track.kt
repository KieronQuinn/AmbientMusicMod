package com.kieronquinn.app.ambientmusicmod.model.database

import java.util.*

data class Track(val track: String, val artist: String) {

    val formattedKey: String = track.toLowerCase(Locale.getDefault()).trim() + artist.toLowerCase(Locale.getDefault()).trim()

}
