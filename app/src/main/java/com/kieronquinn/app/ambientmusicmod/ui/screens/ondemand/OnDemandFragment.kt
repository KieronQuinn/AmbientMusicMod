package com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerAttentionLevel
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerButton
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.GoogleAppState
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand.OnDemandViewModel.OnDemandSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand.OnDemandViewModel.State
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnDemandFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<OnDemandViewModel>()

    override val adapter by lazy {
        OnDemandAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
                adapter.update(loadItems(
                    state.googleAppState,
                    state.onDemandEnabled,
                    state.onDemandSaveEnabled,
                    state.onDemandVibrate
                ), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(
        googleAppState: GoogleAppState,
        onDemandEnabled: Boolean,
        onDemandSave: Boolean,
        onDemandVibrate: Boolean
    ): List<BaseSettingsItem> {
        val faqEvent = NavigationEvent.Id(R.id.action_global_faqFragment)
        val banner = when(googleAppState){
            GoogleAppState.UNSUPPORTED -> OnDemandSettingsItem.Banner(
                getString(R.string.on_demand_banner_unsupported_title),
                getString(R.string.on_demand_banner_unsupported_content),
                BannerAttentionLevel.HIGH,
                BannerButton(R.string.on_demand_banner_unsupported_button, faqEvent),
                onDemandEnabled,
                viewModel::onBannerButtonClicked,
                viewModel::onBannerDisableButtonClicked
            )
            GoogleAppState.NEEDS_OVERLAY -> OnDemandSettingsItem.Banner(
                getString(R.string.on_demand_banner_needs_overlay_title),
                getString(R.string.on_demand_banner_needs_overlay_content),
                BannerAttentionLevel.HIGH,
                BannerButton(R.string.on_demand_banner_needs_overlay_button, faqEvent),
                onDemandEnabled,
                viewModel::onBannerButtonClicked,
                viewModel::onBannerDisableButtonClicked
            )
            GoogleAppState.NEEDS_SPLIT -> OnDemandSettingsItem.Banner(
                getString(R.string.on_demand_banner_needs_split_title),
                getString(R.string.on_demand_banner_needs_split_content),
                BannerAttentionLevel.MEDIUM,
                BannerButton(R.string.on_demand_banner_needs_split_button, faqEvent),
                onDemandEnabled,
                viewModel::onBannerButtonClicked,
                viewModel::onBannerDisableButtonClicked
            )
            else -> null
        }
        if(banner != null){
            return listOf(banner)
        }
        val switch = GenericSettingsItem.Switch(
            onDemandEnabled,
            getString(R.string.on_demand_switch),
            viewModel::onSwitchChanged
        )
        if(!onDemandEnabled){
            return listOf(OnDemandSettingsItem.Header, switch)
        }
        val onDemandSaveSetting = GenericSettingsItem.SwitchSetting(
            onDemandSave,
            getString(R.string.on_demand_save_title),
            getString(R.string.on_demand_save_content),
            R.drawable.ic_ondemand_save,
            onChanged = viewModel::onOnDemandSaveChanged
        )
        val onDemandVibrateSetting = GenericSettingsItem.SwitchSetting(
            onDemandVibrate,
            getString(R.string.on_demand_vibrate_title),
            getString(R.string.on_demand_vibrate_content),
            R.drawable.ic_on_demand_vibrate,
            onChanged = viewModel::onOnDemandVibrateChanged
        )
        return listOf(
            OnDemandSettingsItem.Header, switch, onDemandSaveSetting, onDemandVibrateSetting
        )
    }

}