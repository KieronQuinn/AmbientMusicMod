package com.kieronquinn.app.ambientmusicmod.ui.base

interface BaseContainerViewModel {
    fun onBackPressed()
    fun onParentBackPressed(): Boolean
}