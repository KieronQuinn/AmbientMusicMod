package com.kieronquinn.app.ambientmusicmod.utils.extensions

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout

fun AppBarLayout.setScrollEnabled(enabled: Boolean) {
    return
    if(enabled){
        enableScroll()
    }else{
        disableScroll()
    }
}

fun CollapsingToolbarLayout.setScrollEnabled(enabled: Boolean) {
    return
    if(enabled){
        enableScroll()
    }else{
        disableScroll()
    }
}

private fun CollapsingToolbarLayout.disableScroll(){
    val toolbarLayoutParams = layoutParams as AppBarLayout.LayoutParams
    toolbarLayoutParams.scrollFlags = 0
    layoutParams = toolbarLayoutParams
}

private fun CollapsingToolbarLayout.enableScroll(){
    val toolbarLayoutParams = layoutParams as AppBarLayout.LayoutParams
    toolbarLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
    layoutParams = toolbarLayoutParams
}

private fun AppBarLayout.disableScroll(){
    val appbarLayoutParams = layoutParams as CoordinatorLayout.LayoutParams
    appbarLayoutParams.behavior = null
    layoutParams = appbarLayoutParams
}

private fun AppBarLayout.enableScroll(){
    val appbarLayoutParams = layoutParams as CoordinatorLayout.LayoutParams
    appbarLayoutParams.behavior = AppBarLayout.Behavior()
    layoutParams = appbarLayoutParams
}