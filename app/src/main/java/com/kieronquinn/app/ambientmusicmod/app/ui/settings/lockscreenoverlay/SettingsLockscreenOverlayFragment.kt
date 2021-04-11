package com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.preferences.Preference
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseSettingsFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsLockscreenOverlayFragment: BaseSettingsFragment() {

    override val viewModel by viewModel<SettingsLockscreenOverlayViewModel>()

    private val accessibilityServiceStatePreference by preference("lockscreen_overlay_accessibility_status")
    private val enabledPreference by switchPreference("lockscreen_overlay_enabled")
    private val positionPreference by preference("lockscreen_overlay_position")
    private val launchClick by switchPreference("lockscreen_launch_click")

    private val enabledRequiringPreferences by lazy {
        arrayOf(enabledPreference, positionPreference, launchClick)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.lockscreen_overlay)
        lifecycleScope.launch {
            viewModel.accessibilityEnabled.collect {
                accessibilityServiceStatePreference?.setAccessibilityStatusState(it)
                enabledRequiringPreferences.forEach { preference ->
                    preference?.isEnabled = it
                }
            }
        }
        enabledPreference?.isChecked = viewModel.lockScreenOverlayEnabled
        launchClick?.isChecked = viewModel.launchClickEnabled
        enabledPreference?.setOnClickListener(viewModel::onEnableClicked)
        positionPreference?.setOnClickListener(viewModel::onOverlayPositionClicked)
        launchClick?.setOnClickListener(viewModel::onLaunchClickClicked)
        accessibilityServiceStatePreference?.setOnClickListener(viewModel::onAccessibilityClicked)
    }

    private fun Preference.setAccessibilityStatusState(enabled: Boolean){
        title = if(enabled) getString(R.string.settings_lockscreen_overlay_state_enabled_title) else getString(R.string.settings_lockscreen_overlay_state_disabled_title)
        summary = if(enabled) null else getString(R.string.settings_lockscreen_overlay_state_disabled_desc)
        icon = if(enabled) ContextCompat.getDrawable(context, R.drawable.ic_module_check_round) else ContextCompat.getDrawable(context, R.drawable.ic_module_cross_round)
        setBackgroundTint(if(enabled) Color.TRANSPARENT else ContextCompat.getColor(context, R.color.module_cross_circle))
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAccessibilityServiceStatus()
    }

}