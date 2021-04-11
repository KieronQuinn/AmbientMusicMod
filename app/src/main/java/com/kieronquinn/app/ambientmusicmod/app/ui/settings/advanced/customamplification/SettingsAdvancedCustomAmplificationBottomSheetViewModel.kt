package com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced.customamplification

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class SettingsAdvancedCustomAmplificationBottomSheetViewModel: BaseViewModel() {

    abstract val amplification: Flow<Float?>
    abstract val errorState: Flow<Boolean>

    abstract fun setAmplification(amplification: String)
    abstract fun saveAmplification()
}

class SettingsAdvancedCustomAmplificationBottomSheetViewModelImpl: SettingsAdvancedCustomAmplificationBottomSheetViewModel() {

    private val _amplification: MutableStateFlow<Float?> = MutableStateFlow(settings.recordGain)
    override val amplification: Flow<Float?> = _amplification

    override val errorState: Flow<Boolean> = MutableSharedFlow<Boolean>().apply {
        viewModelScope.launch {
            amplification.collect {
                emit(it == null)
            }
        }
    }

    override fun setAmplification(amplification: String) {
        viewModelScope.launch {
            _amplification.emit(amplification.toFloatOrNull())
        }
    }

    override fun saveAmplification() {
        viewModelScope.launch {
            settings.recordGain = _amplification.value ?: AmbientSharedPreferences.DEFAULT_RECORD_GAIN
            settings.sendUpdateIntent()
        }
    }

}