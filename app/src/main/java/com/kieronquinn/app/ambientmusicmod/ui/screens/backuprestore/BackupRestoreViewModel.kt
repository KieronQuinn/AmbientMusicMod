package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.LastBackup
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.PeriodicBackupInterval
import com.kieronquinn.app.ambientmusicmod.work.PeriodicBackupWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class BackupRestoreViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onBackupClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>)
    abstract fun onBackupLocationSelected(uri: Uri)
    abstract fun onRestoreLocationSelected(uri: Uri)
    abstract fun onPeriodicBackupChanged(enabled: Boolean)
    abstract fun onPeriodicBackupIntervalChanged(interval: PeriodicBackupInterval)
    abstract fun onPeriodicBackupLocationClicked(launcher: ActivityResultLauncher<Uri?>)
    abstract fun onPeriodicBackupLocationSelected(uri: Uri)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val periodicBackupEnabled: Boolean,
            val periodicBackupLocation: Uri?,
            val periodicBackupInterval: PeriodicBackupInterval,
            val periodicBackupLastBackup: LastBackup
        ): State()
    }

}

class BackupRestoreViewModelImpl(
    private val navigation: ContainerNavigation,
    private val settingsRepository: SettingsRepository,
    context: Context
): BackupRestoreViewModel() {

    companion object {
        const val BACKUP_FILE_TEMPLATE = "amm_backup_%s.ammbkp"
    }

    private val periodicBackupEnabled = settingsRepository.periodicBackupEnabled
    private val periodicBackupUri = settingsRepository.periodicBackupUri
    private val periodicBackupInterval = settingsRepository.periodicBackupInterval
    private val periodicBackupLastBackup = settingsRepository.periodicBackupLastBackup
    private val workManager = WorkManager.getInstance(context)

    override val state = combine(
        periodicBackupEnabled.asFlow(),
        periodicBackupUri.asFlow(),
        periodicBackupInterval.asFlow(),
        periodicBackupLastBackup.asFlow()
    ) { enabled, uri, interval, last ->
        val backupUri = uri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        State.Loaded(enabled, backupUri, interval, last)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onBackupClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getFilename())
    }

    override fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(arrayOf("*/*"))
    }

    override fun onBackupLocationSelected(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(BackupRestoreFragmentDirections.actionBackupRestoreFragmentToBackupRestoreBackupFragment(uri))
        }
    }

    override fun onRestoreLocationSelected(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(BackupRestoreFragmentDirections.actionBackupRestoreFragmentToBackupRestoreOptionsFragment(uri))
        }
    }

    override fun onPeriodicBackupChanged(enabled: Boolean) {
        viewModelScope.launch {
            periodicBackupEnabled.set(enabled)
            updateWorker()
        }
    }

    override fun onPeriodicBackupIntervalChanged(interval: PeriodicBackupInterval) {
        viewModelScope.launch {
            periodicBackupInterval.set(interval)
            updateWorker()
        }
    }

    override fun onPeriodicBackupLocationSelected(uri: Uri) {
        viewModelScope.launch {
            periodicBackupUri.set(uri.toString())
            //Clear last backup as it no longer refers to the selected location
            periodicBackupLastBackup.set(LastBackup())
        }
    }

    override fun onPeriodicBackupLocationClicked(launcher: ActivityResultLauncher<Uri?>) {
        launcher.launch(null)
    }

    private suspend fun updateWorker() {
        PeriodicBackupWorker.enqueueOrCancelWorker(
            workManager,
            settingsRepository.periodicBackupEnabled.get(),
            settingsRepository.periodicBackupInterval.get()
        )
    }

    private fun getFilename(): String {
        val time = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return String.format(BACKUP_FILE_TEMPLATE, dateTimeFormatter.format(time))
    }

}