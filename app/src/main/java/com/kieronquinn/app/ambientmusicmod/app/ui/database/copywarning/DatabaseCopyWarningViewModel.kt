package com.kieronquinn.app.ambientmusicmod.app.ui.database.copywarning

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.launch

abstract class DatabaseCopyWarningViewModel: BaseViewModel() {

    abstract fun onCopyClicked()

}

class DatabaseCopyWarningViewModelImpl: DatabaseCopyWarningViewModel() {

    override fun onCopyClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(DatabaseCopyWarningFragmentDirections.actionDatabaseCopyWarningFragmentToDatabaseCopyBottomSheetFragment()))
        }
    }

}