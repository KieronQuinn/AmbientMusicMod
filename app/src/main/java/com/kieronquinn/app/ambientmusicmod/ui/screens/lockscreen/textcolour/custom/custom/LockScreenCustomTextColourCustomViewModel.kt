package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom

import android.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class LockScreenCustomTextColourCustomViewModel: ViewModel() {

    abstract val colour: StateFlow<CustomColour>

    abstract fun setColourFromWheel(colour: Int)
    abstract fun setColourFromInput(input: String)
    abstract fun onApplyClicked()

    data class CustomColour(val colour: Int, val source: ColourSource) {
        enum class ColourSource {
            SETTINGS, WHEEL, INPUT
        }
    }

}

class LockScreenCustomTextColourCustomViewModelImpl(
    settings: SettingsRepository,
    private val navigation: ContainerNavigation
): LockScreenCustomTextColourCustomViewModel() {

    private val customColour = settings.lockscreenOverlayCustomColour
    private val colourSetting = settings.lockscreenOverlayColour

    private fun Int.normalize(): Int {
        return if(this == Int.MAX_VALUE) Color.WHITE
        else this
    }

    override val colour = MutableStateFlow(
        CustomColour(customColour.getSync().normalize(), CustomColour.ColourSource.SETTINGS)
    )

    override fun setColourFromWheel(colour: Int) {
        viewModelScope.launch {
            this@LockScreenCustomTextColourCustomViewModelImpl.colour.emit(
                CustomColour(colour, CustomColour.ColourSource.WHEEL)
            )
        }
    }

    override fun setColourFromInput(input: String) {
        viewModelScope.launch {
            val colour = input.toColourIntOptional() ?: return@launch
            this@LockScreenCustomTextColourCustomViewModelImpl.colour.emit(
                CustomColour(colour, CustomColour.ColourSource.INPUT)
            )
        }
    }

    override fun onApplyClicked() {
        viewModelScope.launch {
            customColour.set(colour.value.colour)
            colourSetting.set(SettingsRepository.OverlayTextColour.CUSTOM)
            navigation.navigateBack()
        }
    }

    private fun setupSetting() = viewModelScope.launch {
        viewModelScope.launch {
            customColour.asFlow().collect {
                colour.emit(CustomColour(it, CustomColour.ColourSource.SETTINGS))
            }
        }
    }

    init {
        setupSetting()
    }

    private fun String.toColourIntOptional(): Int? {
        return try {
            "#$this".toColorInt()
        }catch (e: IllegalArgumentException){
            null
        }
    }

}