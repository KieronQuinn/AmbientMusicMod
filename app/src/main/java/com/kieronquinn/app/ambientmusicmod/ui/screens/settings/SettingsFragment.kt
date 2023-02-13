package com.kieronquinn.app.ambientmusicmod.ui.screens.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.SettingsViewModel.State
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment: BaseSettingsFragment(), BackAvailable {

    override val addAdditionalPadding = true

    private val viewModel by viewModel<SettingsViewModel>()

    override val adapter by lazy {
        SettingsAdapter(binding.settingsBaseRecyclerView, emptyList())
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
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> = listOf(
        GenericSettingsItem.Setting(
            getString(R.string.settings_recognition_period),
            getString(R.string.settings_recognition_period_content,
                getString(state.recognitionPeriod.title)),
            R.drawable.ic_settings_recognition_period,
            viewModel::onRecognitionPeriodClicked
        ),
        GenericSettingsItem.Setting(
            getString(R.string.settings_recognition_buffer),
            getString(R.string.settings_recognition_buffer_content,
                getString(state.recognitionBuffer.title)),
            R.drawable.ic_settings_recognition_buffer,
            viewModel::onRecognitionBufferClicked
        ),
        GenericSettingsItem.SwitchSetting(
            state.triggerWhenScreenOn,
            getString(R.string.settings_trigger_when_screen_on),
            getString(R.string.settings_trigger_when_screen_on_content),
            R.drawable.ic_settings_trigger_when_screen_on,
            onChanged = viewModel::onTriggerWhenScreenOnChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.albumArtEnabled,
            getString(R.string.settings_show_album_art),
            getString(R.string.settings_show_album_art_content),
            R.drawable.ic_settings_show_album_art,
            onChanged = viewModel::onAlbumArtChanged
        ),
        GenericSettingsItem.Setting(
            getString(R.string.settings_bedtime),
            getString(R.string.settings_bedtime_content, if(state.bedtimeMode){
                getString(R.string.settings_bedtime_content_enabled)
            }else{
                getString(R.string.settings_bedtime_content_disabled)
            }),
            R.drawable.ic_settings_bedtime,
            viewModel::onBedtimeClicked
        ),
        GenericSettingsItem.Setting(
            getString(R.string.settings_advanced),
            getString(R.string.settings_advanced_content),
            R.drawable.ic_nowplaying_settings,
            viewModel::onAdvancedClicked
        )
    )

}