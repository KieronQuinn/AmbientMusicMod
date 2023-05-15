package com.kieronquinn.app.ambientmusicmod.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.repositories.ExternalAccessRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExternalAccessReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        private const val ACTION_ENABLE = "${BuildConfig.APPLICATION_ID}.action.ENABLE"
        private const val ACTION_DISABLE = "${BuildConfig.APPLICATION_ID}.action.DISABLE"
        private const val ACTION_TOGGLE = "${BuildConfig.APPLICATION_ID}.action.TOGGLE"

        private const val ACTION_RUN_RECOGNITION =
            "${BuildConfig.APPLICATION_ID}.action.RUN_RECOGNITION"
        private const val ACTION_RUN_ONLINE_RECOGNITION =
            "${BuildConfig.APPLICATION_ID}.action.RUN_ONLINE_RECOGNITION"
    }

    private val externalAccess by inject<ExternalAccessRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            ACTION_ENABLE -> externalAccess.onEnable(intent)
            ACTION_DISABLE -> externalAccess.onDisable(intent)
            ACTION_TOGGLE -> externalAccess.onToggle(intent)
            ACTION_RUN_RECOGNITION -> externalAccess.onRecognise(intent, false)
            ACTION_RUN_ONLINE_RECOGNITION -> externalAccess.onRecognise(intent, true)
        }
    }

}