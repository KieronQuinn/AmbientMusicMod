package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.AccessibilityRepository
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.LockScreenViewModel.LockScreenSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.LockScreenViewModel.State
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class LockScreenFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<LockScreenViewModel>()
    private val accessibility by inject<AccessibilityRepository>()

    override val adapter by lazy {
        LockScreenAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        accessibility.bringToFrontOnAccessibilityStart(this)
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
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val switch = GenericSettingsItem.Switch(
            state.enabled,
            getString(R.string.lockscreen_enabled)
        ) { viewModel.onSwitchChanged(requireContext(), it)}
        val ownerInfo = GenericSettingsItem.Setting(
            getString(R.string.lockscreen_owner_info),
            getString(R.string.lockscreen_owner_info_content),
            R.drawable.ic_lock_screen_owner_info,
            viewModel::onOwnerInfoClicked
        )
        if(!state.enabled) return listOf(switch, ownerInfo, LockScreenSettingsItem.Footer)
        val header = LockScreenSettingsItem.Header(
            state.style,
            viewModel::onStyleChanged,
            viewModel::onPositionClicked
        )
        val enhancedSetting = GenericSettingsItem.SwitchSetting(
            state.enhancedEnabled,
            getString(R.string.lockscreen_enhanced),
            getString(R.string.lockscreen_enhanced_content),
            R.drawable.ic_lockscreen_enhanced,
            onChanged = viewModel::onEnhancedChanged
        )
        val clickAction = GenericSettingsItem.Setting(
            getString(R.string.lockscreen_overlay_on_clicked),
            getString(
                R.string.lockscreen_overlay_on_clicked_content,
                getString(state.clickAction.title)
            ),
            R.drawable.ic_lockscreen_click_action,
            viewModel::onClickActionClicked
        )
        val textColour = GenericSettingsItem.Setting(
            getString(R.string.lockscreen_overlay_text_colour_title),
            getString(R.string.lockscreen_overlay_text_colour_content, state.overlayTextColour),
            R.drawable.ic_lockscreen_overlay_text_colour,
            viewModel::onTextColourClicked
        )
        val onDemand = if(state.onDemandAvailable){
            GenericSettingsItem.SwitchSetting(
                state.onDemandEnabled,
                getString(R.string.lockscreen_show_on_demand),
                getString(R.string.lockscreen_show_on_demand_content),
                R.drawable.ic_nowplaying_ondemand,
                onChanged = viewModel::onOnDemandChanged
            )
        }else null
        return listOfNotNull(
            switch, header, enhancedSetting, clickAction, textColour, onDemand, ownerInfo, LockScreenSettingsItem.Footer
        )
    }

}