package com.kieronquinn.app.ambientmusicmod.app.ui.database.copy

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.databinding.BsLoadingBinding
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SecureBroadcastReceiver
import com.kieronquinn.app.ambientmusicmod.utils.extensions.keepScreenOn
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseCopyBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<DatabaseCopyViewModel>()
    private val sharedViewModel by sharedViewModel<DatabaseSharedViewModel>()

    private val copyStartedIntentFilter = IntentFilter(PixelAmbientServices.INTENT_ACTION_SEND_SUPERPACKS_START)
    private val copyFinishedIntentFilter = IntentFilter(PixelAmbientServices.INTENT_ACTION_SEND_SUPERPACKS_COMPLETE)

    private var binding by autoCleared<BsLoadingBinding>()



    private val startReceiver = SecureBroadcastReceiver { _, _ ->
        Log.d("DatabaseCopy", "start received")
        viewModel.copyStarted()
    }

    private val resultReceiver = SecureBroadcastReceiver { _, _ ->
        Log.d("DatabaseCopy", "finish received")
        viewModel.copyFinished()
    }

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.database_viewer)
        message(R.string.database_viewer_copy_awaiting)
        binding = BsLoadingBinding.inflate(layoutInflater, materialDialog.view, false)
        customView(view = binding.root)
        cancelOnTouchOutside(false)
        negativeButton(android.R.string.cancel){
            viewModel.cancelClicked()
        }
        lifecycleScope.launch {
            launch {
                viewModel.state.collect {
                    handleState(it, this@apply)
                    handleLoadingState(it.loadingState)
                }
            }
            launch {
                viewModel.closeBus.collect {
                    sharedViewModel.reload(true)
                    viewModel.closeToViewer()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keepScreenOn = true
        requireContext().registerReceiver(startReceiver, copyStartedIntentFilter)
        requireContext().registerReceiver(resultReceiver, copyFinishedIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        keepScreenOn = false
        requireContext().unregisterReceiver(startReceiver)
        requireContext().unregisterReceiver(resultReceiver)
    }

    private fun handleState(state: DatabaseCopyViewModel.CopyState, dialog: MaterialDialog) = when(state) {
        is DatabaseCopyViewModel.CopyState.AwaitingStart -> dialog.message(R.string.database_viewer_copy_awaiting)
        is DatabaseCopyViewModel.CopyState.Copying -> dialog.message(R.string.database_viewer_copy_content)
        is DatabaseCopyViewModel.CopyState.CopyComplete -> dialog.message(R.string.database_viewer_read_offsets_content)
        is DatabaseCopyViewModel.CopyState.ParseStart -> dialog.message(R.string.database_viewer_read_offsets_content)
        is DatabaseCopyViewModel.CopyState.ParsingDatabases -> dialog.message(R.string.database_viewer_read_offsets_content)
        is DatabaseCopyViewModel.CopyState.Done -> dialog
        is DatabaseCopyViewModel.CopyState.Error -> {
            if(state.errorType is DatabaseCopyViewModel.ErrorType.ParseFailed){
                dialog.message(text = getString(state.errorType.errorRes, state.errorType.file))
            }else dialog.message(state.errorType.errorRes)
            dialog.negativeButton(R.string.close){
                viewModel.cancelClicked()
            }
        }
    }

    private fun handleLoadingState(loadingState: DatabaseCopyViewModel.LoadingState) = when(loadingState){
        is DatabaseCopyViewModel.LoadingState.Hidden -> {
            binding.bsLoadingIndeterminate.isVisible = false
            binding.bsLoadingProgress.isVisible = false
        }
        is DatabaseCopyViewModel.LoadingState.Progress -> {
            binding.bsLoadingProgress.isVisible = true
            binding.bsLoadingProgress.progress = loadingState.progress
            binding.bsLoadingIndeterminate.isVisible = false
        }
        is DatabaseCopyViewModel.LoadingState.Indeterminate -> {
            binding.bsLoadingProgress.isVisible = false
            binding.bsLoadingIndeterminate.isVisible = true
        }
    }

}