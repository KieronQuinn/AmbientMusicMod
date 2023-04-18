package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.backup

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentBackupRestoreBackupFragmentBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.backup.BackupRestoreBackupViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreBackupFragment: BoundFragment<FragmentBackupRestoreBackupFragmentBinding>(FragmentBackupRestoreBackupFragmentBinding::inflate), BackAvailable {

    private val viewModel by viewModel<BackupRestoreBackupViewModel>()
    private val args by navArgs<BackupRestoreBackupFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupMonet()
        setupClose()
        viewModel.setBackupUri(args.uri)
    }

    private fun setupMonet() {
        binding.backupRestoreBackupLoadingProgress.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.backupRestoreBackupClose.run {
            setTextColor(accent)
            iconTint = ColorStateList.valueOf(accent)
            overrideRippleColor(accent)
        }
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
        when(state){
            is State.Loading -> {
                binding.backupRestoreBackupLoading.isVisible = true
                binding.backupRestoreBackupScroll.isVisible = false
                binding.backupRestoreBackupLoadingLabel.setText(state.backupState.title)
            }
            is State.Finished -> {
                binding.backupRestoreBackupLoading.isVisible = false
                binding.backupRestoreBackupScroll.isVisible = true
                binding.backupRestoreBackupFinishedLabel.setText(state.backupResult.title)
                binding.backupRestoreBackupFinishedLabelSub.setText(state.backupResult.content)
                binding.backupRestoreBackupFinishedIcon.setImageResource(state.backupResult.icon)
            }
        }
    }

    private fun setupClose() = whenResumed {
        binding.backupRestoreBackupClose.onClicked().collect {
            viewModel.onCloseClicked()
        }
    }

}