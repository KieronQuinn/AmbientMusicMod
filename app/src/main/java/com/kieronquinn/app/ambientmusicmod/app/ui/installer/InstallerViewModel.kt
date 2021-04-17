package com.kieronquinn.app.ambientmusicmod.app.ui.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.installer.Installer
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.constants.*
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getInt
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getString
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isXposedInstalled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

abstract class InstallerViewModel: BaseViewModel() {

    abstract val moduleStatus: Flow<ModuleStatus>
    abstract val compatibilityStatus: Flow<CompatibilityStatus>

    abstract fun getModelStatus()
    abstract fun getCompatibilityStatus()
    abstract fun getXposedStatus()
    abstract fun getModelCompatibilityStatus()

    abstract fun onBuildFabClicked(compatibilityStatus: CompatibilityStatus)
    abstract fun onHelpClicked(compatibilityStatus: CompatibilityStatus)
    abstract fun onModelCheckClicked()

    abstract fun onFaqClicked()
    abstract fun onXDAThreadClicked()
    abstract fun onGitHubClicked()
    abstract fun onDonateClicked()
    abstract fun onTwitterClicked()

    sealed class ModuleStatus {
        object NotInstalled: ModuleStatus()
        data class Installed(val version: String): ModuleStatus()
        data class UpdateAvailable(val currentVersion: String, val newVersion: String): ModuleStatus()
    }

    @Parcelize
    data class SoundTriggerStatus(val version: String, val compatible: Boolean): Parcelable

    @Parcelize
    data class CompatibilityStatus(val soundTriggerStatus: SoundTriggerStatus, val soundTriggerPlatformExists: Boolean, val xposedInstalled: Boolean, val getModelSupported: AmbientSharedPreferences.GetModelSupported): Parcelable {

        fun toEnum(): AmbientContainerSharedViewModel.CompatibilityState {
            return when {
                getModelSupported == AmbientSharedPreferences.GetModelSupported.SUPPORTED && soundTriggerStatus.compatible && soundTriggerPlatformExists && xposedInstalled -> AmbientContainerSharedViewModel.CompatibilityState.COMPATIBLE
                getModelSupported == AmbientSharedPreferences.GetModelSupported.SUPPORTED && soundTriggerStatus.compatible && soundTriggerPlatformExists && !xposedInstalled -> AmbientContainerSharedViewModel.CompatibilityState.NO_XPOSED
                getModelSupported == AmbientSharedPreferences.GetModelSupported.UNKNOWN -> AmbientContainerSharedViewModel.CompatibilityState.NEED_MODULE_CHECK
                else -> AmbientContainerSharedViewModel.CompatibilityState.NOT_COMPATIBLE
            }
        }

    }

}

class InstallerViewModelImpl(private val context: Context): InstallerViewModel() {

    private val _moduleStatus = MutableSharedFlow<ModuleStatus>()
    override val moduleStatus: Flow<ModuleStatus> = _moduleStatus.asSharedFlow()

    private val soundTriggerStatus = MutableSharedFlow<SoundTriggerStatus>()
    private val soundTriggerPlatformExists = MutableSharedFlow<Boolean>()

    private val xposedInstalled = MutableSharedFlow<Boolean>()

    private val modelCompatibilityStatus = MutableSharedFlow<AmbientSharedPreferences.GetModelSupported>()

    override val compatibilityStatus: Flow<CompatibilityStatus> = combine(soundTriggerStatus, soundTriggerPlatformExists, xposedInstalled, modelCompatibilityStatus){ status: SoundTriggerStatus, exists: Boolean, xposed: Boolean, getModelSupported: AmbientSharedPreferences.GetModelSupported ->
        CompatibilityStatus(status, exists, xposed, getModelSupported)
    }

    override fun getModelStatus() {
        viewModelScope.launch {
            val version = SystemProperties_getInt(MODULE_VERSION_CODE_PROP, -1)
            val versionName = SystemProperties_getString(MODULE_VERSION_PROP, "")
            if(version != -1){
                if(version < BUILD_MODULE_VERSION_CODE){
                    _moduleStatus.emit(ModuleStatus.UpdateAvailable(versionName, BUILD_MODULE_VERSION))
                }else {
                    _moduleStatus.emit(ModuleStatus.Installed(versionName))
                }
            }else{
                _moduleStatus.emit(ModuleStatus.NotInstalled)
            }
        }
    }

    override fun getCompatibilityStatus() {
        viewModelScope.launch {
            val platformFileExists = File(SOUND_TRIGGER_PLATFORM_PATH).exists()
            soundTriggerPlatformExists.emit(platformFileExists)
            val maxSoundTriggerVersion = Installer.getMaxSoundTriggerVersion()
            soundTriggerStatus.emit(SoundTriggerStatus(maxSoundTriggerVersion.toString(), maxSoundTriggerVersion >= MIN_SOUND_TRIGGER_VERSION))
        }
    }

    override fun getXposedStatus() {
        viewModelScope.launch {
            xposedInstalled.emit(context.isXposedInstalled())
        }
    }

    override fun getModelCompatibilityStatus() {
        viewModelScope.launch {
            modelCompatibilityStatus.emit(settings.getModelSupported)
        }
    }

    override fun onBuildFabClicked(compatibilityStatus: CompatibilityStatus) {
        viewModelScope.launch {
            when(compatibilityStatus.toEnum()){
                AmbientContainerSharedViewModel.CompatibilityState.NO_XPOSED -> {
                    navigation.navigate(NavigationEvent.NavigateByDirections(InstallerFragmentDirections.actionInstallerFragmentToInstallerXposedWarningBottomSheetFragment()))
                }
                AmbientContainerSharedViewModel.CompatibilityState.NOT_COMPATIBLE -> {
                    navigation.navigate(NavigationEvent.NavigateByDirections(InstallerFragmentDirections.actionInstallerFragmentToInstallerNotCompatibleFragment(compatibilityStatus)))
                }
                AmbientContainerSharedViewModel.CompatibilityState.COMPATIBLE -> {
                    navigation.navigate(NavigationEvent.NavigateByDirections(InstallerFragmentDirections.actionInstallerFragmentToInstallerOutputPickerFragment()))
                }
                AmbientContainerSharedViewModel.CompatibilityState.NEED_MODULE_CHECK -> {
                    Toast.makeText(context, context.getString(R.string.installer_get_model_state_run_toast), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onHelpClicked(compatibilityStatus: CompatibilityStatus) {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(InstallerFragmentDirections.actionInstallerFragmentToInstallerNotCompatibleFragment(compatibilityStatus)))
        }
    }

    override fun onFaqClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(InstallerFragmentDirections.actionInstallerFragmentToFaqFragment()))
        }
    }

    override fun onXDAThreadClicked() {
        launchURL(XDA_THREAD_REDIRECT_URL)
    }

    override fun onGitHubClicked() {
        launchURL(GITHUB_REDIRECT_URL)
    }

    override fun onDonateClicked() {
        launchURL(DONATE_REDIRECT_URL)
    }

    override fun onTwitterClicked() {
        launchURL(TWITTER_REDIRECT_URL)
    }

    override fun onModelCheckClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(InstallerFragmentDirections.actionInstallerFragmentToInstallerModelStateCheckBottomSheetFragment()))
        }
    }

    private fun launchURL(url: String){
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }))
        }
    }

}