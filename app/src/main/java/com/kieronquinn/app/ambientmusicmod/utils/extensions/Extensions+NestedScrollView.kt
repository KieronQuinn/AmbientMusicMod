package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.core.widget.NestedScrollView


/**
 * Used to scroll to the given view.
 *
 * @param scrollViewParent Parent ScrollView
 * @param view View to which we need to scroll.
 */
fun NestedScrollView.scrollToView(view: View) {
    // Get deepChild Offset
    val childOffset = Point()
    getDeepChildOffset(this, view.parent, view, childOffset)
    // Scroll to child.
    smoothScrollTo(0, childOffset.y)
}

/**
 * Used to get deep child offset.
 *
 *
 * 1. We need to scroll to child in scrollview, but the child may not the direct child to scrollview.
 * 2. So to get correct child position to scroll, we need to iterate through all of its parent views till the main parent.
 *
 * @param mainParent        Main Top parent.
 * @param parent            Parent.
 * @param child             Child.
 * @param accumulatedOffset Accumulated Offset.
 */
private fun getDeepChildOffset(
    mainParent: ViewGroup,
    parent: ViewParent,
    child: View,
    accumulatedOffset: Point
) {
    val parentGroup = parent as ViewGroup
    accumulatedOffset.x += child.left
    accumulatedOffset.y += child.top
    if (parentGroup == mainParent) {
        return
    }
    getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
}