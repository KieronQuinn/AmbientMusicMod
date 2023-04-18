package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.trackinfo

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTrackInfoCountryBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTrackInfoPlayersBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.recognition.RecognitionChipsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.trackinfo.TrackInfoBottomSheetViewModel.TrackInfoSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView

class TrackInfoBottomSheetAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<TrackInfoSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            TrackInfoSettingsItem.ItemType.COUNTRY -> TrackInfoViewHolder.Country(
                ItemTrackInfoCountryBinding.inflate(layoutInflater, parent, false)
            )
            TrackInfoSettingsItem.ItemType.PLAYERS -> TrackInfoViewHolder.Players(
                ItemTrackInfoPlayersBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is TrackInfoViewHolder.Players -> {
                val item = items[position] as TrackInfoSettingsItem.Players
                holder.setup(item)
            }
            is TrackInfoViewHolder.Country -> {
                val item = items[position] as TrackInfoSettingsItem.Country
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun TrackInfoViewHolder.Players.setup(
        item: TrackInfoSettingsItem.Players
    ) = with(binding.root) {
        adapter = RecognitionChipsAdapter(item.players, item.onChipClicked, this)
        layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
        }
    }

    private fun TrackInfoViewHolder.Country.setup(
        item: TrackInfoSettingsItem.Country
    ) = with(binding) {
        itemTrackInfoCountryTitle.setText(item.title)
        itemTrackInfoCountryContent.setText(item.subtitle)
        itemTrackInfoCountryIcon.setImageResource(item.icon)
        itemTrackInfoCountryIcon.clipToOutline = true
    }

    sealed class TrackInfoViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Country(override val binding: ItemTrackInfoCountryBinding):
            TrackInfoViewHolder(binding)
        data class Players(override val binding: ItemTrackInfoPlayersBinding):
            TrackInfoViewHolder(binding)
    }

}