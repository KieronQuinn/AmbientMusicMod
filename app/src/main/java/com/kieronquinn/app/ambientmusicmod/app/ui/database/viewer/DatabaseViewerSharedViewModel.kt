package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.components.superpacks.Superpacks
import com.kieronquinn.app.ambientmusicmod.utils.extensions.broadcastReceiverFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class DatabaseViewerSharedViewModel: BaseViewModel() {

    abstract val loadBus: Flow<Unit>
    abstract val navigationBus: Flow<NavigationEvent>
    abstract val searchTerm: Flow<String>

    abstract fun navigate(navigationEvent: NavigationEvent)
    abstract fun onUpdateBannerClicked()
    abstract fun setSearchTerm(searchTerm: String)
}

class DatabaseViewerSharedViewModelImpl(private val context: Context) : DatabaseViewerSharedViewModel() {

    private val _navigationBus = MutableSharedFlow<NavigationEvent>()
    override val navigationBus = _navigationBus.asSharedFlow()

    private val _searchTerm = MutableStateFlow("")
    override val searchTerm = _searchTerm.asStateFlow()

    private val _loadBus = MutableSharedFlow<Unit>().apply {
        viewModelScope.launch {
            emit(Unit)
        }
    }

    override val loadBus = _loadBus.asSharedFlow()

    override fun navigate(navigationEvent: NavigationEvent) {
        viewModelScope.launch {
            _navigationBus.emit(navigationEvent)
        }
    }

    override fun onUpdateBannerClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(DatabaseViewerFragmentDirections.actionDatabaseViewerFragment2ToDatabaseCopyWarningFragment()))
        }
    }

    override fun setSearchTerm(searchTerm: String) {
        viewModelScope.launch {
            _searchTerm.emit(searchTerm)
        }
    }

}