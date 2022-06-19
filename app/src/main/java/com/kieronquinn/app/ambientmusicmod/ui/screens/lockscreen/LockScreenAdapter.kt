package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenFooterBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.LockscreenOverlayStyle
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.LockScreenViewModel.LockScreenSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.LockScreenViewModel.LockScreenSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onSelected
import com.kieronquinn.app.ambientmusicmod.utils.extensions.selectTab
import com.kieronquinn.monetcompat.extensions.toArgb
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect

class LockScreenAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ItemType.HEADER -> LockScreenViewHolder.Header(
                ItemLockscreenHeaderBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.FOOTER -> LockScreenViewHolder.Footer(
                ItemLockscreenFooterBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is LockScreenViewHolder.Header -> {
                val item = items[position] as LockScreenSettingsItem.Header
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun LockScreenViewHolder.Header.setup(
        header: LockScreenSettingsItem.Header
    ) = with(binding) {
        with(lockscreenHeaderNewPreview){
            nowPlayingIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
            nowPlayingText.setTextColor(Color.WHITE)
            nowPlayingText.setText(R.string.item_nowplaying_header_preview)
            (nowPlayingIcon.drawable as AnimatedVectorDrawable).start()
            lifecycleScope.launchWhenResumed {
                root.onClicked().collect { lockscreenHeaderTabs.selectTab(0) }
            }
        }
        with(lockscreenHeaderClassicPreview){
            nowPlayingIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
            nowPlayingText.setTextColor(Color.WHITE)
            nowPlayingText.setText(R.string.item_nowplaying_header_preview)
            (nowPlayingIcon.drawable as AnimatedVectorDrawable).start()
            lifecycleScope.launchWhenResumed {
                root.onClicked().collect { lockscreenHeaderTabs.selectTab(1) }
            }
        }
        lockscreenHeaderTabs.selectTab(header.style.ordinal)
        val tabBackground = monet.getMonetColors().accent1[600]?.toArgb()
            ?: monet.getAccentColor(root.context, false)
        lockscreenHeaderTabs.backgroundTintList = ColorStateList.valueOf(tabBackground)
        lockscreenHeaderTabs.setSelectedTabIndicatorColor(monet.getAccentColor(root.context))
        lockscreenHeaderChangePosition.overrideRippleColor(monet.getAccentColor(root.context))
        lifecycleScope.launchWhenResumed {
            lockscreenHeaderTabs.onSelected().collect {
                header.onStyleSelected(LockscreenOverlayStyle.values()[it])
            }
        }
        lifecycleScope.launchWhenResumed {
            lockscreenHeaderChangePosition.onClicked().collect {
                header.onPositionClicked()
            }
        }
    }

    sealed class LockScreenViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemLockscreenHeaderBinding): LockScreenViewHolder(binding)
        data class Footer(override val binding: ItemLockscreenFooterBinding): LockScreenViewHolder(binding)
    }

}