package com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemNowplayingBannerBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemNowplayingFooterBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemNowplayingHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingViewModel.NowPlayingSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingViewModel.NowPlayingSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.filterColour
import kotlinx.coroutines.flow.collect

class NowPlayingAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemId(item: BaseSettingsItem): Long {
        return when(item){
            is NowPlayingSettingsItem -> item.type.itemIndex.toLong()
            else -> super.getItemId(item)
        }
    }

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ItemType.HEADER -> NowPlayingViewHolder.Header(
                ItemNowplayingHeaderBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.BANNER -> NowPlayingViewHolder.Banner(
                ItemNowplayingBannerBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.FOOTER -> NowPlayingViewHolder.Footer(
                ItemNowplayingFooterBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is NowPlayingViewHolder.Header -> holder.setup()
            is NowPlayingViewHolder.Banner -> {
                val item = items[position] as NowPlayingSettingsItem.Banner
                holder.setup(item)
            }
            is NowPlayingViewHolder.Footer -> {
                val item = items[position] as NowPlayingSettingsItem.Footer
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun NowPlayingViewHolder.Header.setup() = with(binding.itemNowplayingHeaderLottie) {
        val background = monet.getPrimaryColor(context, !context.isDarkMode)
        val accent = monet.getAccentColor(context)
        backgroundTintList = ColorStateList.valueOf(background)
        filterColour("RingsNotes_Precomp", "**", filter = accent)
        playAnimation()
    }

    private fun NowPlayingViewHolder.Banner.setup(
        banner: NowPlayingSettingsItem.Banner
    ) = with(binding) {
        val bannerMessage = banner.bannerMessage
        val backgroundColour =
            ContextCompat.getColor(root.context, bannerMessage.attentionLevel.background)
        val accentColour =
            ContextCompat.getColor(root.context, bannerMessage.attentionLevel.accent)
        root.backgroundTintList = ColorStateList.valueOf(backgroundColour)
        nowPlayingBannerIcon.setImageResource(bannerMessage.attentionLevel.icon)
        nowPlayingBannerIcon.imageTintList = ColorStateList.valueOf(accentColour)
        nowPlayingBannerTitle.setText(bannerMessage.title)
        nowPlayingBannerContent.setText(bannerMessage.message)
        nowPlayingBannerButton.isVisible = bannerMessage.button != null
        nowPlayingBannerButton.setTextColor(accentColour)
        bannerMessage.button?.let {
            nowPlayingBannerButton.setText(it.buttonText)
            lifecycleScope.launchWhenResumed {
                nowPlayingBannerButton.onClicked().collect { _ ->
                    banner.onButtonClick(it.onClick)
                }
            }
        }
    }

    private fun NowPlayingViewHolder.Footer.setup(
        item: NowPlayingSettingsItem.Footer
    ) = with(binding.nowplayingFooterLink) {
        val accent = monet.getAccentColor(context)
        val primary = monet.getPrimaryColor(context)
        background.setTint(primary)
        setTextColor(accent)
        paintFlags = paintFlags or Paint.ANTI_ALIAS_FLAG or Paint.UNDERLINE_TEXT_FLAG
        lifecycleScope.launchWhenResumed {
            onClicked().collect {
                item.onLinkClicked()
            }
        }
    }

    sealed class NowPlayingViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemNowplayingHeaderBinding): NowPlayingViewHolder(binding)
        data class Banner(override val binding: ItemNowplayingBannerBinding): NowPlayingViewHolder(binding)
        data class Footer(override val binding: ItemNowplayingFooterBinding): NowPlayingViewHolder(binding)
    }

}