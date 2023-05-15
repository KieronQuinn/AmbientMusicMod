package com.kieronquinn.app.ambientmusicmod.model.settings

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem.GenericSettingsItemType

abstract class BaseSettingsItem(val itemType: BaseSettingsItemType) {

    open fun deepEquals(other: Any?) = equals(other)

}

interface BaseSettingsItemType {
    companion object {
        inline fun <reified E: Enum<E>> findIndex(index: Int): E? {
            return enumValues<E>().firstOrNull {
                val itemIndex = (it as? BaseSettingsItemType)?.itemIndex
                    ?: throw RuntimeException("${E::class.java.simpleName} is not a BaseSettingsItemType")
                index == itemIndex
            }
        }
    }

    fun firstIndex() = GenericSettingsItemType.values().size

    val itemIndex
        get() = run {
            (this as Enum<*>).ordinal + firstIndex()
        }
}

sealed class GenericSettingsItem(val type: GenericSettingsItemType): BaseSettingsItem(type) {

    data class Switch(
        val enabled: Boolean,
        val text: CharSequence,
        val onChanged: (checked: Boolean) -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SWITCH)

    data class Setting(
        val title: CharSequence,
        val subtitle: CharSequence,
        @DrawableRes
        val icon: Int,
        val enabled: Boolean = true,
        val onLongClick: (() -> Unit)? = null,
        val onClick: () -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SETTING)

    data class SwitchSetting(
        val checked: Boolean,
        val title: CharSequence,
        val subtitle: CharSequence,
        @DrawableRes
        val icon: Int,
        val enabled: Boolean = true,
        val onChanged: (checked: Boolean) -> Unit
    ): GenericSettingsItem(GenericSettingsItemType.SWITCH_SETTING)

    data class Header(
        val text: CharSequence
    ): GenericSettingsItem(GenericSettingsItemType.HEADER)

    data class Dropdown<T>(
        val title: CharSequence,
        val subtitle: CharSequence,
        val icon: Drawable?,
        val setting: T,
        val onSet: (T) -> Unit,
        val options: List<T>,
        val adapter: (T) -> Int
    ): GenericSettingsItem(GenericSettingsItemType.DROPDOWN)

    enum class GenericSettingsItemType: BaseSettingsItemType {
        HEADER, SWITCH, SETTING, SWITCH_SETTING, DROPDOWN;

        //Base ItemType starts at 0, then other types go from there
        override fun firstIndex() = 0
    }

}