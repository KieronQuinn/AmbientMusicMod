package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore

import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.PeriodicBackupInterval
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.BackupRestoreViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.takeUriPermission
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.app.ambientmusicmod.work.PeriodicBackupWorker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.URLDecoder
import java.time.Instant
import java.util.Date

class BackupRestoreFragment: BaseSettingsFragment(), BackAvailable {

    override val addAdditionalPadding = true

    private val viewModel by viewModel<BackupRestoreViewModel>()

    private val backupFilePickerLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")){
        if(it == null) return@registerForActivityResult
        whenResumed {
            viewModel.onBackupLocationSelected(it)
        }
    }

    private val restoreFilePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()){
        if(it == null) return@registerForActivityResult
        whenResumed {
            viewModel.onRestoreLocationSelected(it)
        }
    }

    private val periodicBackupLocationLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if(it == null) return@registerForActivityResult
        requireContext().contentResolver.takeUriPermission(it)
        viewModel.onPeriodicBackupLocationSelected(it)
    }

    override val adapter by lazy {
        BackupRestoreAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.createItems(), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.createItems(): List<BaseSettingsItem> {
        val items = listOf(
            GenericSettingsItem.Setting(
                getString(R.string.backup_restore_backup_title),
                getString(R.string.backup_restore_backup_content),
                R.drawable.ic_backup_restore_backup
            ) {
                viewModel.onBackupClicked(backupFilePickerLauncher)
            },
            GenericSettingsItem.Setting(
                getString(R.string.backup_restore_restore_title),
                getString(R.string.backup_restore_restore_content),
                R.drawable.ic_backup_restore_restore
            ) {
                viewModel.onRestoreClicked(restoreFilePickerLauncher)
            },
            GenericSettingsItem.Header(
                getString(R.string.backup_automatic_title)
            ),
            GenericSettingsItem.SwitchSetting(
                periodicBackupEnabled,
                getString(R.string.backup_periodic_title),
                getString(R.string.backup_periodic_content),
                R.drawable.ic_backup_periodic,
                onChanged = viewModel::onPeriodicBackupChanged
            )
        )
        if(!periodicBackupEnabled) return items
        return items + listOf(
            GenericSettingsItem.Dropdown(
                getString(R.string.backup_periodic_interval_title),
                getString(periodicBackupInterval.title),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_backup_periodic_interval),
                periodicBackupInterval,
                viewModel::onPeriodicBackupIntervalChanged,
                PeriodicBackupInterval.values().toList()
            ) {
                it.title
            },
            GenericSettingsItem.Setting(
                getString(R.string.backup_periodic_location_title),
                getBackupLocationContent(
                    periodicBackupLocation,
                    periodicBackupLastBackup,
                    periodicBackupInterval
                ),
                R.drawable.ic_backup_periodic_location
            ) {
                viewModel.onPeriodicBackupLocationClicked(periodicBackupLocationLauncher)
            }
        )
    }

    private fun String.getKnownPathOrNull(): String? {
        return if(contains("%3A")){
            substring(lastIndexOf("%3A") + 3).let {
                URLDecoder.decode(it, "UTF-8")
            }
        }else null
    }

    private fun getBackupLocationContent(
        uri: Uri?,
        lastBackup: SettingsRepository.LastBackup,
        interval: PeriodicBackupInterval
    ) = SpannableStringBuilder().apply {
        val location = if(uri != null){
            uri.toString().getKnownPathOrNull()
                ?: getString(R.string.backup_periodic_location_external)
        } else getString(R.string.backup_periodic_location_unset)
        append(location)
        if(lastBackup.timestamp != null && lastBackup.result != null) {
            appendLine()
            val time = Instant.ofEpochMilli(lastBackup.timestamp).format()
            append(getText(R.string.backup_periodic_location_last))
            append(" ")
            append(time)
        }
        appendLine()
        val nextTime = PeriodicBackupWorker.getNextTime(interval).toInstant().format()
        append(getText(R.string.backup_periodic_location_next))
        append(" ")
        append(nextTime)
    }

    private fun Instant.format(): String {
        val dateFormat = DateFormat.getDateFormat(requireContext())
        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val date = Date.from(this)
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

}