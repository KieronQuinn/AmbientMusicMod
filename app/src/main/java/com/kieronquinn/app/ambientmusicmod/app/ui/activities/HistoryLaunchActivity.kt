package com.kieronquinn.app.ambientmusicmod.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices.Companion.nowPlayingHistoryIntent

class HistoryLaunchActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(nowPlayingHistoryIntent)
        finish()
    }

}