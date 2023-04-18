package com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemCountryPickerBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country.CountryPickerViewModel.CountryPickerSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country.CountryPickerViewModel.CountryPickerSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onLongClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed

class CountryPickerAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun getItemId(item: BaseSettingsItem): Long {
        return when(item){
            is CountryPickerSettingsItem.Country -> item.country?.countryName?.toLong() ?: -1L
            else -> super.getItemId(item)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ItemType.COUNTRY -> CountryPickerViewHolder.Country(
                ItemCountryPickerBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is CountryPickerViewHolder.Country -> {
                val item = items[position] as CountryPickerSettingsItem.Country
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun CountryPickerViewHolder.Country.setup(
        item: CountryPickerSettingsItem.Country,
    ) = with(binding) {
        if(item.country != null){
            itemCountryPickerIcon.setImageResource(item.country.icon)
            itemCountryPickerTitle.setText(item.country.countryName)
        }else{
            itemCountryPickerIcon.setImageResource(R.drawable.ic_country_picker_automatic)
            itemCountryPickerTitle.setText(R.string.country_picker_automatic)
        }
        itemCountryPickerIcon.clipToOutline = true
        itemCountryPickerCheck.isVisible = item.isSelected
        whenResumed {
            root.onClicked().collect {
                item.onSelected(item.country)
            }
        }
        whenResumed {
            item.onLongSelected?.let { long ->
                root.onLongClicked().collect {
                    long(item.country)
                }
            } ?: run {
                root.setOnLongClickListener(null)
            }
        }
    }

    sealed class CountryPickerViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Country(override val binding: ItemCountryPickerBinding): CountryPickerViewHolder(binding)
    }

}