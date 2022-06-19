package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenOwnerInfoBannerBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenOwnerInfoFooterBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenOwnerInfoHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerAttentionLevel
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.LockScreenOwnerInfoViewModel.LockScreenOwnerInfoSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView

class LockScreenOwnerInfoAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<LockScreenOwnerInfoSettingsItem.ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            LockScreenOwnerInfoSettingsItem.ItemType.HEADER -> LockScreenOwnerInfoViewHolder.Header(
                ItemLockscreenOwnerInfoHeaderBinding.inflate(layoutInflater, parent, false)
            )
            LockScreenOwnerInfoSettingsItem.ItemType.BANNER -> LockScreenOwnerInfoViewHolder.Banner(
                ItemLockscreenOwnerInfoBannerBinding.inflate(layoutInflater, parent, false)
            )
            LockScreenOwnerInfoSettingsItem.ItemType.FOOTER -> LockScreenOwnerInfoViewHolder.Footer(
                ItemLockscreenOwnerInfoFooterBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is LockScreenOwnerInfoViewHolder.Header -> {
                val item = items[position] as LockScreenOwnerInfoSettingsItem.Header
                holder.setup(item)
            }
            is LockScreenOwnerInfoViewHolder.Banner -> holder.setup()
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun LockScreenOwnerInfoViewHolder.Banner.setup() = with(binding) {
        val level = BannerAttentionLevel.HIGH
        val backgroundColour =
            ContextCompat.getColor(root.context, level.background)
        val accentColour =
            ContextCompat.getColor(root.context, level.accent)
        root.backgroundTintList = ColorStateList.valueOf(backgroundColour)
        lockscreenOwnerInfoBannerIcon.setImageResource(level.icon)
        lockscreenOwnerInfoBannerIcon.imageTintList = ColorStateList.valueOf(accentColour)
    }

    private fun LockScreenOwnerInfoViewHolder.Header.setup(item: LockScreenOwnerInfoSettingsItem.Header) = with(binding) {
        val context = root.context
        lockscreenOwnerInfoHeaderPreview.text = if(item.showNote){
            context.getString(R.string.lockscreen_owner_info_note,
                context.getString(R.string.item_nowplaying_header_preview))
        }else{
            context.getString(R.string.item_nowplaying_header_preview)
        }
    }

    sealed class LockScreenOwnerInfoViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemLockscreenOwnerInfoHeaderBinding): LockScreenOwnerInfoViewHolder(binding)
        data class Banner(override val binding: ItemLockscreenOwnerInfoBannerBinding): LockScreenOwnerInfoViewHolder(binding)
        data class Footer(override val binding: ItemLockscreenOwnerInfoFooterBinding): LockScreenOwnerInfoViewHolder(binding)
    }

}