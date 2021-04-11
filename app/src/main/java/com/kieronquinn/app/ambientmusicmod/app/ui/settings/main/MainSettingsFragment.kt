package com.kieronquinn.app.ambientmusicmod.app.ui.settings.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.preferences.ChipPreference
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.RootFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainSettingsFragment: BaseSettingsFragment(), RootFragment {

    override val viewModel by viewModel<MainSettingsViewModel>()

    private val moduleStatePreference by preference("setting_module_enabled")
    private val batteryOptimisationPreference by preference("setting_battery_optimisation")
    private val enabledPreference by switchPreference("enabled")

    private val manualTriggerPreference by preference("settings_manual_trigger")
    private val databaseViewerPreference by preference("settings_database_viewer")

    //Mod Settings
    private val modSettingsCategory by preferenceCategory("setting_category_mod")
    private val modSettingsAmplification by preference("setting_gain")
    private val modSettingsListenPeriod by preference("setting_job_time")
    private val modSettingsRunWhenWoken by switchPreference("run_when_woken")
    private val modSettingsAdvanced by preference("setting_advanced")
    private val modSettingsShowHistory by switchPreference("show_history_in_launcher")
    private val modSettingsShowOnLockscreen by preference("show_on_lockscreen")
    private val modSettingsDeveloperOptions by preference("setting_developer_options")

    private val faqPreference by preference("faq")
    private val xdaPreference by preference("xda_thread")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.module_preferences)
        enabledPreference?.setOnClickListener(viewModel::onEnabledClicked)
        manualTriggerPreference?.setOnClickListener(viewModel::onManualTriggerClicked)
        modSettingsAmplification?.setOnClickListener(viewModel::onAmplificationClicked)
        modSettingsListenPeriod?.setOnClickListener(viewModel::onListenPeriodClicked)
        modSettingsRunWhenWoken?.setOnClickListener(viewModel::onWhenWokenClicked)
        modSettingsRunWhenWoken?.isChecked = viewModel.runWhenWoken
        enabledPreference?.setOnPreferenceChangeListener { _, _ -> false }
        modSettingsAdvanced?.setOnClickListener(viewModel::onAdvancedClicked)
        modSettingsShowHistory?.setOnClickListener(viewModel::onShowHistoryClicked)
        modSettingsShowHistory?.setOnPreferenceChangeListener { _, _ -> false }
        modSettingsShowOnLockscreen?.setOnClickListener(viewModel::onShowOnLockscreenClicked)
        batteryOptimisationPreference?.setOnClickListener(viewModel::onBatteryOptimisationClicked)
        modSettingsDeveloperOptions?.setOnClickListener(viewModel::onDeveloperOptionsClicked)
        databaseViewerPreference?.setOnClickListener(viewModel::onTrackListClicked)
        faqPreference?.setOnClickListener(viewModel::onFaqClicked)
        xdaPreference?.setOnClickListener(viewModel::onXDAThreadClicked)
        findPreference<ChipPreference>("about")?.let {
            setupAboutPreference(it)
        }
        lifecycleScope.launchWhenCreated {
            launch {
                viewModel.moduleState.collect {
                    setModuleStatePref(it)
                }
            }
            launch {
                viewModel.historyActivityState.collect {
                    modSettingsShowHistory?.isChecked = it
                }
            }
            launch {
                viewModel.enabledState.collect {
                    enabledPreference?.isChecked = it
                    manualTriggerPreference?.isEnabled = it
                }
            }
            launch {
                viewModel.batteryOptimisationDisabled.collect {
                    batteryOptimisationPreference?.run {
                        isVisible = !it
                        if(!it){
                            setBackgroundTint(ContextCompat.getColor(context, R.color.icon_circle_11))
                        }
                    }
                }
            }
            launch {
                viewModel.developerOptionsEnabled.collect {
                    modSettingsDeveloperOptions?.isVisible = it
                }
            }
            launch {
                viewModel.pixelAmbientServicesVersionIncompatible.collect {
                    if(it) viewModel.showPASIncompatibleDialog()
                }
            }
        }
    }

    private fun setModuleStatePref(moduleState: MainSettingsViewModel.ModuleState) {
        moduleStatePreference?.run {
            when(moduleState){
                MainSettingsViewModel.ModuleState.ENABLED -> {
                    title = getString(R.string.settings_module_state_title_enabled)
                    summary = getText(R.string.settings_module_state_title_enabled_desc)
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_module_check_round)
                    setBackgroundTint(Color.TRANSPARENT)
                }
                MainSettingsViewModel.ModuleState.DISABLED -> {
                    title = getString(R.string.settings_module_state_title_disabled)
                    summary = getText(R.string.settings_module_state_title_disabled_desc)
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_module_cross_round)
                    setBackgroundTint(ContextCompat.getColor(context, R.color.module_cross_circle))
                }
                MainSettingsViewModel.ModuleState.NO_XPOSED -> {
                    title = getString(R.string.settings_module_state_title_not_installed)
                    summary = getText(R.string.settings_module_state_title_not_installed_desc)
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_module_cross_round)
                    setBackgroundTint(ContextCompat.getColor(context, R.color.module_cross_circle))
                }
            }
            modSettingsCategory?.isVisible = moduleState == MainSettingsViewModel.ModuleState.ENABLED
            enabledPreference?.isVisible = moduleState == MainSettingsViewModel.ModuleState.ENABLED
            manualTriggerPreference?.isVisible = moduleState == MainSettingsViewModel.ModuleState.ENABLED
            databaseViewerPreference?.isVisible = moduleState == MainSettingsViewModel.ModuleState.ENABLED
        }
    }

    private fun setupAboutPreference(preference: ChipPreference) = with(preference) {
        summary = getString(R.string.about_desc, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
        tripleTapListener = {
            viewModel.toggleDeveloperOptions()
        }
        setChips(arrayOf(
            ChipPreference.PreferenceChip(R.color.icon_circle_7, R.string.donate, R.drawable.ic_donate){
               viewModel.onDonateClicked()
            },
            ChipPreference.PreferenceChip(R.color.icon_circle_8, R.string.twitter, R.drawable.ic_twitter){
                viewModel.onTwitterClicked()
            },
            ChipPreference.PreferenceChip(R.color.icon_circle_6, R.string.github, R.drawable.ic_github){
                viewModel.onGitHubClicked()
            }
        ))
    }

    override fun onResume() {
        super.onResume()
        viewModel.getModuleState()
        viewModel.getHistoryActivityState()
        viewModel.getBatteryOptimisationState()
    }

}