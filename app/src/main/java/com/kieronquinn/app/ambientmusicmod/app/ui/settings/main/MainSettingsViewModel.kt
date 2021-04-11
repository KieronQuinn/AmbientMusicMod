package com.kieronquinn.app.ambientmusicmod.app.ui.settings.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.activities.HistoryLaunchActivity
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.constants.*
import com.kieronquinn.app.ambientmusicmod.utils.ModuleStateCheck
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getAppVersionCode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isXposedInstalled
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception

abstract class MainSettingsViewModel: BaseViewModel() {
    abstract val moduleState: Flow<ModuleState>
    abstract val historyActivityState: Flow<Boolean>
    abstract val developerOptionsEnabled: Flow<Boolean>
    abstract val pixelAmbientServicesVersionIncompatible: Flow<Boolean>

    abstract fun onEnabledClicked()
    abstract fun onManualTriggerClicked()
    abstract fun onAmplificationClicked()
    abstract fun onListenPeriodClicked()
    abstract fun onWhenWokenClicked()
    abstract fun onAdvancedClicked()
    abstract fun onShowHistoryClicked()
    abstract fun onShowOnLockscreenClicked()
    abstract fun onBatteryOptimisationClicked()
    abstract fun onDeveloperOptionsClicked()
    abstract fun onTrackListClicked()
    abstract fun getHistoryActivityState()
    abstract fun getModuleState()
    abstract fun getBatteryOptimisationState()
    abstract fun getDeveloperOptionsState()
    abstract fun onFaqClicked()
    abstract fun onXDAThreadClicked()
    abstract fun onDonateClicked()
    abstract fun onTwitterClicked()
    abstract fun onGitHubClicked()
    abstract fun showPASIncompatibleDialog()

    abstract fun toggleDeveloperOptions()

    abstract val runWhenWoken: Boolean

    abstract val enabledState: Flow<Boolean>
    abstract val batteryOptimisationDisabled: Flow<Boolean>

    enum class ModuleState {
        ENABLED, DISABLED, NO_XPOSED
    }
}

class MainSettingsViewModelImpl(private val context: Context): MainSettingsViewModel() {

    private val historyActivityComponent = ComponentName(context, HistoryLaunchActivity::class.java)

    private val isHistoryActivityEnabled
        get() = context.packageManager.getComponentEnabledSetting(historyActivityComponent) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    private val _moduleState = MutableSharedFlow<ModuleState>()
    private val _historyActivityState = MutableSharedFlow<Boolean>()

    override val runWhenWoken = settings.runWhenWoken

    private val _enabledState = MutableStateFlow(settings.enabled)
    override val enabledState = _enabledState.asStateFlow()

    private val _developerOptionsEnabled = MutableStateFlow(settings.developerModeEnabled)
    override val developerOptionsEnabled = _developerOptionsEnabled.asStateFlow()

    private val batteryOptimisationDisabledAMM = MutableSharedFlow<Boolean>()
    private val batteryOptimisationDisabledPAS = MutableSharedFlow<Boolean>()

    override val pixelAmbientServicesVersionIncompatible = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).apply {
        viewModelScope.launch {
            context.packageManager.getAppVersionCode(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)?.let {
                emit(it != PIXEL_AMBIENT_SERVICES_VERSION)
            } ?: run {
                emit(false)
            }
        }
    }

    override val batteryOptimisationDisabled = combine(batteryOptimisationDisabledAMM, batteryOptimisationDisabledPAS){ amm, pas ->
        amm && pas
    }

    override fun onEnabledClicked() {
        viewModelScope.launch {
            val newEnabled = !_enabledState.value
            settings.enabled = newEnabled
            _enabledState.emit(newEnabled)
            settings.sendUpdateIntent()
        }
    }

    override fun onManualTriggerClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsManualTriggerBottomSheetFragment()))
        }
    }

    override fun onAmplificationClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsAmplificationBottomSheetFragment()))
        }
    }

    override fun onListenPeriodClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsListenPeriodBottomSheetFragment()))
        }
    }

    override fun onWhenWokenClicked() {
        viewModelScope.launch {
            settings.sendUpdateIntent()
        }
    }

    override fun onAdvancedClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsAdvancedFragment()))
        }
    }

    override fun getModuleState() {
        viewModelScope.launch {
            when {
                ModuleStateCheck.isModuleEnabled() -> {
                    _moduleState.emit(ModuleState.ENABLED)
                }
                context.isXposedInstalled() -> {
                    _moduleState.emit(ModuleState.DISABLED)
                }
                else -> {
                    _moduleState.emit(ModuleState.NO_XPOSED)
                }
            }
        }
    }

    override fun getHistoryActivityState() {
        viewModelScope.launch {
            _historyActivityState.emit(isHistoryActivityEnabled)
        }
    }

    override fun getBatteryOptimisationState() {
        viewModelScope.launch {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            batteryOptimisationDisabledAMM.emit(powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID))
            try {
                batteryOptimisationDisabledPAS.emit(powerManager.isIgnoringBatteryOptimizations(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME))
            }catch (e: Exception){
                //Likely Pixel Ambient Services isn't installed, don't crash
                batteryOptimisationDisabledPAS.emit(false)
            }
        }
    }

    override fun onShowHistoryClicked() {
        viewModelScope.launch {
            toggleHistoryActivityState()
            getHistoryActivityState()
        }
    }

    override fun onShowOnLockscreenClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsLockscreenOverlayFragment()))
        }
    }

    override fun onBatteryOptimisationClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsBatteryOptimisationFragment()))
        }
    }

    override fun onDeveloperOptionsClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToSettingsDeveloperOptionsFragment()))
        }
    }

    override fun onTrackListClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToDatabaseViewerActivity()))
        }
    }

    override fun onFaqClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToFaqFragment()))
        }
    }

    override fun onXDAThreadClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(XDA_THREAD_REDIRECT_URL)
            }))
        }
    }

    override fun onDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(DONATE_REDIRECT_URL)
            }))
        }
    }

    override fun onTwitterClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(TWITTER_REDIRECT_URL)
            }))
        }
    }

    override fun onGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(GITHUB_REDIRECT_URL)
            }))
        }
    }

    override fun toggleDeveloperOptions() {
        viewModelScope.launch {
            val newState = !settings.developerModeEnabled
            settings.developerModeEnabled = newState
            if(newState){
                Toast.makeText(context, R.string.developer_options_enabled, Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(context, R.string.developer_options_disabled, Toast.LENGTH_LONG).show()
            }
            getDeveloperOptionsState()
        }
    }

    override fun getDeveloperOptionsState() {
        viewModelScope.launch {
            _developerOptionsEnabled.emit(settings.developerModeEnabled)
        }
    }

    override fun showPASIncompatibleDialog() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionSettingsFragmentToPixelAmbientServicesIncompatibleBottomSheetFragment()))
        }
    }

    private fun toggleHistoryActivityState(){
        if(isHistoryActivityEnabled){
            context.packageManager.setComponentEnabledSetting(historyActivityComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }else{
            context.packageManager.setComponentEnabledSetting(historyActivityComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }
    }

    override val moduleState: Flow<ModuleState> = _moduleState.asSharedFlow()
    override val historyActivityState: Flow<Boolean> = _historyActivityState.asSharedFlow()

}