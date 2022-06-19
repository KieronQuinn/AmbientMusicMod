package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists

import androidx.lifecycle.LifecycleCoroutineScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTracklistBinding
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.TracklistArtistsViewModel.ShardArtist
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistAdapter
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import kotlinx.coroutines.flow.collect

class TracklistArtistsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val onArtistClicked: (String) -> Unit,
    override var items: List<ShardArtist>
): GenericTracklistAdapter<ShardArtist>(recyclerView, null, items) {

    override fun setupView(
        item: ShardArtist,
        binding: ItemTracklistBinding,
        lifecycleScope: LifecycleCoroutineScope
    ) = with(binding) {
        itemTracklistTitle.text = item.name
        itemTracklistContent.text = root.context.resources.getQuantityString(
            R.plurals.tracklist_item_artist_content, item.tracks.size, item.tracks.size
        )
        itemTracklistIcon.setImageResource(R.drawable.ic_nav_tracklist_artists)
        lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onArtistClicked(item.name)
            }
        }
        Unit
    }

}