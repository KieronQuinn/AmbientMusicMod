package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository.BackupResult
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository.BackupState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BackupRestoreBackupViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setBackupUri(uri: Uri)
    abstract fun onCloseClicked()

    sealed class State {
        data class Loading(val backupState: BackupState): State()
        data class Finished(val backupResult: BackupResult): State()
    }

}

class BackupRestoreBackupViewModelImpl(
    backupRestoreRepository: BackupRestoreRepository,
    private val navigation: ContainerNavigation
): BackupRestoreBackupViewModel() {

    private val backupUri = MutableSharedFlow<Uri>()

    override val state = backupUri.take(1).flatMapLatest {
        backupRestoreRepository.createBackup(it)
    }.map {
        if(it is BackupState.BackupComplete){
            State.Finished(it.result)
        }else{
            State.Loading(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading(BackupState.Loading))

    override fun setBackupUri(uri: Uri) {
        viewModelScope.launch {
            backupUri.emit(uri)
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}