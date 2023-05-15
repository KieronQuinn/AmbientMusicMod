package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.externalaccess

import android.graphics.Paint
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsAdvancedExternalAccessFooterBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.externalaccess.SettingsAdvancedExternalAccessViewModel.ExternalAccessSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed

class SettingsAdvancedExternalAccessAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemId(item: BaseSettingsItem): Long {
        return when(item){
            is ExternalAccessSettingsItem -> item.type.itemIndex.toLong()
            else -> super.getItemId(item)
        }
    }

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ExternalAccessSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ExternalAccessSettingsItem.ItemType.FOOTER -> ExternalAccessViewHolder.Footer(
                ItemSettingsAdvancedExternalAccessFooterBinding
                    .inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ExternalAccessViewHolder.Footer -> {
                val item = items[position] as ExternalAccessSettingsItem.Footer
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun ExternalAccessViewHolder.Footer.setup(
        item: ExternalAccessSettingsItem.Footer
    ) = with(binding.externalAccessFooterLink) {
        val accent = monet.getAccentColor(context)
        val primary = monet.getPrimaryColor(context)
        background.setTint(primary)
        setTextColor(accent)
        paintFlags = paintFlags or Paint.ANTI_ALIAS_FLAG or Paint.UNDERLINE_TEXT_FLAG
        whenResumed {
            onClicked().collect {
                item.onLinkClicked()
            }
        }
    }

    sealed class ExternalAccessViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Footer(override val binding: ItemSettingsAdvancedExternalAccessFooterBinding):
            ExternalAccessViewHolder(binding)
    }

}