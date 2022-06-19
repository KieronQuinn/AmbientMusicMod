package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restore

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentBackupRestoreRestoreFragmentBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restore.BackupRestoreRestoreViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreRestoreFragment: BoundFragment<FragmentBackupRestoreRestoreFragmentBinding>(FragmentBackupRestoreRestoreFragmentBinding::inflate), BackAvailable {

    private val viewModel by viewModel<BackupRestoreRestoreViewModel>()
    private val args by navArgs<BackupRestoreRestoreFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupMonet()
        setupClose()
        viewModel.setOptions(args.uri, args.options)
    }

    private fun setupMonet() {
        binding.backupRestoreRestoreLoadingProgress.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.backupRestoreRestoreClose.run {
            setTextColor(accent)
            iconTint = ColorStateList.valueOf(accent)
            overrideRippleColor(accent)
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.backupRestoreRestoreLoading.isVisible = true
                binding.backupRestoreRestoreScroll.isVisible = false
                binding.backupRestoreRestoreLoadingLabel.setText(state.restoreState.title)
            }
            is State.Finished -> {
                binding.backupRestoreRestoreLoading.isVisible = false
                binding.backupRestoreRestoreScroll.isVisible = true
                binding.backupRestoreRestoreFinishedLabel.setText(state.restoreResult.title)
                binding.backupRestoreRestoreFinishedLabelSub.setText(state.restoreResult.content)
                binding.backupRestoreRestoreFinishedIcon.setImageResource(state.restoreResult.icon)
            }
        }
    }

    private fun setupClose() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.backupRestoreRestoreClose.onClicked().collect {
            viewModel.onCloseClicked()
        }
    }

}