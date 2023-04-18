package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.trackinfo

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.TracklistNavigation
import com.kieronquinn.app.ambientmusicmod.model.recognition.Player
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository
import kotlinx.coroutines.launch

abstract class TrackInfoBottomSheetViewModel: ViewModel() {

    abstract fun supportsEdit(): Boolean
    abstract fun onCloseClicked()
    abstract fun onEditClicked(track: ShardTrack, isFromArtists: Boolean)
    abstract fun onChipClicked(intent: Intent)

    sealed class TrackInfoSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Country(
            @StringRes
            val title: Int,
            @StringRes
            val subtitle: Int,
            @DrawableRes
            val icon: Int
        ): TrackInfoSettingsItem(ItemType.COUNTRY)

        data class Players(
            val players: List<Player>,
            val onChipClicked: (Intent) -> Unit
        ): TrackInfoSettingsItem(ItemType.PLAYERS)

        enum class ItemType: BaseSettingsItemType {
            COUNTRY, PLAYERS
        }

    }

}

class TrackInfoBottomSheetViewModelImpl(
    private val tracklistNavigation: TracklistNavigation,
    private val updatesRepository: UpdatesRepository
): TrackInfoBottomSheetViewModel() {

    override fun supportsEdit(): Boolean {
        return updatesRepository.doesPAMSupportSummaryAndEditing()
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            tracklistNavigation.navigateBack()
        }
    }

    override fun onEditClicked(track: ShardTrack, isFromArtists: Boolean) {
        viewModelScope.launch {
            //Colliding nav directions means we have to do this manually
            val arguments = bundleOf(
                "track" to track
            )
            val directions = if(isFromArtists){
                R.id.action_trackInfoBottomSheetFragment_to_trackEditFragment2
            }else{
                R.id.action_trackInfoBottomSheetFragment2_to_trackEditFragment
            }
            tracklistNavigation.navigate(directions, arguments)
        }
    }

    override fun onChipClicked(intent: Intent) {
        viewModelScope.launch {
            tracklistNavigation.navigate(intent)
        }
    }

}