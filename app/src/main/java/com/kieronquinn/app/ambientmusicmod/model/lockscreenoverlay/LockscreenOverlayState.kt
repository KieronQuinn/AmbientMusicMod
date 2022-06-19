package com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class LockscreenOverlayState(val overlayState: OverlayState): Parcelable

@Parcelize
sealed class OverlayState: Parcelable {

    @Parcelize
    object Hidden: OverlayState()

    @Parcelize
    data class Shown(
        val style: LockscreenOverlayStyle,
        val yPos: Int,
        @DrawableRes val icon: Int,
        val content: CharSequence,
        val contentAfterBullet: CharSequence?,
        val endTime: Long,
        val onClick: () -> Unit
    ): OverlayState()

    @Parcelize
    data class IconOnly(
        val style: LockscreenOverlayStyle,
        val yPos: Int,
        @DrawableRes val icon: Int,
        val onClick: () -> Unit
    ): OverlayState()

}

fun OverlayState.stateEquals(other: OverlayState): Boolean {
    if(this is OverlayState.Hidden && other is OverlayState.Hidden) return true
    if(this is OverlayState.IconOnly && other is OverlayState.IconOnly) {
        return icon == other.icon && yPos == other.yPos && onClick == other.onClick
    }
    if(this is OverlayState.Shown && other is OverlayState.Shown) {
        return icon == other.icon && content == other.content && style == other.style && yPos == other.yPos && onClick == other.onClick
    }
    return false
}