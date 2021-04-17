package com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.playback

import android.content.DialogInterface
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.databinding.BsManualTriggerPlaybackBinding
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import com.kieronquinn.app.ambientmusicmod.utils.extensions.keepScreenOn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsManualTriggerPlaybackBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<SettingsManualTriggerPlaybackBottomSheetViewModel>()
    private var binding by autoCleared<BsManualTriggerPlaybackBinding>()

    private val documentPickedContract = registerForActivityResult(ActivityResultContracts.CreateDocument()){
        viewModel.onSaveToFileDocumentPicked(it)
    }

    private val avdPlayToStop by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.avd_play_to_stop) as AnimatedVectorDrawable
    }

    private val avdStopToPlay by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.avd_stop_to_play) as AnimatedVectorDrawable
    }

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        customView(R.layout.bs_manual_trigger_playback)
        binding = BsManualTriggerPlaybackBinding.bind(getCustomView())
        positiveButton(R.string.close)
        with(binding){
            bsManualTriggerPlaybackPlayPause.setOnClickListener {
                viewModel.playStop()
            }
            bsManualTriggerPlaybackMenu.setOnClickListener {
                showOverflowMenu(it)
            }
        }
        lifecycleScope.launch {
            launch {
                viewModel.state.collect {
                    handleState(it)
                }
            }
            launch {
                viewModel.loadState.collect {
                    handleLoadState(it)
                }
            }
            launch {
                viewModel.progress.collect {
                    handleProgress(it)
                }
            }
            launch {
                viewModel.shouldShowOverflow.collect {
                    binding.bsManualTriggerPlaybackMenu.isVisible = it
                }
            }
        }
    }

    private fun handleState(state: SettingsManualTriggerPlaybackBottomSheetViewModel.State) = when(state) {
        is SettingsManualTriggerPlaybackBottomSheetViewModel.State.Playing -> {
            binding.bsManualTriggerPlaybackPlayPause.setImageDrawable(avdPlayToStop)
            avdPlayToStop.start()
        }
        is SettingsManualTriggerPlaybackBottomSheetViewModel.State.Stopped -> {
            binding.bsManualTriggerPlaybackPlayPause.setImageDrawable(avdStopToPlay)
            avdStopToPlay.start()
        }
    }

    private fun handleLoadState(loadState: SettingsManualTriggerPlaybackBottomSheetViewModel.LoadState) = when(loadState) {
        is SettingsManualTriggerPlaybackBottomSheetViewModel.LoadState.Loaded -> {
            binding.bsManualTriggerPlaybackAudiowave.run {
                visibility = View.VISIBLE
                setRawData(loadState.rawBytes)
            }
        }
        is SettingsManualTriggerPlaybackBottomSheetViewModel.LoadState.Loading -> {
            binding.bsManualTriggerPlaybackAudiowave.visibility = View.INVISIBLE
        }
    }

    private fun handleProgress(progress: Float) {
        binding.bsManualTriggerPlaybackAudiowave.progress = progress
    }

    private fun showOverflowMenu(view: View){
        PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.menu_playback_debug, menu)
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.bs_manual_trigger_playback_write_to_file -> viewModel.onSaveToFileClicked(documentPickedContract)
                    R.id.bs_manual_trigger_playback_file_info -> viewModel.onFileInfoClicked()
                }
                true
            }
        }.show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.release()
    }

    override fun onResume() {
        super.onResume()
        keepScreenOn = true
        viewModel.getDeveloperModeState()
    }

    override fun onPause() {
        super.onPause()
        keepScreenOn = false
    }

}