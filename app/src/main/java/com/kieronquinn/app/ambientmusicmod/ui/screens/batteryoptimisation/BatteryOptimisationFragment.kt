package com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation.BatteryOptimisationViewModel.BatteryOptimisationSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation.BatteryOptimisationViewModel.State
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BatteryOptimisationFragment: BaseSettingsFragment() {

    override val addAdditionalPadding = true

    private val viewModel by viewModel<BatteryOptimisationViewModel>()

    override val adapter by lazy {
        BatteryOptimisationAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
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
                onAcceptabilityChanged(state.batteryOptimisationsDisabled)
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val optimisationSwitch = if(state.batteryOptimisationsDisabled){
            GenericSettingsItem.SwitchSetting(
                true,
                getString(R.string.battery_optimisation_system_title),
                getString(R.string.battery_optimisation_system_content),
                R.drawable.ic_settings_battery_saver,
                false){}
        }else{
            GenericSettingsItem.Setting(
                getString(R.string.battery_optimisation_system_title),
                getString(R.string.battery_optimisation_system_content),
                R.drawable.ic_settings_battery_saver,
                viewModel::onBatteryOptimisationClicked
            )
        }
        val oemSetting = if(state.oemBatteryOptimisationAvailable){
            GenericSettingsItem.Setting(
                getString(R.string.battery_optimisation_oem_title),
                getString(R.string.battery_optimisation_oem_content),
                R.drawable.ic_open,
                viewModel::onOemBatteryOptimisationClicked
            )
        }else null
        val footer = BatteryOptimisationSettingsItem.Footer(
            viewModel::onLearnMoreClicked
        )
        return listOfNotNull(optimisationSwitch, oemSetting, footer)
    }

    abstract fun onAcceptabilityChanged(acceptable: Boolean)

}