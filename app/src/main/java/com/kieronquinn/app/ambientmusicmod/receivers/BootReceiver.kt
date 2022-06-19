package com.kieronquinn.app.ambientmusicmod.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService

class BootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AmbientMusicModForegroundService.start(context, true)
    }

}