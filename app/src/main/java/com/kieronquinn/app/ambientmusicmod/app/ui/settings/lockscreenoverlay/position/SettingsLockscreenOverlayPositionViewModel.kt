package com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.position

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class SettingsLockscreenOverlayPositionViewModel: ViewModel() {

    abstract val position: Flow<Pair<Float, Float>?>

    abstract fun setPosition(x: Float, y: Float)
    abstract fun centerHorizontally(centerX: Float)
    abstract fun resetPosition()
    abstract fun save()

}

class SettingsLockscreenOverlayPositionViewModelImpl(private val settings: AmbientSharedPreferences): SettingsLockscreenOverlayPositionViewModel() {

    private val _position = MutableStateFlow<Pair<Float, Float>?>(null).apply {
        viewModelScope.launch {
            val overlayPositionX = settings.overlayPositionX
            val overlayPositionY = settings.overlayPositionY
            if(overlayPositionX == -1f || overlayPositionY == -1f) return@launch
            emit(Pair(overlayPositionX, overlayPositionY))
        }
    }

    override val position: Flow<Pair<Float, Float>?> = _position.asStateFlow()

    override fun setPosition(x: Float, y: Float) {
        viewModelScope.launch {
            _position.emit(Pair(x, y))
        }
    }

    override fun centerHorizontally(centerX: Float) {
        val currentY = _position.value?.second ?: return
        viewModelScope.launch {
            _position.emit(Pair(centerX, currentY))
        }
    }

    override fun resetPosition() {
        viewModelScope.launch {
            _position.emit(null)
        }
    }

    override fun save() {
        val position = _position.value
        if(position != null){
            settings.overlayPositionX = position.first
            settings.overlayPositionY = position.second
        }else{
            settings.overlayPositionX = AmbientSharedPreferences.DEFAULT_OVERLAY_POSITION_X
            settings.overlayPositionY = AmbientSharedPreferences.DEFAULT_OVERLAY_POSITION_Y
        }
    }

}