package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreFragment: BaseSettingsFragment(), BackAvailable {

    override val addAdditionalPadding = true

    private val viewModel by viewModel<BackupRestoreViewModel>()

    private val backupFilePickerLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()){
        if(it == null) return@registerForActivityResult
        viewModel.onBackupLocationSelected(it)
    }

    private val restoreFilePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()){
        if(it == null) return@registerForActivityResult
        viewModel.onRestoreLocationSelected(it)
    }

    override val adapter by lazy {
        BackupRestoreAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsBaseLoading.isVisible = true
        binding.settingsBaseRecyclerView.isVisible = false
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            adapter.update(createItems(), binding.settingsBaseRecyclerView)
            binding.settingsBaseLoading.isVisible = false
            binding.settingsBaseRecyclerView.isVisible = true
        }
    }

    private fun createItems(): List<BaseSettingsItem> = listOf(
        GenericSettingsItem.Setting(
            getString(R.string.backup_restore_backup_title),
            getString(R.string.backup_restore_backup_content),
            R.drawable.ic_backup_restore_backup){
            viewModel.onBackupClicked(backupFilePickerLauncher)
        },
        GenericSettingsItem.Setting(
            getString(R.string.backup_restore_restore_title),
            getString(R.string.backup_restore_restore_content),
            R.drawable.ic_backup_restore_restore){
            viewModel.onRestoreClicked(restoreFilePickerLauncher)
        }
    )

}