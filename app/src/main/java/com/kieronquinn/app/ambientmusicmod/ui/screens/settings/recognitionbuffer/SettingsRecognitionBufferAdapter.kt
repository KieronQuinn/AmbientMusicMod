package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionbuffer

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsRecognitionBufferBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionbuffer.SettingsRecognitionBufferViewModel.SettingsRecognitionBufferSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionbuffer.SettingsRecognitionBufferViewModel.SettingsRecognitionBufferSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect

class SettingsRecognitionBufferAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when (itemType) {
            ItemType.BUFFER -> SettingsRecognitionBufferViewHolder.Buffer(
                ItemSettingsRecognitionBufferBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is SettingsRecognitionBufferViewHolder.Buffer -> {
                val item = items[position] as SettingsRecognitionBufferSettingsItem.Buffer
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun SettingsRecognitionBufferViewHolder.Buffer.setup(
        buffer: SettingsRecognitionBufferSettingsItem.Buffer
    ) = with(binding) {
        settingsRecognitionPeriodTitle.setText(buffer.buffer.title)
        settingsRecognitionPeriodRadio.isChecked = buffer.enabled
        settingsRecognitionPeriodRadio.applyMonet()
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        root.setOnClickListener {
            settingsRecognitionPeriodRadio.callOnClick()
        }
        lifecycleScope.launchWhenResumed {
            settingsRecognitionPeriodRadio.onClicked().collect {
                buffer.onClicked(buffer.buffer)
            }
        }
    }

    sealed class SettingsRecognitionBufferViewHolder(override val binding: ViewBinding) : ViewHolder(binding) {
        data class Buffer(override val binding: ItemSettingsRecognitionBufferBinding):
            SettingsRecognitionBufferViewHolder(binding)
    }

}