package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.ItemTracklistBinding
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.monetcompat.core.MonetCompat

abstract class GenericTracklistAdapter<T>(
    recyclerView: LifecycleAwareRecyclerView,
    protected open val onOnDemandClicked: (() -> Unit)?,
    open var items: List<T>
): LifecycleAwareRecyclerView.Adapter<GenericTracklistAdapter.ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTracklistBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        setupView(items[position], holder.binding, holder.lifecycleScope)
        with(holder.binding){
            itemTracklistIcon.backgroundTintList = ColorStateList.valueOf(
                monet.getPrimaryColor(root.context, !root.context.isDarkMode)
            )
            itemTracklistIcon.imageTintList = ColorStateList.valueOf(
                monet.getAccentColor(root.context)
            )
        }
    }

    abstract fun setupView(
        item: T,
        binding: ItemTracklistBinding,
        lifecycleScope: LifecycleCoroutineScope
    )

    data class ViewHolder(val binding: ItemTracklistBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}