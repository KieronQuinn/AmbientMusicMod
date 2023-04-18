package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.service.LockscreenOverlayAccessibilityService
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivity
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getSettingAsFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.secureStringConverter
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenCreated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

interface AccessibilityRepository {

    val accessibilityStartBus: Flow<Unit>
    val enabled: Flow<Boolean>

    fun bringToFrontOnAccessibilityStart(fragment: Fragment)
    suspend fun onAccessibilityStarted()

}

class AccessibilityRepositoryImpl(context: Context): AccessibilityRepository {

    companion object {
        private val COMPONENT_ACCESSIBILITY_SERVICE = ComponentName(
            BuildConfig.APPLICATION_ID, LockscreenOverlayAccessibilityService::class.java.name
        )
    }

    override val accessibilityStartBus = MutableSharedFlow<Unit>()

    override val enabled = context.getSettingAsFlow(
        Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
        context.secureStringConverter(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    ).map { current ->
        if(current == null) return@map false
        val currentSet = when {
            current.contains(",") -> {
                current.split(",").toSet()
            }
            current.contains(":") -> {
                current.split(":").toSet()
            }
            else -> setOf(current)
        }
        currentSet.contains(COMPONENT_ACCESSIBILITY_SERVICE.flattenToString())
    }

    override fun bringToFrontOnAccessibilityStart(fragment: Fragment) {
        fragment.viewLifecycleOwner.whenCreated {
            this@AccessibilityRepositoryImpl.accessibilityStartBus.collect {
                fragment.bringToFront()
            }
        }
    }

    override suspend fun onAccessibilityStarted() {
        accessibilityStartBus.emit(Unit)
    }

    private fun Fragment.bringToFront() {
        startActivity(Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

}