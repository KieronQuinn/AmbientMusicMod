package com.kieronquinn.app.ambientmusicmod.app.ui.installer

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.RootFragment
import com.kieronquinn.app.ambientmusicmod.components.settings.ScrollableFragment
import com.kieronquinn.app.ambientmusicmod.constants.MIN_SOUND_TRIGGER_VERSION
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentInstallerBinding
import com.kieronquinn.app.ambientmusicmod.databinding.IncludeInstallerAboutBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class InstallerFragment: BaseFragment<FragmentInstallerBinding>(FragmentInstallerBinding::class), RootFragment, ScrollableFragment {

    private val viewModel by viewModel<InstallerViewModel>()
    private val containerViewModel by sharedViewModel<AmbientContainerSharedViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            launch {
                viewModel.moduleStatus.collect {
                    setModuleStatus(it)
                }
            }
            launch {
                viewModel.compatibilityStatus.collect {
                    setCompatibilityStatus(it)
                }
            }
            launch {
                containerViewModel.fabClick.collect {
                    onFabClicked(it)
                }
            }
        }
        lifecycleScope.launch {
            containerViewModel.forceRefreshBus.collect {
                viewModel.getModelCompatibilityStatus()
            }
        }
        setupAboutCard(binding.installerAboutContainer)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getModelStatus()
        viewModel.getCompatibilityStatus()
        viewModel.getXposedStatus()
        viewModel.getModelCompatibilityStatus()
    }

    private fun setModuleStatus(moduleStatus: InstallerViewModel.ModuleStatus){
        with(binding.installerModuleStatus){
            root.isVisible = true
            when(moduleStatus){
                is InstallerViewModel.ModuleStatus.Installed -> {
                    installerModuleStatusIcon.setImageResource(R.drawable.ic_module_check_round)
                    installerModuleStatusTitle.text = getString(R.string.installer_module_status_title_installed)
                    installerModuleStatusContent.text = getString(R.string.installer_module_status_content_version, moduleStatus.version)
                }
                is InstallerViewModel.ModuleStatus.NotInstalled -> {
                    installerModuleStatusIcon.setImageResource(R.drawable.ic_module_cross_round)
                    installerModuleStatusTitle.text = getString(R.string.installer_module_status_title_not_installed)
                    installerModuleStatusContent.text = getString(R.string.installer_module_status_content_not_installed)
                }
                is InstallerViewModel.ModuleStatus.UpdateAvailable -> {
                    installerModuleStatusIcon.setImageResource(R.drawable.ic_update_round)
                    installerModuleStatusTitle.text = getString(R.string.installer_module_status_title_update)
                    installerModuleStatusContent.text = getString(R.string.installer_module_status_content_update, moduleStatus.currentVersion, moduleStatus.newVersion)
                }
            }
        }
    }

    private fun setCompatibilityStatus(compatibilityStatus: InstallerViewModel.CompatibilityStatus){
        containerViewModel.setCompatibilityState(compatibilityStatus)
        with(binding.installerModuleCompatibility){
            root.isVisible = true
            if(compatibilityStatus.soundTriggerStatus.compatible){
                installerModuleCompatibilityCheckSoundTriggerIcon.setImageResource(R.drawable.ic_module_check)
                installerModuleCompatibilityCheckSoundTriggerText.text = getString(R.string.installer_module_compatibility_check_sound_trigger_text_compatible,
                    compatibilityStatus.soundTriggerStatus.version)
            }else{
                installerModuleCompatibilityCheckSoundTriggerIcon.setImageResource(R.drawable.ic_module_cross_small)
                installerModuleCompatibilityCheckSoundTriggerText.text = getString(R.string.installer_module_compatibility_check_sound_trigger_text_not_compatible,
                    compatibilityStatus.soundTriggerStatus.version, MIN_SOUND_TRIGGER_VERSION.toString())
            }
            if(compatibilityStatus.soundTriggerPlatformExists){
                installerModuleCompatibilityCheckSoundTriggerPlatformIcon.setImageResource(R.drawable.ic_module_check)
                installerModuleCompatibilityCheckSoundTriggerPlatformText.text = getString(R.string.installer_module_compatibility_check_sound_trigger_platform_text_compatible)
            }else{
                installerModuleCompatibilityCheckSoundTriggerPlatformIcon.setImageResource(R.drawable.ic_module_cross_small)
                installerModuleCompatibilityCheckSoundTriggerPlatformText.text = getString(R.string.installer_module_compatibility_check_sound_trigger_platform_text_not_compatible)
            }
            if(compatibilityStatus.xposedInstalled){
                installerModuleCompatibilityCheckXposedIcon.setImageResource(R.drawable.ic_module_check)
                installerModuleCompatibilityCheckXposedText.text = getString(R.string.installer_module_compatibility_check_xposed_text_installed)
            }else{
                installerModuleCompatibilityCheckXposedIcon.setImageResource(R.drawable.ic_warning)
                installerModuleCompatibilityCheckXposedText.text = getString(R.string.installer_module_compatibility_check_xposed_text_not_installed)
            }
            when(compatibilityStatus.getModelSupported){
                AmbientSharedPreferences.GetModelSupported.SUPPORTED -> {
                    installerModuleCompatibilityCheckSoundModelText.text = getString(R.string.installer_module_compatibility_check_sound_model_text_compatible)
                    installerModuleCompatibilityCheckSoundModelIcon.setImageResource(R.drawable.ic_module_check)
                }
                AmbientSharedPreferences.GetModelSupported.UNSUPPORTED -> {
                    installerModuleCompatibilityCheckSoundModelText.text = getString(R.string.installer_module_compatibility_check_sound_model_text_incompatible)
                    installerModuleCompatibilityCheckSoundModelIcon.setImageResource(R.drawable.ic_module_cross_small)
                }
                AmbientSharedPreferences.GetModelSupported.UNKNOWN -> {
                    installerModuleCompatibilityCheckSoundModelText.text = getString(R.string.installer_module_compatibility_check_sound_model_text_unknown)
                    installerModuleCompatibilityCheckSoundModelIcon.setImageResource(R.drawable.ic_warning)
                }
            }
            installerModuleCompatibilityCheckSoundModelButton.setOnClickListener {
                viewModel.onModelCheckClicked()
            }
            installerModuleCompatibilityHelp.isVisible = !compatibilityStatus.soundTriggerStatus.compatible || !compatibilityStatus.soundTriggerPlatformExists || !compatibilityStatus.xposedInstalled
            installerModuleCompatibilityHelp.setOnClickListener {
                viewModel.onHelpClicked(compatibilityStatus)
            }
        }
    }

    private fun setupAboutCard(binding: IncludeInstallerAboutBinding) = with(binding) {
        installerAboutContent.text = getString(R.string.about_desc, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
        installerFaq.setOnClickListener { viewModel.onFaqClicked() }
        installerAboutChipXda.setOnClickListener { viewModel.onXDAThreadClicked() }
        installerAboutChipGithub.setOnClickListener { viewModel.onGitHubClicked() }
        installerAboutChipDonate.setOnClickListener { viewModel.onDonateClicked() }
        installerAboutChipTwitter.setOnClickListener { viewModel.onTwitterClicked() }
    }

    private fun onFabClicked(compatibilityStatus: InstallerViewModel.CompatibilityStatus){
        viewModel.onBuildFabClicked(compatibilityStatus)
    }

    override fun scrollToTop() {
        binding.nestedScrollView.scrollTo(0, 0)
    }

}