package com.kieronquinn.app.ambientmusicmod.app.ui.activities

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.ambientmusicmod.app.receivers.BootCompletedReceiver
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices.Companion.standardSettingsIntent

class BootHelperLaunchActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(standardSettingsIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(BootCompletedReceiver.NOTIFICATION_ID_HELPER)
        finish()
    }

}