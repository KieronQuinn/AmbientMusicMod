package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.permissions

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.components.navigation.SetupNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.Companion.INTENT_ACTION_REQUEST_PERMISSIONS
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class SetupPermissionsViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun checkPermissions()
    abstract fun showPermissionPrompt()
    abstract fun moveToNext()

    sealed class State {
        object Loading: State()
        object Request: State()
        object Granted: State()
    }

}

class SetupPermissionsViewModelImpl(
    private val navigation: SetupNavigation,
    remoteSettingsRepository: RemoteSettingsRepository
): SetupPermissionsViewModel() {

    private val checkPermissionsBus = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val state = checkPermissionsBus.flatMapLatest {
        remoteSettingsRepository.permissionsGranted.map {
            if(it) State.Granted else State.Request
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun checkPermissions() {
        viewModelScope.launch {
            checkPermissionsBus.emit(Unit)
        }
    }

    override fun showPermissionPrompt() {
        val intent = Intent(INTENT_ACTION_REQUEST_PERMISSIONS).apply {
            `package` = PACKAGE_NAME_PAM
        }
        viewModelScope.launch {
            navigation.navigate(intent)
        }
    }

    override fun moveToNext() {
        viewModelScope.launch {
            navigation.navigate(SetupPermissionsFragmentDirections.actionSetupPermissionsFragmentToSetupBatteryOptimisationFragment())
        }
    }

}