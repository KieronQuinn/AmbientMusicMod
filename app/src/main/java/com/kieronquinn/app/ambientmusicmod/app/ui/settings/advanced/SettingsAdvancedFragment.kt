package com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced

import android.os.Bundle
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsAdvancedFragment: BaseSettingsFragment() {

    override val viewModel by viewModel<SettingsAdvancedViewModel>()

    private val runOnLittleCores by switchPreference("run_on_little_cores")
    private val showAlbumArt by switchPreference("show_album_art")
    private val useAssistantForClick by switchPreference("use_assistant_for_click")
    private val customAmplification by preference("advanced_custom_amplification")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.module_advanced_preferences)
        runOnLittleCores?.setOnClickListener(viewModel::onRunOnLittleCoresClicked)
        runOnLittleCores?.isChecked = viewModel.runOnLittleCores
        useAssistantForClick?.setOnClickListener(viewModel::onUseAssistantForClickClicked)
        useAssistantForClick?.isChecked = viewModel.useAssistantForClick
        showAlbumArt?.setOnClickListener(viewModel::onShowAlbumArtClicked)
        showAlbumArt?.isChecked = viewModel.showAlbumArt
        customAmplification?.setOnClickListener(viewModel::onCustomAmplificationClicked)
    }

}