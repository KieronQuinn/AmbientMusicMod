package com.kieronquinn.app.ambientmusicmod.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenCreated
import com.kieronquinn.app.ambientmusicmod.work.UpdateWorker
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import org.koin.android.ext.android.inject

class RecognitionModalActivity: MonetCompatActivity() {

    private val settingsRepository by inject<SettingsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!settingsRepository.hasSeenSetup.getSync()){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        AmbientMusicModForegroundService.start(this, true)
        UpdateWorker.queueWorker(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        whenCreated {
            monet.awaitMonetReady()
            setContentView(R.layout.activity_recognition_modal)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(null)
        }
    }

}