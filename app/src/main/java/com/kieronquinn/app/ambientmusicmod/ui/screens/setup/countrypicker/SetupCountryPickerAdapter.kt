package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemCountryPickerBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemCountryPickerSetupHeaderBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel.SetupCountryPickerSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel.SetupCountryPickerSettingsItem.ItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import kotlinx.coroutines.flow.collect

class SetupCountryPickerAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun getItemId(item: BaseSettingsItem): Long {
        return when(item){
            is SetupCountryPickerSettingsItem.Country -> item.country?.countryName?.toLong() ?: -1L
            else -> super.getItemId(item)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ItemType.HEADER -> SetupCountryPickerViewHolder.Header(
                ItemCountryPickerSetupHeaderBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.COUNTRY -> SetupCountryPickerViewHolder.Country(
                ItemCountryPickerBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is SetupCountryPickerViewHolder.Header -> holder.setup()
            is SetupCountryPickerViewHolder.Country -> {
                val item = items[position] as SetupCountryPickerSettingsItem.Country
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun SetupCountryPickerViewHolder.Header.setup() = with(binding) {
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
    }

    private fun SetupCountryPickerViewHolder.Country.setup(
        item: SetupCountryPickerSettingsItem.Country,
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
        lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                item.onSelected(item.country)
            }
        }
    }

    sealed class SetupCountryPickerViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemCountryPickerSetupHeaderBinding): SetupCountryPickerViewHolder(binding)
        data class Country(override val binding: ItemCountryPickerBinding): SetupCountryPickerViewHolder(binding)
    }

}