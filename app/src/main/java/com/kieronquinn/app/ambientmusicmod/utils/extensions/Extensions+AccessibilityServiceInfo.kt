package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.accessibilityservice.AccessibilityServiceInfo

fun AccessibilityServiceInfo.setCapabilities(capabilities: Int){
    AccessibilityServiceInfo::class.java.getMethod("setCapabilities", Integer.TYPE)
        .invoke(this, capabilities)
}