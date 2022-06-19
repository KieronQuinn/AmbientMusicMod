package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsRecognitionPeriodBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod.SettingsRecognitionPeriodViewModel.SettingsRecognitionPeriodSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect

class SettingsRecognitionPeriodAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<SettingsRecognitionPeriodSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when (itemType) {
            SettingsRecognitionPeriodSettingsItem.ItemType.PERIOD -> SettingsRecognitionPeriodViewHolder.Period(
                ItemSettingsRecognitionPeriodBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is SettingsRecognitionPeriodViewHolder.Period -> {
                val item = items[position] as SettingsRecognitionPeriodSettingsItem.Period
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun SettingsRecognitionPeriodViewHolder.Period.setup(
        period: SettingsRecognitionPeriodSettingsItem.Period
    ) = with(binding) {
        settingsRecognitionPeriodTitle.setText(period.period.title)
        val content = period.period.content
        if(content != null){
            val topPadding = root.context.resources.getDimension(R.dimen.margin_16).toInt()
            settingsRecognitionPeriodContent.isVisible = true
            settingsRecognitionPeriodContent.text = root.context.getString(content)
            settingsRecognitionPeriodTitle.updatePadding(top = topPadding)
        }else{
            settingsRecognitionPeriodContent.isVisible = false
            settingsRecognitionPeriodContent.text = null
            settingsRecognitionPeriodTitle.updatePadding(top = 0)
        }
        settingsRecognitionPeriodRadio.isChecked = period.enabled
        settingsRecognitionPeriodRadio.applyMonet()
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        root.setOnClickListener {
            settingsRecognitionPeriodRadio.callOnClick()
        }
        lifecycleScope.launchWhenResumed {
            settingsRecognitionPeriodRadio.onClicked().collect {
                period.onClicked(period.period)
            }
        }
    }

    sealed class SettingsRecognitionPeriodViewHolder(override val binding: ViewBinding) : ViewHolder(binding) {
        data class Period(override val binding: ItemSettingsRecognitionPeriodBinding):
            SettingsRecognitionPeriodViewHolder(binding)
    }

}