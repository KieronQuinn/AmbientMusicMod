package com.kieronquinn.app.ambientmusicmod.app.ui.settings.batteryoptimisation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*

abstract class SettingsBatteryOptimisationViewModel: BaseViewModel() {

    abstract val batteryOptimisationDisabledAMM: Flow<Boolean>
    abstract val batteryOptimisationDisabledPAS: Flow<Boolean>

    abstract fun getBatteryOptimisationStates()

    abstract fun onAMMDisableClick()
    abstract fun onPASDisableClick()

}

class SettingsBatteryOptimisationViewModelImpl(context: Context): SettingsBatteryOptimisationViewModel() {

    companion object {
        /**
         *  OEMs that have their own Battery Optimisation setting that may not be fully disabled by the standard REQUEST_IGNORE_BATTERY_OPTIMIZATION intent
         *  We'll always fire off to the settings on these devices.
         */
        private val ANNOYING_OEMS = arrayOf(
            "oneplus", "huawei", "samsung", "xiaomi", "meizu", "asus", "wiko", "lenovo", "oppo", "nokia", "sony"
        )
        private val BATTERY_INTENT = Intent("android.settings.APP_BATTERY_SETTINGS")
    }

    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val hasBatteryIntent by lazy {
        context.packageManager.resolveActivity(BATTERY_INTENT.apply {
            data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        }, 0) != null
    }

    private val isAnnoyingOem by lazy {
        ANNOYING_OEMS.contains(Build.MANUFACTURER.toLowerCase(Locale.getDefault())) && hasBatteryIntent
    }

    private val _batteryOptimisationDisabledAMM = MutableSharedFlow<Boolean>()
    private val _batteryOptimisationDisabledPAS = MutableSharedFlow<Boolean>()

    override val batteryOptimisationDisabledAMM = _batteryOptimisationDisabledAMM.asSharedFlow()
    override val batteryOptimisationDisabledPAS = _batteryOptimisationDisabledPAS.asSharedFlow()

    override fun getBatteryOptimisationStates() {
        viewModelScope.launch {
            _batteryOptimisationDisabledAMM.emit(powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID))
            try {
                _batteryOptimisationDisabledPAS.emit(powerManager.isIgnoringBatteryOptimizations(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME))
            }catch (e: Exception){
                //Likely Pixel Ambient Services isn't installed, don't crash
                _batteryOptimisationDisabledPAS.emit(false)
            }
        }
    }

    override fun onAMMDisableClick() {
        viewModelScope.launch {
            Log.d("AMM", "isAnnoyingOem $isAnnoyingOem hasBatteryIntent $hasBatteryIntent")
            if(!isAnnoyingOem){
                launchIgnoreOptimisations()
            }else if(hasBatteryIntent){
                launchBatterySettingsForPackage(BuildConfig.APPLICATION_ID)
            }else{
                launchAppInfoForPackage(BuildConfig.APPLICATION_ID)
            }
        }
    }

    override fun onPASDisableClick() {
        //PAS doesn't have the permission to allow automatic request disable so we'll have to launch the settings
        viewModelScope.launch {
            if(hasBatteryIntent) launchBatterySettingsForPackage(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)
            else launchAppInfoForPackage(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)
        }
    }

    private suspend fun launchIgnoreOptimisations(){
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        }
        navigation.navigate(NavigationEvent.NavigateToActivityDestination(intent))
    }

    private suspend fun launchBatterySettingsForPackage(packageName: String){
        val intent = BATTERY_INTENT.apply {
            data = Uri.parse("package:$packageName")
        }
        navigation.navigate(NavigationEvent.NavigateToActivityDestination(intent))
    }

    private suspend fun launchAppInfoForPackage(packageName: String){
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        navigation.navigate(NavigationEvent.NavigateToActivityDestination(intent))
    }

}
