package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BackupRestoreRestoreViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setOptions(uri: Uri, restoreOptions: RestoreOptions)
    abstract fun onCloseClicked()

    sealed class State {
        data class Loading(val restoreState: RestoreState): State()
        data class Finished(val restoreResult: RestoreResult): State()
    }

}

class BackupRestoreRestoreViewModelImpl(
    backupRestoreRepository: BackupRestoreRepository,
    private val navigation: ContainerNavigation
): BackupRestoreRestoreViewModel() {

    private val options = MutableSharedFlow<Pair<Uri, RestoreOptions>>()

    override val state = options.take(1).flatMapLatest {
        backupRestoreRepository.restoreBackup(it.first, it.second)
    }.map {
        if(it is RestoreState.RestoreComplete){
            State.Finished(it.result)
        }else{
            State.Loading(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading(RestoreState.LoadingBackup))

    override fun setOptions(uri: Uri, restoreOptions: RestoreOptions) {
        viewModelScope.launch {
            options.emit(Pair(uri, restoreOptions))
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.backupRestoreFragment)
        }
    }

}