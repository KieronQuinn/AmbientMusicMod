package com.kieronquinn.app.ambientmusicmod.app.ui.installer.notcompatible

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.constants.MIN_SOUND_TRIGGER_VERSION
import com.kieronquinn.app.ambientmusicmod.databinding.InstallerNotCompatibleBinding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getText

class InstallerNotCompatibleFragment: BaseFragment<InstallerNotCompatibleBinding>(InstallerNotCompatibleBinding::class) {

    private val navArguments by navArgs<InstallerNotCompatibleFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(navArguments.compatibilityStatus){
            if(!soundTriggerStatus.compatible){
                binding.installerNotCompatibleSoundTrigger.isVisible = true
                binding.installerNotCompatibleSoundTriggerContent.text = requireContext().getText(R.string.installer_not_compatible_sound_trigger_content, soundTriggerStatus.version, MIN_SOUND_TRIGGER_VERSION.toString())
            }else binding.installerNotCompatibleSoundTrigger.isVisible = false
            binding.installerNotCompatibleSoundTriggerPlatform.isVisible = !soundTriggerPlatformExists
            binding.installerNotCompatibleXposed.isVisible = !xposedInstalled
        }
    }

}