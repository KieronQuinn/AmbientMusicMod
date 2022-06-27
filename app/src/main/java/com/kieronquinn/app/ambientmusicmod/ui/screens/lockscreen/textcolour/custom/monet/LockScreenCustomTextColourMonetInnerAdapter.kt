package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenCustomTextColourMonetColourBinding
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetInnerAdapter.ViewHolder
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetViewModel.Colour
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isColorDark
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import kotlinx.coroutines.flow.collect

class LockScreenCustomTextColourMonetInnerAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<Colour>,
    private val onColourClicked: (colour: Int) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemLockscreenCustomTextColourMonetColourBinding.inflate(
            layoutInflater, parent, false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        val isColourDark = item.colour.isColorDark()
        colourBackground.foregroundTintList = ColorStateList.valueOf(
            if(isColourDark) Color.WHITE else Color.BLACK
        )
        colourBackground.backgroundTintList = ColorStateList.valueOf(item.colour)
        colourCheck.isVisible = item.isSelected
        colourCheck.imageTintList = ColorStateList.valueOf(
            if(isColourDark) Color.WHITE else Color.BLACK
        )
        holder.lifecycleScope.launchWhenResumed {
            colourBackground.onClicked().collect {
                onColourClicked(item.colour)
            }
        }
        Unit
    }

    data class ViewHolder(val binding: ItemLockscreenCustomTextColourMonetColourBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}