package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.tracks

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTrackListTrackBinding
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class DatabaseViewerTracksAdapter(private val context: Context, var tracks: List<Track>): RecyclerView.Adapter<DatabaseViewerTracksAdapter.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter {

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getItemCount() = tracks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTrackListTrackBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = tracks[position]
        with(holder.binding){
            itemTrackListTrackTrack.text = item.track
            itemTrackListTrackArtist.text = item.artist
        }
    }

    override fun getSectionName(position: Int): String {
        return tracks[position].track.substring(0, 1)
    }

    data class ViewHolder(val binding: ItemTrackListTrackBinding): RecyclerView.ViewHolder(binding.root)

}