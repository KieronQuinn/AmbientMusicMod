package com.kieronquinn.app.ambientmusicmod.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.ActivityNavigator
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.position.SettingsLockscreenOverlayPositionFragment
import dev.chrisbanes.insetter.Insetter

class SettingsLockscreenOverlayPositionActivity: AppCompatActivity() {

    private val windowInsetsController by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }

    private val fragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragment_lockscreen_overlay_position_fragment) as SettingsLockscreenOverlayPositionFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Insetter.setEdgeToEdgeSystemUiFlags(window.decorView, true)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setContentView(R.layout.activity_settings_lockscreen_overlay_position)
    }

    override fun onBackPressed() {
        fragment.onBackPressed()
        super.onBackPressed()
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }

}