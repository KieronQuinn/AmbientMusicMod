package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.dump

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.databinding.BsLoadingBinding
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsDeveloperOptionsDumpLogBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<SettingsDeveloperOptionsDumpLogViewModel>()
    private var binding by autoCleared<BsLoadingBinding>()

    private val documentPickedContract = registerForActivityResult(ActivityResultContracts.CreateDocument()){
        if(it == null) return@registerForActivityResult
        viewModel.onOutputSelected(it)
    }

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.developer_options_dump_logs)
        message(text = "")
        binding = BsLoadingBinding.inflate(layoutInflater)
        customView(view = binding.root)
        positiveButton(android.R.string.cancel){
            viewModel.cancelAndClose()
        }
        lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(materialDialog, it)
            }
        }
    }

    private fun handleState(materialDialog: MaterialDialog, state: SettingsDeveloperOptionsDumpLogViewModel.State) = when(state) {
        is SettingsDeveloperOptionsDumpLogViewModel.State.OutputSelection -> {
            viewModel.onOutputPickerLaunch(documentPickedContract)
        }
        is SettingsDeveloperOptionsDumpLogViewModel.State.RequestingRoot -> {
            materialDialog.message(R.string.developer_options_log_dump_requesting_root)
        }
        is SettingsDeveloperOptionsDumpLogViewModel.State.DumpLogs -> {
            materialDialog.message(R.string.developer_options_log_dump_creating_dump)
        }
        is SettingsDeveloperOptionsDumpLogViewModel.State.Done -> {
            binding.bsLoadingIndeterminate.isVisible = false
            materialDialog.message(R.string.developer_options_log_dump_done)
            materialDialog.positiveButton(R.string.close) { viewModel.close() }
        }
        is SettingsDeveloperOptionsDumpLogViewModel.State.Error -> {
            binding.bsLoadingIndeterminate.isVisible = false
            materialDialog.message(R.string.developer_options_log_dump_error)
            materialDialog.positiveButton(R.string.close) { viewModel.cancelAndClose() }
        }
        is SettingsDeveloperOptionsDumpLogViewModel.State.NoRoot -> {
            binding.bsLoadingIndeterminate.isVisible = false
            materialDialog.message(R.string.developer_options_log_dump_no_root)
            materialDialog.positiveButton(R.string.close) { viewModel.cancelAndClose() }
        }
        else -> {}
    }

}