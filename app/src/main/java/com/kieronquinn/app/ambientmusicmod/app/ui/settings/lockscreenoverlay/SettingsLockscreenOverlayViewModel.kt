package com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.app.service.LockscreenOverlayAccessibilityService
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isAccessibilityServiceEnabled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

abstract class SettingsLockscreenOverlayViewModel: BaseViewModel() {

    abstract val accessibilityEnabled: Flow<Boolean>
    abstract val lockScreenOverlayEnabled: Boolean
    abstract val launchClickEnabled: Boolean

    abstract fun getAccessibilityServiceStatus()
    abstract fun onOverlayPositionClicked()
    abstract fun onEnableClicked()
    abstract fun onAccessibilityClicked()
    abstract fun onLaunchClickClicked()

}

class SettingsLockscreenOverlayViewModelImpl(private val context: Context): SettingsLockscreenOverlayViewModel() {

    companion object {
        const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
        const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
    }

    private val _accessibilityEnabled = MutableSharedFlow<Boolean>()
    override val accessibilityEnabled: Flow<Boolean> = _accessibilityEnabled.asSharedFlow()

    override val lockScreenOverlayEnabled: Boolean
        get() = settings.lockScreenOverlayEnabled

    override val launchClickEnabled: Boolean
        get() = settings.lockScreenOverlayLaunchClick

    private val settingsAccessibilityIntent by lazy {
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val bundle = Bundle()
            val componentName = ComponentName(BuildConfig.APPLICATION_ID, LockscreenOverlayAccessibilityService::class.java.name).flattenToString()
            bundle.putString(EXTRA_FRAGMENT_ARG_KEY, componentName)
            putExtra(EXTRA_FRAGMENT_ARG_KEY, componentName)
            putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
        }
    }

    override fun getAccessibilityServiceStatus() {
        viewModelScope.launch {
            _accessibilityEnabled.emit(isAccessibilityServiceEnabled(context, LockscreenOverlayAccessibilityService::class.java))
        }
    }

    override fun onOverlayPositionClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsLockscreenOverlayFragmentDirections.actionSettingsLockscreenOverlayFragmentToSettingsLockscreenOverlayPositionActivity()))
        }
    }

    override fun onAccessibilityClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(settingsAccessibilityIntent))
        }
    }

    override fun onEnableClicked() {
        viewModelScope.launch {
            settings.lockScreenOverlayEnabled = !settings.lockScreenOverlayEnabled
            settings.sendUpdateIntent()
        }
    }

    override fun onLaunchClickClicked() {
        viewModelScope.launch {
            settings.lockScreenOverlayLaunchClick = !settings.lockScreenOverlayLaunchClick
            settings.sendUpdateIntent()
        }
    }

}