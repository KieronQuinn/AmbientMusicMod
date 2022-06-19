package com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay

import androidx.annotation.DrawableRes
import com.kieronquinn.app.ambientmusicmod.R

enum class LockscreenOverlayStyle(@DrawableRes val icon: Int) {
    NEW(R.drawable.audioanim_animation), CLASSIC(R.drawable.audioanim_animation_classic)
}