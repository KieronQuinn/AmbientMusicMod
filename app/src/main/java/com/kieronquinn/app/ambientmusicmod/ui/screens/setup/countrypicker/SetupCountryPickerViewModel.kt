package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.SetupNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardCountry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SetupCountryPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onCountrySelected(shardCountry: ShardCountry?)
    abstract fun onNextClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val selectedCountry: ShardCountry?): State()
    }

    sealed class SetupCountryPickerSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        object Header: SetupCountryPickerSettingsItem(ItemType.HEADER)

        data class Country(
            val country: ShardCountry?,
            val isSelected: Boolean,
            val onSelected: (ShardCountry?) -> Unit
        ): SetupCountryPickerSettingsItem(ItemType.COUNTRY)

        enum class ItemType: BaseSettingsItemType {
            HEADER, COUNTRY
        }
    }

}

class SetupCountryPickerViewModelImpl(
    deviceConfigRepository: DeviceConfigRepository,
    private val navigation: SetupNavigation
): SetupCountryPickerViewModel() {

    private val selectedCountry = deviceConfigRepository.deviceCountry

    private fun String.toShardCountry(): ShardCountry? {
        return ShardCountry.values().firstOrNull { it.code == this }
    }

    override val state = selectedCountry.asFlow().map { State.Loaded(it.toShardCountry()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onCountrySelected(shardCountry: ShardCountry?) {
        viewModelScope.launch {
            selectedCountry.set(shardCountry?.code ?: "")
        }
    }

    override fun onNextClicked() {
        viewModelScope.launch {
            navigation.navigate(SetupCountryPickerFragmentDirections.actionSetupCountryPickerFragmentToSetupInstallPAMFragment())
        }
    }

}