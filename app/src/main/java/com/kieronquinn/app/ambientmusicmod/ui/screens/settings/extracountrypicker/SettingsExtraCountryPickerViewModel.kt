package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardCountry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsExtraCountryPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val errorBus: SharedFlow<Unit>

    abstract fun onCountrySelected(shardCountry: ShardCountry)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val primaryCountry: ShardCountry,
            val selectedCountries: List<ShardCountry>
        ): State()
    }

    sealed class SettingsExtraCountryPickerSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        object Header: SettingsExtraCountryPickerSettingsItem(ItemType.HEADER)

        data class Country(
            val country: ShardCountry,
            val isSelected: Boolean,
            val onSelected: (ShardCountry) -> Unit
        ): SettingsExtraCountryPickerSettingsItem(ItemType.COUNTRY)

        enum class ItemType: BaseSettingsItemType {
            HEADER, COUNTRY
        }
    }

}

class SettingsExtraCountryPickerViewModelImpl(
    private val deviceConfigRepository: DeviceConfigRepository
): SettingsExtraCountryPickerViewModel() {

    companion object {
        private const val MAX_EXTRA_LANGUAGE_LIMIT = 2
    }

    private val selectedPrimaryCountry = deviceConfigRepository.deviceCountry
    private val selectedExtraCountries = deviceConfigRepository.extraLanguages
    private val selectedExtraCountriesLimit = deviceConfigRepository.extraLanguageLimit

    override val state = combine(
        selectedPrimaryCountry.asFlow(),
        selectedExtraCountries.asFlow()
    ) { _, _ ->
        val primary = ShardCountry.forCode(deviceConfigRepository.getPrimaryLanguage())
        val extras = getExtraCountries()
        State.Loaded(primary, extras)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val errorBus = MutableSharedFlow<Unit>()

    private suspend fun getExtraCountries(): List<ShardCountry> {
        val primary = ShardCountry.forCode(deviceConfigRepository.getPrimaryLanguage())
        return deviceConfigRepository.getExtraLanguages().map {
            ShardCountry.forCode(it)
        }.filterNot { it == primary }
    }

    override fun onCountrySelected(shardCountry: ShardCountry) {
        viewModelScope.launch {
            val current = getExtraCountries().distinct()
            val newCurrent = if(current.contains(shardCountry)){
                current - shardCountry
            }else{
                current + shardCountry
            }.distinct()
            if(newCurrent.size > MAX_EXTRA_LANGUAGE_LIMIT) {
                errorBus.emit(Unit)
                return@launch
            }
            val trimmedNewCurrent = newCurrent.take(MAX_EXTRA_LANGUAGE_LIMIT)
            selectedExtraCountries.set(trimmedNewCurrent.joinToString(",") { it.code })
            selectedExtraCountriesLimit.set(trimmedNewCurrent.size)
            //Trigger a primary change to force a reload of Now Playing shards
            selectedPrimaryCountry.notifyChange()
        }
    }

}