package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.OverlayTextColour
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.LockScreenTextColourViewModel.LockScreenTextColourSettingsItem.Colour
import com.kieronquinn.app.ambientmusicmod.utils.extensions.toHexColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class LockScreenTextColourViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    sealed class State {
        object Loading: State()
        data class Loaded(val colours: List<Colour>): State()
    }

    sealed class LockScreenTextColourSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Colour(
            val colour: OverlayTextColour,
            val contentOverride: String?,
            val isSelected: Boolean,
            val onClicked: (colour: OverlayTextColour) -> Unit
        ): LockScreenTextColourSettingsItem(ItemType.COLOUR) {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }

        enum class ItemType: BaseSettingsItemType {
            COLOUR
        }
    }

}

class LockScreenTextColourViewModelImpl(
    settings: SettingsRepository,
    private val navigation: ContainerNavigation
): LockScreenTextColourViewModel() {

    private val textColour = settings.lockscreenOverlayColour
    private val customColour = settings.lockscreenOverlayCustomColour

    override val state = combine(
        textColour.asFlow(),
        customColour.asFlow()
    ) { colour, custom ->
        State.Loaded(OverlayTextColour.values().map {
            val textOverride = if(it == OverlayTextColour.CUSTOM){
                if(custom == Int.MAX_VALUE) null
                else custom.toHexColor(true)
            }else null
            Colour(it, textOverride,colour == it, ::onColourClicked)
        })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private fun onColourClicked(colour: OverlayTextColour) {
        viewModelScope.launch {
            if(colour == OverlayTextColour.CUSTOM){
                navigation.navigate(LockScreenTextColourFragmentDirections.actionLockScreenTextColourFragmentToLockScreenCustomTextColourFragment())
            }else{
                textColour.set(colour)
            }
        }
    }

}