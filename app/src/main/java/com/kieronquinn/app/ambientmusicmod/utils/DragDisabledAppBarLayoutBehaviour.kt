package com.kieronquinn.app.ambientmusicmod.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class DragDisabledAppBarLayoutBehaviour: AppBarLayout.Behavior {

    constructor(): super()
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)

    init {
        setDragCallback(object: DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
    }

}