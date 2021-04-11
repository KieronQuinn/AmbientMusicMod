package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer

import android.os.Bundle
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsDeveloperOptionsFragment: BaseSettingsFragment() {

    override val viewModel by viewModel<SettingsDeveloperOptionsViewModel>()

    private val phenotypes by preference("developer_options_phenotypes")
    private val dumpLogs by preference("developer_dump_logs")
    private val enableLogging by switchPreference("developer_enable_logging")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.developer_options)
        enableLogging?.isChecked = viewModel.enableLogging
        phenotypes?.setOnClickListener(viewModel::onPhenotypesClicked)
        dumpLogs?.setOnClickListener(viewModel::onDumpLogsClicked)
    }

}