package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.externalaccess

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.externalaccess.SettingsAdvancedExternalAccessViewModel.ExternalAccessSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.externalaccess.SettingsAdvancedExternalAccessViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsAdvancedExternalAccessFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsAdvancedExternalAccessViewModel>()

    override val adapter by lazy {
        SettingsAdvancedExternalAccessAdapter(binding.settingsBaseRecyclerView, emptyList())
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
        when(state) {
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems(), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        val switch = GenericSettingsItem.Switch(
            enabled,
            getString(R.string.settings_external_access_switch),
            viewModel::onEnabledChanged
        )
        val footer = ExternalAccessSettingsItem.Footer(viewModel::onWikiClicked)
        if(!enabled) return listOf(switch, footer)
        return listOfNotNull(
            switch,
            GenericSettingsItem.SwitchSetting(
                toggleEnabled,
                getString(R.string.settings_external_access_toggle_title),
                getString(R.string.settings_external_access_toggle_content),
                R.drawable.ic_settings_advanced_external_access_toggle,
                onChanged = viewModel::onToggleChanged
            ),
            GenericSettingsItem.SwitchSetting(
                recognitionEnabled,
                getString(R.string.settings_external_access_recognition_title),
                getText(R.string.settings_external_access_recognition_content),
                R.drawable.ic_fab_recognise,
                onChanged = viewModel::onRecognitionChanged
            ),
            GenericSettingsItem.SwitchSetting(
                requireToken,
                getString(R.string.settings_external_access_require_token_title),
                getText(R.string.settings_external_access_require_token_content),
                R.drawable.ic_settings_advanced_external_access_require_token,
                onChanged = viewModel::onRequireTokenChanged
            ),
            if(requireToken){
                GenericSettingsItem.Setting(
                    getString(R.string.settings_external_access_token_title),
                    getString(R.string.settings_external_access_token_content, token),
                    R.drawable.ic_settings_advanced_external_access_token,
                    onClick = { viewModel.onTokenClicked(requireContext()) },
                    onLongClick = viewModel::onTokenLongClicked
                )
            }else null,
            footer
        )
    }

}