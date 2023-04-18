package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks

import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTracklistBinding
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistAdapter
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed

class TracklistTracksAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override val onOnDemandClicked: (() -> Unit)?,
    override val onTrackClicked: ((ShardTrack) -> Unit)?,
    override var items: List<ShardTrack>
): GenericTracklistAdapter<ShardTrack>(recyclerView, onOnDemandClicked, onTrackClicked, items) {

    override fun setupView(
        item: ShardTrack,
        binding: ItemTracklistBinding,
        holder: ViewHolder
    ) = with(binding) {
        itemTracklistTitle.text = item.trackName
        itemTracklistContent.text = item.artist
        itemTracklistIcon.setImageResource(R.drawable.ic_nav_tracklist_tracks)
        itemTracklistOnDemand.isVisible = item.isLinear
        holder.whenResumed {
            itemTracklistOnDemand.onClicked().collect {
                onOnDemandClicked?.invoke()
            }
        }
        holder.whenResumed {
            root.onClicked().collect {
                onTrackClicked?.invoke(item)
            }
        }
        Unit
    }

}