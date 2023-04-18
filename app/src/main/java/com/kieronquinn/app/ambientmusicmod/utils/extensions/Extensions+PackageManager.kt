package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build

fun PackageManager.isPermissionGranted(packageName: String, vararg permission: String): Boolean {
    val packageInfo = try {
        getPackageInfoCompat(packageName, PackageManager.GET_PERMISSIONS)
    }catch (e: PackageManager.NameNotFoundException){
        //Not installed
        return true
    }
    return packageInfo.requestedPermissions
        .zip(packageInfo.requestedPermissionsFlags.toTypedArray())
        .filter { permission.contains(it.first) }
        .also { if(it.size != permission.size) return false } //Missing at least one permission
        .all { it.second and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
}

fun PackageManager.isAppInstalled(packageName: String): Boolean {
    return try {
        getPackageInfoCompat(packageName, 0)
        true
    }catch (e: PackageManager.NameNotFoundException){
        false
    }
}

fun PackageManager.getSplits(packageName: String): Array<String> {
    val applicationInfo = try {
        getApplicationInfo(packageName)
    }catch (e: PackageManager.NameNotFoundException){
        return emptyArray()
    }
    return applicationInfo.splitNames ?: emptyArray()
}

/**
 *  Finds the Keyguard package (usually SystemUI) from the framework. If the config value
 *  isn't found or isn't set somehow, it will default to [PACKAGE_KEYGUARD_DEFAULT].
 *
 *  This handles some OEMs who use a custom SystemUI package.
 */
@SuppressLint("DiscouragedApi")
private fun findKeyguard(): String? {
    val resources = Resources.getSystem()
    val configKeyguard = resources.getIdentifier(
        "config_keyguardComponent", "string", "android"
    )
    if(configKeyguard == 0) return null
    val keyguardString = resources.getString(configKeyguard)
    if(!keyguardString.contains("/")) return null
    val component = ComponentName.unflattenFromString(keyguardString)
    return component?.packageName
}

/**
 *  Returns the SystemUI package, which is not always `com.android.systemui`. Finds it using the
 *  shared user ID `android.uid.systemui`, which does seem to always be used. If this breaks in the
 *  future, `com.android.systemui` will be returned.
 */
@SuppressLint("QueryPermissionsNeeded")
private fun PackageManager.findSystemUI(): String {
    return getInstalledPackagesCompat(PackageManager.MATCH_SYSTEM_ONLY).firstOrNull {
        it.sharedUserId == "android.uid.systemui"
    }?.packageName ?: "com.android.systemui"
}

/**
 *  Attempts to find the SystemUI package using primarily the Keyguard component, but if not set
 *  then will fallback to looking for the shared user ID
 */
fun PackageManager.getSystemUI(): String {
    return findKeyguard() ?: findSystemUI()
}

@Suppress("DEPRECATION")
fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageInfo(packageName, flags)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.getApplicationInfo(packageName: String): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
    } else {
        getApplicationInfo(packageName, 0)
    }
}

@Suppress("DEPRECATION")
@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getInstalledPackagesCompat(flags: Int = 0): List<PackageInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getInstalledPackages(flags)
    }
}