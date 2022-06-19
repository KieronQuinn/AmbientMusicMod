package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.annotation.SuppressLint
import android.app.WallpaperColors
import android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Context.wallpaperSupportsDarkText() = callbackFlow {
    val wallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            trySend(wallpaperManager.supportsDarkText())
        }
    }
    trySend(wallpaperManager.supportsDarkText())
    registerReceiver(receiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))
    awaitClose {
        unregisterReceiver(receiver)
    }
}

@SuppressLint("InlinedApi")
private fun WallpaperManager.supportsDarkText(): Boolean {
    val colors = getWallpaperColors(WallpaperManager.FLAG_LOCK)
        ?: getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return false
    return (colors.getColorHintsCompat() and HINT_SUPPORTS_DARK_TEXT) != 0
}

/**
 *  [WallpaperColors.getColorHints] exists on older versions but is hidden, so requires reflection
 */
private fun WallpaperColors.getColorHintsCompat(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorHints
    } else {
        WallpaperColors::class.java.getMethod("getColorHints").invoke(this) as Int
    }
}