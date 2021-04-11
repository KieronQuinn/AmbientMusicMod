package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTrackListArtistBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTrackListTrackBinding
import com.kieronquinn.app.ambientmusicmod.model.database.Artist
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class DatabaseViewerArtistsAdapter(private val context: Context, var artists: List<Artist>, val onArtistClicked: (String) -> Unit): RecyclerView.Adapter<DatabaseViewerArtistsAdapter.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter {

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getItemCount() = artists.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTrackListArtistBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = artists[position]
        with(holder.binding){
            itemTrackListArtist.text = item.name
            root.setOnClickListener {
                onArtistClicked(item.name)
            }
        }
    }

    override fun getSectionName(position: Int): String {
        return artists[position].name.substring(0, 1)
    }

    data class ViewHolder(val binding: ItemTrackListArtistBinding): RecyclerView.ViewHolder(binding.root)

}