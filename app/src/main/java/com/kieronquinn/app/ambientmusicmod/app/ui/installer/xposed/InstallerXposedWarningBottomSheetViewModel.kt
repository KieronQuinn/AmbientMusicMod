package com.kieronquinn.app.ambientmusicmod.app.ui.installer.xposed

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class InstallerXposedWarningBottomSheetViewModel: BaseViewModel() {

    abstract fun onIgnoreClicked()

}

class InstallerXposedWarningBottomSheetViewModelImpl: InstallerXposedWarningBottomSheetViewModel(){

    override fun onIgnoreClicked() {
        viewModelScope.launch {

        }
    }

}