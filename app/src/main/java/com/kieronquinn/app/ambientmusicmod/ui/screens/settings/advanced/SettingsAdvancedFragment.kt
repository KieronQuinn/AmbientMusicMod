package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.SettingsAdvancedViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isArmv7
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsAdvancedFragment: BaseSettingsFragment(), BackAvailable {

    override val addAdditionalPadding = true

    private val viewModel by viewModel<SettingsAdvancedViewModel>()

    override val adapter by lazy {
        SettingsAdvancedAdapter(binding.settingsBaseRecyclerView, emptyList())
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

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> = listOfNotNull(
        GenericSettingsItem.Setting(
            getString(R.string.settings_advanced_gain),
            getText(R.string.settings_advanced_gain_content),
            R.drawable.ic_settings_advanced_gain,
            onClick = viewModel::onGainClicked
        ),
        GenericSettingsItem.SwitchSetting(
            state.alternativeEncoding,
            getString(R.string.settings_advanced_alternative_encoding),
            getText(R.string.settings_advanced_alternative_encoding_content),
            R.drawable.ic_settings_advanced_alternative_encoding,
            onChanged = viewModel::onAlternativeEncodingChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.runOnSmallCores && !isArmv7,
            getString(R.string.settings_advanced_small_cores),
            getStringOrUnsupported(!isArmv7, R.string.settings_advanced_small_cores_content),
            R.drawable.ic_settings_advanced_small_cores,
            enabled = !isArmv7,
            onChanged = viewModel::onRunOnLittleCoresChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.superpacksRequireWifi,
            getString(R.string.settings_advanced_superpacks_require_wifi),
            getText(R.string.settings_advanced_superpacks_require_wifi_content),
            R.drawable.ic_advanced_require_wifi,
            onChanged = viewModel::onSuperpacksRequireWiFiChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.superpacksRequireCharging,
            getString(R.string.settings_advanced_superpacks_require_charging),
            getString(R.string.settings_advanced_superpacks_require_charging_content),
            R.drawable.ic_advanced_require_charging,
            onChanged = viewModel::onSuperpacksRequireChargingChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.enableLogging,
            getString(R.string.settings_advanced_enable_logging),
            getString(R.string.settings_advanced_enable_logging_content),
            R.drawable.ic_settings_advanced_logging,
            onChanged = viewModel::onEnableLoggingChanged
        ),
        GenericSettingsItem.Setting(
            getString(R.string.settings_advanced_clear_album_art_cache),
            getString(R.string.settings_advanced_clear_album_art_cache_content),
            R.drawable.ic_advanced_clear_album_art_cache
        ) {
            viewModel.onClearAlbumArtClicked(requireContext())
        },
        if(state.enableLogging){
            GenericSettingsItem.Header(getString(R.string.settings_advanced_debug))
        }else null,
        if(state.enableLogging) {
            GenericSettingsItem.Setting(
                getString(R.string.settings_advanced_poke_jobscheduler),
                getText(R.string.settings_advanced_poke_jobscheduler_content),
                R.drawable.ic_settings_advanced_poke_jobscheduler
            ) {
                viewModel.onPokeJobSchedulerClicked(requireContext())
            }
        }else null
    )

    private fun getStringOrUnsupported(isEnabled: Boolean, @StringRes resource: Int): CharSequence {
        return if(isEnabled){
            getText(resource)
        } else {
            getString(R.string.settings_generic_unsupported)
        }
    }

}