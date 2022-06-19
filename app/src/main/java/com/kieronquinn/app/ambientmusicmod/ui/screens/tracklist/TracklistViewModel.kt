package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.Scopes
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.components.navigation.TracklistNavigation
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseContainerViewModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent

abstract class TracklistViewModel: ViewModel(), BaseContainerViewModel, KoinScopeComponent

class TracklistViewModelImpl(
    private val tracklistNavigation: TracklistNavigation,
    private val rootNavigation: RootNavigation
): TracklistViewModel() {

    override val scope by lazy {
        getKoin().getOrCreateScope<TracklistViewModel>(Scopes.TRACK_LIST.name)
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            tracklistNavigation.navigateBack()
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.close()
    }

    override fun onParentBackPressed(): Boolean {
        viewModelScope.launch {
            rootNavigation.navigateBack()
        }
        return true
    }

}