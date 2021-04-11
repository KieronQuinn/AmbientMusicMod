package com.kieronquinn.app.ambientmusicmod.utils.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.isScrollable(): Boolean {
    val layoutManager = layoutManager as LinearLayoutManager
    return adapter?.let {
        layoutManager.findLastCompletelyVisibleItemPosition() < it.itemCount - 1 && layoutManager.findFirstCompletelyVisibleItemPosition() == 0
    } ?: run {
        false
    }
}