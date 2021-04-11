package com.kieronquinn.app.ambientmusicmod.app.ui.settings.listenperiod

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class SettingsListenPeriodBottomSheetViewModel: BaseViewModel() {

    abstract var currentSelectedItem: Int
    abstract fun setSelectedItem(selectedItem: Int)
    abstract fun saveListenPeriod()

}

class SettingsListenPeriodBottomSheetViewModelImpl: SettingsListenPeriodBottomSheetViewModel(){

    private val jobTimeValues = arrayOf(
        0, -1, 1, 2, 3, 4, 5, 10, 15, 20, 30, 60
    )

    override var currentSelectedItem: Int = jobTimeValues.indexOf(settings.jobTime)

    override fun setSelectedItem(selectedItem: Int) {
        this.currentSelectedItem = selectedItem
    }

    override fun saveListenPeriod() {
        viewModelScope.launch {
            Log.d("AC", "new jobtime $currentSelectedItem ${jobTimeValues[currentSelectedItem]}")
            settings.jobTime = jobTimeValues[currentSelectedItem]
            settings.sendUpdateIntent()
        }
    }

}