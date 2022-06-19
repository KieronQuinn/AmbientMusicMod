package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.text.TextPaint
import android.text.TextUtils

fun CharSequence.ellipsizeToSize(textPaint: TextPaint, width: Float): CharSequence {
    return TextUtils.ellipsize(this, textPaint, width, TextUtils.TruncateAt.END)
}