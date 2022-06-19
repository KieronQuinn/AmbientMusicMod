package com.kieronquinn.app.ambientmusicmod.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.judemanutd.autostarter.AutoStartPermissionHelper
import com.kieronquinn.app.ambientmusicmod.BuildConfig

interface BatteryOptimisationRepository {

    fun getDisableBatteryOptimisationsIntent(): Intent?
    fun areOemOptimisationsAvailable(context: Context): Boolean
    fun startOemOptimisationSettings(context: Context)

}

class BatteryOptimisationRepositoryImpl(
    context: Context
): BatteryOptimisationRepository {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val autoStarter = AutoStartPermissionHelper.getInstance()

    private fun areBatteryOptimisationsDisabled(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
    }

    @SuppressLint("BatteryLife")
    override fun getDisableBatteryOptimisationsIntent(): Intent? {
        if(areBatteryOptimisationsDisabled()) return null
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        }
    }

    override fun areOemOptimisationsAvailable(context: Context): Boolean {
        return autoStarter.isAutoStartPermissionAvailable(context)
    }

    override fun startOemOptimisationSettings(context: Context) {
        autoStarter.getAutoStartPermission(context, open = true, newTask = true)
    }

}