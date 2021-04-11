package com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.components.superpacks.Superpacks
import com.kieronquinn.app.ambientmusicmod.utils.extensions.broadcastReceiverFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class DatabaseViewerViewModel: BaseViewModel() {

    abstract val selectedScreenId: Flow<Int>
    abstract val shouldShowControls: Flow<Boolean>
    abstract val shouldShowUpdateBanner: Flow<Boolean>
    abstract val loadBus: Flow<Boolean>

    abstract fun setSelectedScreen(id: Int)
    abstract fun setCurrentDestination(id: Int)
    abstract fun onUpdateBannerClicked()
    abstract fun sendSuperpacksUpdateCheckBroadcast()
    abstract fun onLoaded()

}

class DatabaseViewerViewModelImpl(private val context: Context) : DatabaseViewerViewModel() {

    private val _selectedScreenId = MutableStateFlow(0)
    override val selectedScreenId = _selectedScreenId.asStateFlow()

    private val currentDestination = MutableStateFlow(0)

    private val _loadBus = MutableStateFlow(false)
    override val loadBus = _loadBus.asSharedFlow()

    override val shouldShowControls = MutableStateFlow(false).apply {
        viewModelScope.launch {
            currentDestination.collect {
                emit(when(it){
                    R.id.databaseViewerArtistsFragment, R.id.databaseViewerTracksFragment -> true
                    else -> false
                })
            }
        }
    }

    private val superpacksUpdateIntentFilter = IntentFilter(PixelAmbientServices.INTENT_ACTION_RESPONSE_SUPERPACKS_VERSION)
    private val isSuperpacksUpdateAvailable = context.broadcastReceiverFlow(superpacksUpdateIntentFilter).map {
        val localSuperpacksVersion = Superpacks.getSuperpackVersion(context, Superpacks.SUPERPACK_AMBIENT_MUSIC_INDEX)
        val remoteSuperpacksVersion = it?.getIntExtra(PixelAmbientServices.INTENT_RESPONSE_SUPERPACKS_VERSION_EXTRA_VERSION, 0) ?: 0
        Log.d("XASuperpacks", "Got response, local version $localSuperpacksVersion remote version $remoteSuperpacksVersion")
        localSuperpacksVersion != remoteSuperpacksVersion
    }

    override val shouldShowUpdateBanner = combine(loadBus, isSuperpacksUpdateAvailable){ loaded, update ->
        Log.d("XASuperpacks", "shouldShowUpdateBanner")
        loaded && update
    }

    override fun setSelectedScreen(id: Int) {
        viewModelScope.launch {
            _selectedScreenId.emit(id)
        }
    }

    override fun setCurrentDestination(id: Int) {
        viewModelScope.launch {
            currentDestination.emit(id)
        }
    }

    override fun onUpdateBannerClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(DatabaseViewerFragmentDirections.actionDatabaseViewerFragment2ToDatabaseCopyWarningFragment()))
        }
    }

    override fun onLoaded() {
        Log.d("XASuperpacks", "onLoaded")
        viewModelScope.launch {
            _loadBus.emit(true)
        }
    }

    override fun sendSuperpacksUpdateCheckBroadcast() {
        Log.d("XASuperpacks", "sendSuperpacksUpdateCheckBroadcast")
        context.sendSecureBroadcast(Intent(PixelAmbientServices.INTENT_ACTION_REQUEST_SUPERPACKS_VERSION).apply {
            `package` = PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
        })
    }

}