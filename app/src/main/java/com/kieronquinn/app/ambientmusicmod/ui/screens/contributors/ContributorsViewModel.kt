package com.kieronquinn.app.ambientmusicmod.ui.screens.contributors

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import kotlinx.coroutines.launch

abstract class ContributorsViewModel: ViewModel() {

    abstract fun onLinkClicked(url: String)

    sealed class ContributorsSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class LinkedSetting(
            val title: CharSequence,
            val subtitle: CharSequence,
            @DrawableRes
            val icon: Int,
            val onLinkClicked: (url: String) -> Unit
        ): ContributorsSettingsItem(ItemType.LINKED_SETTING)

        enum class ItemType: BaseSettingsItemType {
            LINKED_SETTING
        }

    }

}

class ContributorsViewModelImpl(
    private val navigation: ContainerNavigation
): ContributorsViewModel() {

    override fun onLinkClicked(url: String) {
        viewModelScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

}