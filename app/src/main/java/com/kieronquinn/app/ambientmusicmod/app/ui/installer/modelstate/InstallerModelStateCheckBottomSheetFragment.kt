package com.kieronquinn.app.ambientmusicmod.app.ui.installer.modelstate

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentInstallerModelCheckBinding
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class InstallerModelStateCheckBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<InstallerModelStateCheckBottomSheetViewModel>()
    private val containerViewModel by sharedViewModel<AmbientContainerSharedViewModel>()
    private var binding by autoCleared<FragmentInstallerModelCheckBinding>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.installer_get_model_state_check_title)
        customView(R.layout.fragment_installer_model_check)
        noAutoDismiss()
        binding = FragmentInstallerModelCheckBinding.bind(getCustomView())
        lifecycleScope.launchWhenCreated {
            viewModel.state.collect {
                handleState(materialDialog, it)
            }
        }
        binding.installerModelCheckOverrideCheck.setOnCheckedChangeListener { _, checked ->
            materialDialog.getActionButton(WhichButton.NEUTRAL).isEnabled = checked
        }
        positiveButton(android.R.string.cancel){ materialDialog.dismiss() }
    }

    private fun handleState(materialDialog: MaterialDialog, state: InstallerModelStateCheckBottomSheetViewModel.State) = when(state) {
        is InstallerModelStateCheckBottomSheetViewModel.State.AwaitingService -> {
            binding.installerModelCheckTitle.text = getString(R.string.installer_get_model_state_check_state_awaiting_title)
            binding.installerModelCheckContent.text = getString(R.string.installer_get_model_state_check_state_awaiting_content)
            binding.installerModelCheckSpin.isInvisible = false
            binding.installerModelCheckIcon.isInvisible = true
            binding.installerModelCheckOverrideCheck.isVisible = false
        }
        is InstallerModelStateCheckBottomSheetViewModel.State.ServiceConnected -> {
            binding.installerModelCheckTitle.text = getString(R.string.installer_get_model_state_check_state_running_title)
            binding.installerModelCheckContent.text = ""
            binding.installerModelCheckSpin.isInvisible = false
            binding.installerModelCheckIcon.isInvisible = true
            binding.installerModelCheckOverrideCheck.isVisible = false
        }
        is InstallerModelStateCheckBottomSheetViewModel.State.ResultReceived -> {
            if(state.supported == AmbientSharedPreferences.GetModelSupported.SUPPORTED){
                binding.installerModelCheckTitle.text = getString(R.string.installer_get_model_state_check_state_result_title_compatible)
                binding.installerModelCheckIcon.setImageResource(R.drawable.ic_module_check)
                binding.installerModelCheckIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.module_check))
                binding.installerModelCheckContent.text = getString(R.string.installer_get_model_state_check_state_result_content, state.result)
                binding.installerModelCheckOverrideCheck.isVisible = false
            }else{
                binding.installerModelCheckTitle.text = getString(R.string.installer_get_model_state_check_state_result_title_incompatible)
                binding.installerModelCheckIcon.setImageResource(R.drawable.ic_error)
                binding.installerModelCheckIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.module_cross))
                binding.installerModelCheckContent.text = getString(R.string.installer_get_model_state_check_state_result_content_failed, state.result)
                binding.installerModelCheckOverrideCheck.isVisible = true
                materialDialog.neutralButton(R.string.installer_get_model_state_check_state_result_content_failed_override){
                    viewModel.onOverrideClicked()
                }
                materialDialog.getActionButton(WhichButton.NEUTRAL).isEnabled = false
            }
            binding.installerModelCheckSpin.isInvisible = true
            binding.installerModelCheckIcon.isInvisible = false
            materialDialog.positiveButton(R.string.close){ materialDialog.dismiss() }
            Unit
        }
    }

    override fun onPause() {
        super.onPause()
        containerViewModel.forceRefresh()
    }

}