package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists

import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTracklistBinding
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.TracklistArtistsViewModel.ShardArtist
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistAdapter
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed

class TracklistArtistsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val onArtistClicked: (String) -> Unit,
    override var items: List<ShardArtist>
): GenericTracklistAdapter<ShardArtist>(recyclerView, null, null, items) {

    override fun setupView(
        item: ShardArtist,
        binding: ItemTracklistBinding,
        holder: ViewHolder
    ) = with(binding) {
        itemTracklistTitle.text = item.name
        itemTracklistContent.text = root.context.resources.getQuantityString(
            R.plurals.tracklist_item_artist_content, item.tracks.size, item.tracks.size
        )
        itemTracklistIcon.setImageResource(R.drawable.ic_nav_tracklist_artists)
        holder.whenResumed {
            root.onClicked().collect {
                onArtistClicked(item.name)
            }
        }
        Unit
    }

}