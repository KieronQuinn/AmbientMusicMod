package com.kieronquinn.app.ambientmusicmod.ui.screens.contributors

import android.text.util.Linkify
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemContributorsLinkedSettingBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.contributors.ContributorsViewModel.ContributorsSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class ContributorsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ContributorsSettingsItem.ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ContributorsSettingsItem.ItemType.LINKED_SETTING -> ContributorsViewHolder.LinkedSetting(
                ItemContributorsLinkedSettingBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ContributorsViewHolder.LinkedSetting -> {
                val item = items[position] as ContributorsSettingsItem.LinkedSetting
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun ContributorsViewHolder.LinkedSetting.setup(
        item: ContributorsSettingsItem.LinkedSetting
    ) = with(binding) {
        itemSettingsTextTitle.text = item.title
        itemSettingsTextContent.text = item.subtitle
        itemSettingsTextContent.isVisible = item.subtitle.isNotEmpty()
        itemSettingsTextIcon.setImageResource(item.icon)
        Linkify.addLinks(itemSettingsTextContent, Linkify.ALL)
        itemSettingsTextContent.movementMethod = BetterLinkMovementMethod.newInstance().apply {
            setOnLinkClickListener { _, url ->
                item.onLinkClicked(url)
                true
            }
        }
    }

    sealed class ContributorsViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class LinkedSetting(override val binding: ItemContributorsLinkedSettingBinding):
            ContributorsViewHolder(binding)
    }
    
}