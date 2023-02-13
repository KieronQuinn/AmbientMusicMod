package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.os.Build

/**
 *  Currently supported architectures:
 *  armv8 (from latest official ASI builds)
 *  armv7 (from old Pixel Ambient Services build)
 *  x86_64 (from Android 14 system image ASI build)
 */
enum class Architecture(val abi: String) {
    arm_v7("armeabi-v7a"),
    arm_v8("arm64-v8a"),
    x86_64("x86_64");

    companion object {
        internal fun forAbi(abi: String): Architecture? {
            return values().firstOrNull { it.abi == abi }
        }
    }
}

private fun getArchitecture(): Architecture? {
    return Build.SUPPORTED_ABIS.firstNotNullOfOrNull {
        Architecture.forAbi(it)
    }
}

/**
 *  Uses old libsense and separate leveldbjni
 */
val isArmv7 = getArchitecture() == Architecture.arm_v7

/**
 *  Uses emulator libsense with embedded leveldbjni
 */
val isX86_64 = getArchitecture() == Architecture.x86_64