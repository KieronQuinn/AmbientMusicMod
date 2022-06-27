package com.kieronquinn.app.ambientmusicmod.utils.extensions

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import top.defaults.colorpicker.ColorObserver
import top.defaults.colorpicker.ColorPickerView
import top.defaults.colorpicker.ColorWheelView

fun ColorPickerView.onChanged() = callbackFlow {
    val observer = ColorObserver { color, fromUser, _ ->
        if(fromUser) trySend(color)
    }
    subscribe(observer)
    awaitClose {
        unsubscribe(observer)
    }
}

fun ColorPickerView.setColor(color: Int) {
    val colorWheelView = ColorPickerView::class.java.getDeclaredField("colorWheelView").apply {
        isAccessible = true
    }.get(this) as ColorWheelView
    colorWheelView.setColor(color, false)
}