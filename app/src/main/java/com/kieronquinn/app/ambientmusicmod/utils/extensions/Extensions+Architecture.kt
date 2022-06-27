package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.os.Build

/**
 *  Since we only install on armv7 and armv8, we can simply check if [Build.SUPPORTED_ABIS] contains
 *  armv8, and if not the device is armv7 only.
 */
val isArmv7 = !Build.SUPPORTED_ABIS.contains("arm64-v8a")