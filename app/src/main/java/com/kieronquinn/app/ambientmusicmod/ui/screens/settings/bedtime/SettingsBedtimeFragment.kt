package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime.SettingsBedtimeViewModel.SettingsBedtimeSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime.SettingsBedtimeViewModel.State
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Duration
import java.time.LocalTime

class SettingsBedtimeFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsBedtimeViewModel>()

    override val adapter by lazy {
        SettingsBedtimeAdapter(binding.settingsBaseRecyclerView, emptyList())
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

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val switch = GenericSettingsItem.Switch(
            state.enabled,
            getString(R.string.settings_bedtime_switch),
            viewModel::onEnabledChanged
        )
        if(!state.enabled) return listOf(SettingsBedtimeSettingsItem.Header, switch)
        val startTime = GenericSettingsItem.Setting(
            getString(R.string.settings_bedtime_start),
            state.startTime,
            R.drawable.ic_settings_bedtime_start_time
        ) {
            showStartPicker(state.startLocalTime, DateFormat.is24HourFormat(requireContext()))
        }
        val endTime = GenericSettingsItem.Setting(
            getString(R.string.settings_bedtime_end),
            state.endTime,
            R.drawable.ic_settings_bedtime_end_time
        ) {
            showEndPicker(state.endLocalTime, DateFormat.is24HourFormat(requireContext()))
        }
        return listOf(SettingsBedtimeSettingsItem.Header, switch, startTime, endTime)
    }

    private fun showStartPicker(selected: LocalTime, is24h: Boolean) {
        createTimePicker(
            selected, is24h, R.string.settings_bedtime_start, viewModel::onStartTimeChanged
        ).show(childFragmentManager, "bedtime_start")
    }

    private fun showEndPicker(selected: LocalTime, is24h: Boolean) {
        createTimePicker(
            selected, is24h, R.string.settings_bedtime_end, viewModel::onEndTimeChanged
        ).show(childFragmentManager, "bedtime_end")
    }

    //MaterialTimePicker is STILL final, so we have to show it as a dialog. Ew. Config changes Broke
    private fun createTimePicker(
        selected: LocalTime,
        is24h: Boolean,
        @StringRes title: Int,
        onPositive: (timeInMinutes: Long) -> Unit
    ) = MaterialTimePicker.Builder().apply {
        setHour(selected.hour)
        setMinute(selected.minute)
        setTitleText(title)
        setInputMode(INPUT_MODE_CLOCK)
        setTimeFormat(if(is24h) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }.build().apply {
        addOnPositiveButtonClickListener {
            onPositive(Duration.ofHours(hour.toLong()).plusMinutes(minute.toLong()).toMinutes())
        }
    }

}