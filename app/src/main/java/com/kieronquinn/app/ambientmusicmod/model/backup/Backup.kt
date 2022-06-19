package com.kieronquinn.app.ambientmusicmod.model.backup

import com.google.gson.annotations.SerializedName

data class Backup(
    @SerializedName("version")
    val version: Int = VERSION,
    @SerializedName("history")
    val history: List<HistoryItem>,
    @SerializedName("linear")
    val linear: List<LinearItem>,
    @SerializedName("linear_v3")
    val linearv3: List<LinearItem>,
    @SerializedName("favourites")
    val favourites: List<FavouritesItem>,
    @SerializedName("settings")
    val settingsBackup: SettingsBackup
) {

    companion object {
        const val VERSION = 1
    }

}
