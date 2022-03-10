package com.kieronquinn.app.ambientmusicmod.app.ui.installer.build

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.installer.SoundTriggerPlatformXML
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentInstallerBuildBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallerBuildFragment: BaseFragment<FragmentInstallerBuildBinding>(FragmentInstallerBuildBinding::class) {

    private val viewModel by viewModel<InstallerBuildViewModel>()
    private val arguments by navArgs<InstallerBuildFragmentArgs>()

    private val avdFolderToCross by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.avd_folder_to_cross) as AnimatedVectorDrawable
    }

    private val avdFolderToZip by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.avd_folder_to_zip) as AnimatedVectorDrawable
    }

    private val avdZipToCheck by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.avd_zip_to_check) as AnimatedVectorDrawable
    }

    private val avdZipToCross by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.avd_zip_to_cross) as AnimatedVectorDrawable
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.installerBuildProgress.isIndeterminate = true
        binding.installerBuildClose.setOnClickListener {
            viewModel.onCloseClicked()
        }
        lifecycleScope.launch {
            viewModel.installState.collect {
                handleState(it)
            }
        }
        viewModel.start(arguments.outputUri)
    }

    private fun handleState(state: InstallerBuildViewModel.InstallState) = when(state) {
        is InstallerBuildViewModel.InstallState.Idle -> {}
        is InstallerBuildViewModel.InstallState.CreatingModule -> {
            binding.installerBuildTitle.text = getString(R.string.installer_build_title)
            binding.installerBuildContent.text = getString(R.string.installer_build_building_module)
            avdFolderToZip.reset()
            binding.installerBuildAvd.setImageDrawable(avdFolderToZip)
            binding.installerBuildProgress.isVisible = true
            binding.installerBuildClose.visibility = View.INVISIBLE
            binding.installerBuildClose.isEnabled = false
        }
        is InstallerBuildViewModel.InstallState.ZippingModule -> {
            binding.installerBuildTitle.text = getString(R.string.installer_build_title)
            binding.installerBuildContent.text = getString(R.string.installer_build_zipping_module)
            binding.installerBuildAvd.setImageDrawable(avdFolderToZip)
            avdFolderToZip.start()
            binding.installerBuildProgress.isVisible = true
            binding.installerBuildClose.visibility = View.INVISIBLE
            binding.installerBuildClose.isEnabled = false
        }
        is InstallerBuildViewModel.InstallState.CopyingModule -> {
            binding.installerBuildTitle.text = getString(R.string.installer_build_title)
            binding.installerBuildContent.text = getString(R.string.installer_build_copying_module)
            binding.installerBuildProgress.isVisible = true
            binding.installerBuildClose.visibility = View.INVISIBLE
            binding.installerBuildClose.isEnabled = false
        }
        is InstallerBuildViewModel.InstallState.Done -> {
            binding.installerBuildTitle.text = getString(R.string.installer_build_done)
            binding.installerBuildContent.text = getString(R.string.installer_build_done_content, state.outputFilename)
            binding.installerBuildAvd.setImageDrawable(avdZipToCheck)
            avdZipToCheck.start()
            binding.installerBuildProgress.isVisible = false
            binding.installerBuildClose.visibility = View.VISIBLE
            binding.installerBuildClose.isEnabled = true
        }
        is InstallerBuildViewModel.InstallState.Error -> {
            handleErrorState(state.errorReason)
            binding.installerBuildTitle.text = getString(R.string.installer_build_error_title)
            binding.installerBuildProgress.isVisible = false
            binding.installerBuildClose.visibility = View.VISIBLE
            binding.installerBuildClose.isEnabled = true
        }
    }

    private fun handleErrorState(errorState: InstallerBuildViewModel.ErrorReason) = when(errorState) {
        is InstallerBuildViewModel.ErrorReason.CopyFailure -> {
            binding.installerBuildContent.text = getString(R.string.installer_build_error_copying)
            binding.installerBuildAvd.setImageDrawable(avdZipToCross)
            avdFolderToCross.start()
        }
        is InstallerBuildViewModel.ErrorReason.ZipFailure -> {
            binding.installerBuildContent.text = getString(R.string.installer_build_zipping_module)
            binding.installerBuildAvd.setImageDrawable(avdZipToCross)
            avdFolderToCross.start()
        }
        is InstallerBuildViewModel.ErrorReason.CreateFailure -> {
            val errorText = when(errorState.reason){
                SoundTriggerPlatformXML.CurrentXMLType.ALREADY_INSTALLED -> getString(R.string.installer_build_error_building_already_installed)
                SoundTriggerPlatformXML.CurrentXMLType.INCOMPATIBLE -> getString(R.string.installer_build_error_building_incompatible)
                else -> ""
            }
            binding.installerBuildContent.text = getString(R.string.installer_build_error_building, errorText)
            binding.installerBuildAvd.setImageDrawable(avdFolderToCross)
            avdFolderToCross.start()
        }
    }

}