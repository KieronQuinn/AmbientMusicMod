package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenTextColourBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.LockScreenTextColourViewModel.LockScreenTextColourSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect

class LockScreenTextColourAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<LockScreenTextColourSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when (itemType) {
            LockScreenTextColourSettingsItem.ItemType.COLOUR -> LockScreenTextColourViewHolder.Colour(
                ItemLockscreenTextColourBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is LockScreenTextColourViewHolder.Colour -> {
                val item = items[position] as LockScreenTextColourSettingsItem.Colour
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun LockScreenTextColourViewHolder.Colour.setup(
        colour: LockScreenTextColourSettingsItem.Colour
    ) = with(binding) {
        lockscreenTextColourTitle.setText(colour.colour.title)
        if(colour.contentOverride != null) {
            lockscreenTextColourContent.text = colour.contentOverride
        }else{
            lockscreenTextColourContent.setText(colour.colour.content)
        }
        lockscreenTextColourRadio.isChecked = colour.isSelected
        lockscreenTextColourRadio.applyMonet()
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        root.setOnClickListener {
            lockscreenTextColourRadio.callOnClick()
        }
        lifecycleScope.launchWhenResumed {
            lockscreenTextColourRadio.onClicked().collect {
                colour.onClicked(colour.colour)
            }
        }
    }

    sealed class LockScreenTextColourViewHolder(override val binding: ViewBinding) : ViewHolder(binding) {
        data class Colour(override val binding: ItemLockscreenTextColourBinding) :
            LockScreenTextColourViewHolder(binding)
    }

}