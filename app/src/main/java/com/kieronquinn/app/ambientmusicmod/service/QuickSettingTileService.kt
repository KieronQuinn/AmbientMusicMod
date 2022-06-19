package com.kieronquinn.app.ambientmusicmod.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.utils.lifecycle.LifecycleTileService
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.ext.android.inject

class QuickSettingTileService: LifecycleTileService() {

    private val remoteSettings by inject<RemoteSettingsRepository>()
    private val settings by inject<SettingsRepository>()

    private val isEnabled = combine(
        remoteSettings.getRemoteSettings(), settings.hasSeenSetup.asFlow()
    ) { settings, hasSeenSetup ->
        if(settings !is SettingsState.Available) return@combine null
        Pair(settings.mainEnabled, hasSeenSetup)
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private var tileJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        tileJob = lifecycleScope.launchWhenCreated {
            isEnabled.collect {
                val isEnabled = when {
                    it == null -> null //Not got state yet
                    !it.second -> null //Not finished setup
                    else -> it.first //Settings got
                }
                updateTile(isEnabled)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        tileJob?.cancel()
    }

    private fun updateTile(isEnabled: Boolean?) = qsTile.apply {
        val iconRes = when(isEnabled){
            false -> R.drawable.ic_quick_setting_disabled
            else -> R.drawable.ic_quick_setting_enabled
        }
        icon = Icon.createWithResource(this@QuickSettingTileService, iconRes)
        label = resources.getString(R.string.quick_settings_tile_title)
        val subtitleRes = when(isEnabled){
            true -> R.string.quick_settings_tile_enabled
            false -> R.string.quick_settings_tile_disabled
            null -> null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subtitle = subtitleRes?.let { resources.getString(it) }
        }
        state = when(isEnabled){
            true -> Tile.STATE_ACTIVE
            false -> Tile.STATE_INACTIVE
            else -> Tile.STATE_UNAVAILABLE
        }
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        lifecycleScope.launchWhenCreated {
            val current = isEnabled.value?.first ?: return@launchWhenCreated
            remoteSettings.commitChanges(SettingsStateChange(mainEnabled = !current))
        }
    }

}