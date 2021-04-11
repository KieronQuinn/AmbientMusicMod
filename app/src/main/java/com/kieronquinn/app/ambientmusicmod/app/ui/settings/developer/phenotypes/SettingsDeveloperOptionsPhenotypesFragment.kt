package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.phenotypes

import android.os.Bundle
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsDeveloperOptionsPhenotypesFragment: BaseSettingsFragment() {

    override val viewModel by viewModel<SettingsDeveloperOptionsPhenotypesViewModel>()

    private val phenotypesWarning by preference("phenotypes_warning")
    private val hideOldResults by switchPreference("phenotype_ambient_music_hide_old_results_when_recognition_fails")
    private val smartAudioPlayback by switchPreference("phenotype_ambient_music_smart_audio_playback_detection")
    private val useDspAudioSource by switchPreference("phenotype_ambient_music_use_dsp_audio_source")
    private val highQualityResampling by switchPreference("phenotype_ambient_music_high_quality_resampling")
    private val suppressRecognitionDuringAudioRecording by switchPreference("phenotype_ambient_music_suppress_recognition_during_audio_recording")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.developer_options_phenotypes)
        phenotypesWarning?.setOnClickListener(viewModel::resetPrefs)
        hideOldResults?.isChecked = viewModel.hideResultsWhenRecognitionFails
        smartAudioPlayback?.isChecked = viewModel.smartAudioPlaybackDetection
        useDspAudioSource?.isChecked = viewModel.useDspAudioSource
        highQualityResampling?.isChecked = viewModel.highQualityResampling
        suppressRecognitionDuringAudioRecording?.isChecked = viewModel.suppressDuringAudioRecording
    }

    override fun onPause() {
        super.onPause()
        viewModel.sendUpdateIntent()
    }

}