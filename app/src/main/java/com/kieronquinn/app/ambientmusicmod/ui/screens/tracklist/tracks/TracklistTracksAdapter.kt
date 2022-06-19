package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks

import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTracklistBinding
import com.kieronquinn.app.ambientmusicmod.model.shards.ShardTrack
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistAdapter
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.removeRipple
import kotlinx.coroutines.flow.collect

class TracklistTracksAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override val onOnDemandClicked: (() -> Unit)?,
    override var items: List<ShardTrack>
): GenericTracklistAdapter<ShardTrack>(recyclerView, onOnDemandClicked, items) {

    override fun setupView(
        item: ShardTrack,
        binding: ItemTracklistBinding,
        lifecycleScope: LifecycleCoroutineScope
    ) = with(binding) {
        itemTracklistTitle.text = item.trackName
        itemTracklistContent.text = item.artist
        itemTracklistIcon.setImageResource(R.drawable.ic_nav_tracklist_tracks)
        itemTracklistOnDemand.isVisible = item.isLinear
        lifecycleScope.launchWhenResumed {
            itemTracklistOnDemand.onClicked().collect {
                onOnDemandClicked?.invoke()
            }
        }
        root.removeRipple()
    }

}