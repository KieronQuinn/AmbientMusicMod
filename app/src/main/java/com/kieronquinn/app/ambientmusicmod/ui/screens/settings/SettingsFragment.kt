package com.kieronquinn.app.ambientmusicmod.ui.screens.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.SettingsViewModel.HistorySummaryDays
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.SettingsViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
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

    private fun onHistorySummaryChanged(value: HistorySummaryDays) {
        viewModel.onHistorySummaryDaysChanged(requireContext(), value)
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> = listOfNotNull(
        GenericSettingsItem.Setting(
            getString(R.string.settings_recognition_period),
            getString(R.string.settings_recognition_period_content,
                getString(state.recognitionPeriod.title)),
            R.drawable.ic_settings_recognition_period,
            onClick = viewModel::onRecognitionPeriodClicked
        ),
        GenericSettingsItem.Setting(
            getString(R.string.settings_recognition_buffer),
            getString(R.string.settings_recognition_buffer_content,
                getString(state.recognitionBuffer.title)),
            R.drawable.ic_settings_recognition_buffer,
            onClick = viewModel::onRecognitionBufferClicked
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
        if(state.supportsSummary){
            GenericSettingsItem.Dropdown(
                getString(R.string.settings_history_summary_days),
                getString(R.string.settings_history_summary_days_content, getString(state.historySummaryDays.label)),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_history_summary_days),
                state.historySummaryDays,
                ::onHistorySummaryChanged,
                HistorySummaryDays.values().toList()
            ){
                it.label
            }
        } else null,
        GenericSettingsItem.Setting(
            getString(R.string.settings_bedtime),
            getString(R.string.settings_bedtime_content, if(state.bedtimeMode){
                getString(R.string.settings_bedtime_content_enabled)
            }else{
                getString(R.string.settings_bedtime_content_disabled)
            }),
            R.drawable.ic_settings_bedtime,
            onClick = viewModel::onBedtimeClicked
        ),
        GenericSettingsItem.Setting(
            getString(R.string.settings_advanced),
            getString(R.string.settings_advanced_content),
            R.drawable.ic_nowplaying_settings,
            onClick = viewModel::onAdvancedClicked
        )
    )

}