package com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country.CountryPickerViewModel.CountryPickerSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country.CountryPickerViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class CountryPickerFragment: BaseSettingsFragment(), BackAvailable {

    override val addAdditionalPadding = true

    private val viewModel by viewModel<CountryPickerViewModel>()

    override val adapter by lazy {
        CountryPickerAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<CountryPickerSettingsItem.Country> {
        val automatic = CountryPickerSettingsItem.Country(
            null,
            state.selectedItem == null,
            viewModel::setCountry,
            null
        )
        val countries = ShardsRepository.ShardCountry.values().map {
            CountryPickerSettingsItem.Country(
                it,
                state.selectedItem == it,
                viewModel::setCountry,
                viewModel::setAdditionalCountry
            )
        }.sortedBy {
            getString(it.country!!.countryName).lowercase()
        }
        return listOf(automatic) + countries
    }

}