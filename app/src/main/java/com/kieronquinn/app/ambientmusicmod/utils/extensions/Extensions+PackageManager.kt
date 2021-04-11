package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager

fun PackageManager.isAppInstalled(packageName: String): Boolean {
    return try {
        getApplicationInfo(packageName, 0)
        true
    }catch (e: PackageManager.NameNotFoundException){
        false
    }
}

fun PackageManager.getAppVersion(packageName: String): String {
    return try {
        getPackageInfo(packageName, 0).run {
            "$versionName ($versionCodeCompat)"
        }
    }catch (e: PackageManager.NameNotFoundException){
        "Not installed"
    }
}

fun PackageManager.getAppVersionCode(packageName: String): Long? {
    return try {
        getPackageInfo(packageName, 0).versionCodeCompat
    }catch (e: PackageManager.NameNotFoundException){
        null
    }
}

private val PackageInfo.versionCodeCompat: Long
    get() {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            longVersionCode
        }else{
            versionCode.toLong()
        }
    }