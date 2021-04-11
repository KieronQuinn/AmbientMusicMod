package com.kieronquinn.app.ambientmusicmod.app.service

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.LifecycleAccessibilityService
import com.kieronquinn.app.ambientmusicmod.utils.TimeLimitedStateFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.BroadcastReceiver
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SecureBroadcastReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@SuppressLint("InflateParams")
class LockscreenOverlayAccessibilityService : LifecycleAccessibilityService() {

    companion object {
        @Suppress("DEPRECATION")
        private const val overlayParamFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        private const val ACTION_SHOW_INDICATION = "com.google.android.ambientindication.action.AMBIENT_INDICATION_SHOW"
        private const val ACTION_HIDE_INDICATION = "com.google.android.ambientindication.action.AMBIENT_INDICATION_HIDE"

        //Timeout showing the track if we don't get a clear or new track in 3 minutes
        private const val TRACK_TIMEOUT = 180000L
    }

    private val settings by inject<AmbientSharedPreferences>()

    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val layoutInflater by lazy {
        getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private val isLockScreenShowing = MutableSharedFlow<Boolean>().apply {
        lifecycleScope.launch {
            emit(false)
        }
    }

    private val currentTrack = TimeLimitedStateFlow<CurrentTrack>(lifecycleScope, TRACK_TIMEOUT)

    private val shownTrack = combine(isLockScreenShowing, currentTrack.asStateFlow()) { lockScreen: Boolean, currentTrack: CurrentTrack? ->
        if (lockScreen && !currentTrack?.text.isNullOrBlank()) currentTrack else null
    }

    private val screenOnReceiver = BroadcastReceiver { _, _ ->
        setLockscreenShowing(true)
    }

    private val unlockReceiver = BroadcastReceiver { _, _ ->
        setLockscreenShowing(false)
    }

    private val indicationReceiver = SecureBroadcastReceiver { _, intent ->
        lifecycleScope.launch {
            val text = intent?.getStringExtra("com.google.android.ambientindication.extra.TEXT") ?: return@launch
            val launchIntent = intent.getParcelableExtra<PendingIntent>("com.google.android.ambientindication.extra.OPEN_INTENT") ?: return@launch
            currentTrack.emit(CurrentTrack(text, launchIntent))
        }
    }

    private val clearReceiver = SecureBroadcastReceiver { _, _ ->
        lifecycleScope.launch {
            currentTrack.clear()
        }
    }

    private val overlayPositionX: Float?
        get() {
            val savedPosition = settings.overlayPositionX
            return if (savedPosition == -1f) null else savedPosition
        }

    private val overlayPositionY: Float?
        get() {
            val savedPosition = settings.overlayPositionY
            return if (savedPosition == -1f) null else savedPosition
        }

    private val layoutParams: WindowManager.LayoutParams
        get() {
            return WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    overlayParamFlags,
                    PixelFormat.TRANSLUCENT).apply {
                val positionX = overlayPositionX
                val positionY = overlayPositionY
                if (positionX != null && positionY != null) {
                    x = positionX.toInt()
                    y = positionY.toInt()
                    gravity = Gravity.TOP or Gravity.LEFT
                } else {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
            }
        }

    private val overlayView by lazy {
        layoutInflater.inflate(R.layout.overlay_lockscreen, null, false)
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        registerReceiver(indicationReceiver, IntentFilter(ACTION_SHOW_INDICATION))
        registerReceiver(clearReceiver, IntentFilter(ACTION_HIDE_INDICATION))
        lifecycleScope.launch {
            shownTrack.collect { track ->
                if (track != null) {
                    showOverlay(track)
                } else hideOverlay()
            }
        }
    }

    private fun showOverlay(track: CurrentTrack) {
        if (settings.lockScreenOverlayEnabled && !overlayView.isAttachedToWindow && keyguardManager.isDeviceLocked) {
            windowManager.addView(overlayView.withTrack(track), layoutParams)
            startAnimation()
        }
    }

    private fun hideOverlay() {
        if (overlayView.isAttachedToWindow) {
            windowManager.removeView(overlayView)
        }
    }

    private fun startAnimation() {
        overlayView.findViewById<ImageView>(R.id.overlay_lockscreen_icon).run {
            (drawable as? AnimatedVectorDrawable)?.start()
        }
    }

    private fun View.withTrack(track: CurrentTrack): View {
        findViewById<TextView>(R.id.overlay_lockscreen_text).text = track.text
        setOnClickListener {
            if (settings.lockScreenOverlayLaunchClick) {
                hideOverlay()
                track.clickIntent.send()
            }
        }
        return this
    }

    private fun setLockscreenShowing(shouldShow: Boolean) = lifecycleScope.launch {
        isLockScreenShowing.emit(shouldShow)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //Not subscribed to any events
    }

    override fun onInterrupt() {
        //Unused
    }

    data class CurrentTrack(val text: String, val clickIntent: PendingIntent)

}