package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.text.InputFilter
import androidx.core.graphics.ColorUtils

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}

fun Int.isColorDark(): Boolean {
    return ColorUtils.calculateLuminance(this) < 0.5
}

private val HEX_REGEX = "[^A-F0-9]".toRegex()

fun hexColorFilter() = InputFilter { source, _, _, dest, _, _ ->
    source.toString()
        .uppercase()
        .replace(HEX_REGEX, "")
}

fun Int.toHexColor(includeHash: Boolean = true): String {
    val format = if(includeHash) "#%06X" else "%06X"
    return String.format("#%06X", (0xFFFFFF and this))
}