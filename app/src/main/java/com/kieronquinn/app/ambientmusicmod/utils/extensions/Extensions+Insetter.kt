package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.view.View
import androidx.core.view.WindowInsetsCompat
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.OnApplyInsetsListener
import dev.chrisbanes.insetter.ViewState
import dev.chrisbanes.insetter.applyInsetter

fun View.applySystemWindowInsetsPadding(top: Boolean = false, bottom: Boolean = false, left: Boolean = false, right: Boolean = false){
    applyInsetter {
        type(ime = true, statusBars = true, navigationBars = true) {
            padding(top = top, bottom = bottom, left = left, right = right)
        }
        consume(false)
    }
}

fun View.applySystemWindowInsetsMargin(top: Boolean = false, bottom: Boolean = false, left: Boolean = false, right: Boolean = false){
    applyInsetter {
        type(ime = true, statusBars = true, navigationBars = true) {
            margin(top = top, bottom = bottom, left = left, right = right)
        }
        consume(false)
    }
}

fun View.onApplyWindowInsets(callback: (View, WindowInsetsCompat, ViewState) -> Unit){
    Insetter.builder().setOnApplyInsetsListener(object: OnApplyInsetsListener {
        override fun onApplyInsets(view: View, insets: WindowInsetsCompat, initialState: ViewState) {
            callback.invoke(view, insets, initialState)
        }
    }).applyToView(this)
}

fun WindowInsetsCompat.getStandardBottomInsets(): Int {
    return getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars()).bottom
}

fun WindowInsetsCompat.getStandardTopInsets(): Int {
    return getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars()).top
}