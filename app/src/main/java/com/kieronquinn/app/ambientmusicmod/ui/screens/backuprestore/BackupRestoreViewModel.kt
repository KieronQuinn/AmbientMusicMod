package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class BackupRestoreViewModel: ViewModel() {

    abstract fun onBackupClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>)
    abstract fun onBackupLocationSelected(uri: Uri)
    abstract fun onRestoreLocationSelected(uri: Uri)

}

class BackupRestoreViewModelImpl(
    private val navigation: ContainerNavigation
): BackupRestoreViewModel() {

    companion object {
        const val BACKUP_FILE_TEMPLATE = "amm_backup_%s.ammbkp"
        private val BACKUP_MIME_TYPE = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension("ammbkp")
    }

    override fun onBackupClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getFilename())
    }

    override fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(listOfNotNull(BACKUP_MIME_TYPE).toTypedArray())
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

    private fun getFilename(): String {
        val time = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return String.format(BACKUP_FILE_TEMPLATE, dateTimeFormatter.format(time))
    }

}