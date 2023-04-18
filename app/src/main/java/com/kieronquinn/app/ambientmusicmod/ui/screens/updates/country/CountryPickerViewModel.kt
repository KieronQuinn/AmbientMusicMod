package com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardCountry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class CountryPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setCountry(country: ShardCountry?)

    abstract fun setAdditionalCountry(country: ShardCountry?)

    sealed class State {
        object Loading: State()
        data class Loaded(val selectedItem: ShardCountry?): State()
    }

    sealed class CountryPickerSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Country(
            val country: ShardCountry?,
            val isSelected: Boolean,
            val onSelected: (ShardCountry?) -> Unit,
            val onLongSelected: ((ShardCountry?) -> Unit)?
        ): CountryPickerSettingsItem(ItemType.COUNTRY)

        enum class ItemType: BaseSettingsItemType {
            COUNTRY
        }
    }

}

class CountryPickerViewModelImpl(
    private val deviceConfigRepository: DeviceConfigRepository
): CountryPickerViewModel() {

    override val state = deviceConfigRepository.deviceCountry.asFlow().map { countryCode ->
        val selectedItem = ShardCountry.values().firstOrNull { it.code == countryCode }
        State.Loaded(selectedItem)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun setCountry(country: ShardCountry?) {
        viewModelScope.launch {
            deviceConfigRepository.deviceCountry.set(country?.code ?: "")
        }
    }

    override fun setAdditionalCountry(country: ShardCountry?) {
        viewModelScope.launch {
            deviceConfigRepository.extraLanguages.set(country?.code ?: "")
            deviceConfigRepository.extraLanguageLimit.set(1)
        }
    }

}