package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenCustomTextColourMonetBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetViewModel.LockScreenCustomTextColourMonetSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView

class LockScreenCustomTextColourMonetAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>,
    private val onColourClicked: (colour: Int) -> Unit
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<LockScreenCustomTextColourMonetSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when (itemType) {
            LockScreenCustomTextColourMonetSettingsItem.ItemType.COLOURS -> LockScreenTextColourViewHolder.Colours(
                ItemLockscreenCustomTextColourMonetBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is LockScreenTextColourViewHolder.Colours -> {
                val item = items[position] as LockScreenCustomTextColourMonetSettingsItem.Colours
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun LockScreenTextColourViewHolder.Colours.setup(
        colours: LockScreenCustomTextColourMonetSettingsItem.Colours
    ) = with(binding) {
        customTextColourTitle.setText(colours.label)
        val adapter = LockScreenCustomTextColourMonetInnerAdapter(
            customTextColourInner, colours.colours, onColourClicked
        )
        customTextColourInner.layoutManager = LinearLayoutManager(
            root.context, RecyclerView.HORIZONTAL, false
        )
        customTextColourInner.adapter = adapter
    }

    sealed class LockScreenTextColourViewHolder(override val binding: ViewBinding) : ViewHolder(binding) {
        data class Colours(override val binding: ItemLockscreenCustomTextColourMonetBinding) :
            LockScreenTextColourViewHolder(binding)
    }
    
}