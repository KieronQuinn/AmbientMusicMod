package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.airbnb.lottie.LottieAnimationView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsBedtimeHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime.SettingsBedtimeViewModel.SettingsBedtimeSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onComplete
import com.kieronquinn.app.ambientmusicmod.utils.extensions.replaceColour
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

class SettingsBedtimeAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    companion object {
        private const val INITIAL_ANIMATION_START_DELAY = 250L
    }

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when (itemType) {
            ItemType.HEADER -> SettingsBedtimeViewHolder.Header(
                ItemSettingsBedtimeHeaderBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is SettingsBedtimeViewHolder.Header -> {
                holder.setup()
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun SettingsBedtimeViewHolder.Header.setup() = with(binding) {
        binding.bedtimeHeaderMotion.clipToOutline = true
        bedtimeHeaderLottie.run {
            val night = monet.getAccentColor(context, false).let {
                ColorUtils.blendARGB(it, Color.BLACK, 0.5f)
            }
            val day = monet.getAccentColor(context, true).let {
                ColorUtils.blendARGB(it, Color.WHITE, 0.25f)
            }
            replaceColour("sky_night", "**", replaceWith = night)
            replaceColour("sky_day", "**", replaceWith = day)
        }
        whenResumed {
            bedtimeHeaderMotion.loopBedtimeHeader(bedtimeHeaderRecognised)
        }
        bedtimeHeaderRecognised.setAnimation(R.raw.lottie_bedtime_note_1)
        whenResumed {
            delay(INITIAL_ANIMATION_START_DELAY)
            bedtimeHeaderMotion.setTransition(R.id.start_to_loop_start)
            bedtimeHeaderMotion.transitionToEnd()
        }
    }

    sealed class SettingsBedtimeViewHolder(override val binding: ViewBinding) : ViewHolder(binding) {
        data class Header(override val binding: ItemSettingsBedtimeHeaderBinding):
            SettingsBedtimeViewHolder(binding)
    }

    private suspend fun MotionLayout.loopBedtimeHeader(lottie: LottieAnimationView) {
        onComplete().collect {
            when(it){
                R.id.loop_start -> {
                    setTransition(R.id.loop_start_to_loop_end)
                    lottie.visibility = View.GONE
                    lottie.progress = 0f
                    lottie.setAnimation(R.raw.lottie_bedtime_note_2)
                    lottie.visibility = View.VISIBLE
                    transitionToEnd()
                }
                R.id.loop_end -> {
                    setTransition(R.id.loop_end_to_loop_start)
                    lottie.visibility = View.GONE
                    lottie.progress = 0f
                    lottie.setAnimation(R.raw.lottie_bedtime_note_3)
                    lottie.visibility = View.VISIBLE
                    transitionToEnd()
                }
            }
        }
    }

}