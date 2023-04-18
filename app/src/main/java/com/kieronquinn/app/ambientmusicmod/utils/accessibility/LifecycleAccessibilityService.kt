package com.kieronquinn.app.ambientmusicmod.utils.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

/**
 *  Accessibility Service that implements Lifecycle Owner
 *  Based on androidx.lifecycle.LifecycleService, but does not call onServicePreSuperOnBind as
 *  onBind cannot be overridden
 */
abstract class LifecycleAccessibilityService: AccessibilityService(), LifecycleOwner {

    private val mDispatcher by lazy {
        ServiceLifecycleDispatcher(this@LifecycleAccessibilityService)
    }

    override val lifecycle by lazy {
        mDispatcher.lifecycle
    }

    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

}