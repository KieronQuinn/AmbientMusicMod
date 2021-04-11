package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.phenotypes

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class SettingsDeveloperOptionsPhenotypesViewModel: BaseViewModel() {

    abstract val hideResultsWhenRecognitionFails: Boolean
    abstract val smartAudioPlaybackDetection: Boolean
    abstract val highQualityResampling: Boolean
    abstract val useDspAudioSource: Boolean
    abstract val suppressDuringAudioRecording: Boolean

    abstract fun resetPrefs()
    abstract fun sendUpdateIntent()

}

class SettingsDeveloperOptionsPhenotypesViewModelImpl: SettingsDeveloperOptionsPhenotypesViewModel() {

    override val hideResultsWhenRecognitionFails = settings.phenotype_hideResultsWhenRecognitionFails
    override val smartAudioPlaybackDetection = settings.phenotype_smartAudioPlaybackDetection
    override val highQualityResampling = settings.phenotype_highQualityResampling
    override val useDspAudioSource = settings.phenotype_useDspAudioSource
    override val suppressDuringAudioRecording = settings.phenotype_suppressDuringAudioRecording

    override fun resetPrefs() {
        viewModelScope.launch {
            settings.phenotype_hideResultsWhenRecognitionFails = true
            settings.phenotype_smartAudioPlaybackDetection = true
            settings.phenotype_highQualityResampling = true
            settings.phenotype_useDspAudioSource = true
            settings.phenotype_suppressDuringAudioRecording = true
            navigation.navigate(NavigationEvent.NavigateUp())
        }
    }

    override fun sendUpdateIntent() {
        viewModelScope.launch {
            settings.sendUpdateIntent()
        }
    }

}