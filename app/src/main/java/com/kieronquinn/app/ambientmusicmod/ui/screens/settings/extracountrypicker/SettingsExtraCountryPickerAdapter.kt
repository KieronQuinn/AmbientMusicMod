package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemCountryPickerBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemCountryPickerSettingsHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker.SettingsExtraCountryPickerViewModel.SettingsExtraCountryPickerSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel.SetupCountryPickerSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed

class SettingsExtraCountryPickerAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun getItemId(item: BaseSettingsItem): Long {
        return when(item){
            is SettingsExtraCountryPickerSettingsItem.Country -> item.country.countryName.toLong()
            else -> super.getItemId(item)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ItemType.HEADER -> SettingsExtraCountryPickerViewHolder.Header(
                ItemCountryPickerSettingsHeaderBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.COUNTRY -> SettingsExtraCountryPickerViewHolder.Country(
                ItemCountryPickerBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is SettingsExtraCountryPickerViewHolder.Header -> holder.setup()
            is SettingsExtraCountryPickerViewHolder.Country -> {
                val item = items[position] as SettingsExtraCountryPickerSettingsItem.Country
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun SettingsExtraCountryPickerViewHolder.Header.setup() = with(binding) {
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun SettingsExtraCountryPickerViewHolder.Country.setup(
        item: SettingsExtraCountryPickerSettingsItem.Country,
    ) = with(binding) {
        itemCountryPickerIcon.setImageResource(item.country.icon)
        itemCountryPickerTitle.setText(item.country.countryName)
        itemCountryPickerIcon.clipToOutline = true
        itemCountryPickerCheck.isVisible = item.isSelected
        whenResumed {
            root.onClicked().collect {
                item.onSelected(item.country)
            }
        }
    }

    sealed class SettingsExtraCountryPickerViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemCountryPickerSettingsHeaderBinding): SettingsExtraCountryPickerViewHolder(binding)
        data class Country(override val binding: ItemCountryPickerBinding): SettingsExtraCountryPickerViewHolder(binding)
    }

}