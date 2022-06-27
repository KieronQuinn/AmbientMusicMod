package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetViewModel.LockScreenCustomTextColourMonetSettingsItem.Colours
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.toArgb
import dev.kdrag0n.monet.theme.ColorScheme
import dev.kdrag0n.monet.theme.ColorSwatch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class LockScreenCustomTextColourMonetViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onColourClicked(colour: Int)

    data class Colour(val colour: Int, val isSelected: Boolean)

    sealed class LockScreenCustomTextColourMonetSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Colours(
            @StringRes val label: Int,
            val colours: List<Colour>
        ): LockScreenCustomTextColourMonetSettingsItem(ItemType.COLOURS) {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }

        enum class ItemType: BaseSettingsItemType {
            COLOURS
        }
    }

    sealed class State {
        object Loading: State()
        data class Loaded(val colours: List<Colours>): State()
    }

}

class LockScreenCustomTextColourMonetViewModelImpl(
    settings: SettingsRepository,
    private val navigation: ContainerNavigation
): LockScreenCustomTextColourMonetViewModel(){

    private val monetColours by lazy {
        MonetCompat.getInstance().getMonetColors().getColorList()
    }

    private val customColour = settings.lockscreenOverlayCustomColour
    private val colourSetting = settings.lockscreenOverlayColour

    override val state = customColour.asFlow().map {
        val selectedColour = if(it == Int.MAX_VALUE) Color.WHITE else it
        val colours = monetColours.map { swatch ->
            swatch.second.map { color ->
                val value = color.value.toArgb()
                Colour(value, value == selectedColour)
            }.let { colours ->
                Colours(swatch.first, colours)
            }
        }
        State.Loaded(colours)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onColourClicked(colour: Int) {
        viewModelScope.launch {
            customColour.set(colour)
            colourSetting.set(SettingsRepository.OverlayTextColour.CUSTOM)
            navigation.navigateBack()
        }
    }

    private fun ColorScheme.getColorList(): List<Pair<Int, ColorSwatch>> {
        return listOf(
            Pair(R.string.lockscreen_overlay_text_colour_palette_accent1, accent1),
            Pair(R.string.lockscreen_overlay_text_colour_palette_accent2, accent2),
            Pair(R.string.lockscreen_overlay_text_colour_palette_accent3, accent3),
            Pair(R.string.lockscreen_overlay_text_colour_palette_neutral1, neutral1),
            Pair(R.string.lockscreen_overlay_text_colour_palette_neutral2, neutral2)
        )
    }

}