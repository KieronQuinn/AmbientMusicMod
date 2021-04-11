package com.kieronquinn.app.ambientmusicmod.app.ui.settings.amplification

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

abstract class SettingsAmplificationBottomSheetViewModel: BaseViewModel() {

    abstract var currentGain: Float
    abstract fun saveGain()
    abstract fun setGainByProgress(progress: Float)

}

class SettingsAmplificationBottomSheetViewModelImpl: SettingsAmplificationBottomSheetViewModel() {

    override var currentGain: Float = min(settings.recordGain / 100f, 1f)

    override fun saveGain() {
        viewModelScope.launch {
            settings.recordGain = (currentGain * 100).roundToInt().toFloat()
            settings.sendUpdateIntent()
        }
    }

    override fun setGainByProgress(progress: Float) {
        currentGain = progress
    }

}