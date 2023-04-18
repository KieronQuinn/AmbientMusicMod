package com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand

import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemOndemandBannerBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemOndemandHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand.OnDemandViewModel.OnDemandSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand.OnDemandViewModel.OnDemandSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onComplete
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import kotlinx.coroutines.delay

class OnDemandAdapter(
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
            ItemType.BANNER -> OnDemandViewHolder.Banner(
                ItemOndemandBannerBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.HEADER -> OnDemandViewHolder.Header(
                ItemOndemandHeaderBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is OnDemandViewHolder.Banner -> {
                val item = items[position] as OnDemandSettingsItem.Banner
                holder.setup(item)
            }
            is OnDemandViewHolder.Header -> {
                holder.setup(holder)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun OnDemandViewHolder.Banner.setup(
        banner: OnDemandSettingsItem.Banner
    ) = with(binding) {
        val backgroundColour =
            ContextCompat.getColor(root.context, banner.attentionLevel.background)
        val accentColour =
            ContextCompat.getColor(root.context, banner.attentionLevel.accent)
        root.backgroundTintList = ColorStateList.valueOf(backgroundColour)
        onDemandBannerIcon.setImageResource(banner.attentionLevel.icon)
        onDemandBannerIcon.imageTintList = ColorStateList.valueOf(accentColour)
        onDemandBannerTitle.text = banner.bannerTitle
        onDemandBannerContent.text = banner.bannerContent
        onDemandBannerButton.isVisible = banner.button != null
        onDemandBannerButton.setTextColor(accentColour)
        onDemandBannerButtonDisable.isVisible = banner.isOptionEnabled
        onDemandBannerButtonDisable.setTextColor(accentColour)
        whenResumed {
            onDemandBannerButtonDisable.onClicked().collect {
                banner.onDisableClick()
            }
        }
        banner.button?.let {
            onDemandBannerButton.setText(it.buttonText)
            whenResumed {
                onDemandBannerButton.onClicked().collect { _ ->
                    banner.onButtonClick(it.onClick)
                }
            }
        }
    }

    private fun OnDemandViewHolder.Header.setup(
        holder: LifecycleAwareRecyclerView.ViewHolder
    ) = with(binding) {
        ondemandHeaderIcon.setImageResource(R.drawable.ic_nowplaying_ondemand)
        ondemandHeaderMotion.loopHeader(ondemandHeaderIcon, holder)
    }

    private fun MotionLayout.loopHeader(
        iconImageView: ImageView,
        holder: LifecycleAwareRecyclerView.ViewHolder
    ) {
        val transitionToEndState = suspend {
            delay(2500L)
            iconImageView.setImageResource(R.drawable.audioanim_animation)
            (iconImageView.drawable as AnimatedVectorDrawable).start()
            setTransition(R.id.start_to_end)
            transitionToEnd()
        }
        val transitionToStartState = suspend {
            delay(5000L)
            iconImageView.setImageResource(R.drawable.ic_nowplaying_ondemand)
            setTransition(R.id.end_to_start)
            transitionToEnd()
        }
        holder.whenResumed {
            onComplete().collect {
                when(it){
                    R.id.start -> {
                        transitionToEndState()
                    }
                    R.id.end -> {
                        transitionToStartState()
                    }
                }
            }
        }
        holder.whenResumed {
            transitionToEndState()
        }
    }

    sealed class OnDemandViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Banner(override val binding: ItemOndemandBannerBinding): OnDemandViewHolder(binding)
        data class Header(override val binding: ItemOndemandHeaderBinding): OnDemandViewHolder(binding)
    }

}