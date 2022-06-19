package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

fun PackageManager.isPermissionGranted(packageName: String, vararg permission: String): Boolean {
    val packageInfo = try {
        getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
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
        getPackageInfo(packageName, 0)
        true
    }catch (e: PackageManager.NameNotFoundException){
        false
    }
}

fun PackageManager.getSplits(packageName: String): Array<String> {
    val applicationInfo = try {
        getApplicationInfo(packageName, 0)
    }catch (e: PackageManager.NameNotFoundException){
        return emptyArray()
    }
    return applicationInfo.splitNames ?: emptyArray()
}

/**
 *  Returns the SystemUI package, which is not always `com.android.systemui`. Finds it using the
 *  shared user ID `android.uid.systemui`, which does seem to always be used. If this breaks in the
 *  future, `com.android.systemui` will be returned.
 */
@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getSystemUI(): String {
    return getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY).firstOrNull {
        it.sharedUserId == "android.uid.systemui"
    }?.packageName ?: "com.android.systemui"
}