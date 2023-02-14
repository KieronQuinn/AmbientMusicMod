package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import com.kieronquinn.app.ambientmusicmod.R

fun View.onApplyInsets(block: (view: View, insets: WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets)
        insets
    }
}

fun View.applyBottomNavigationInset(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
    updatePadding(bottom = bottomNavHeight + extraPadding.toInt())
    val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        context.getLegacyWorkaroundNavBarHeight()
    } else 0
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.ime()).bottom + legacyWorkaround
        updatePadding(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
    }
}

fun View.applyBottomNavigationMargin(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height_margins).toInt()
    val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        context.getLegacyWorkaroundNavBarHeight()
    } else 0
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom +
                legacyWorkaround
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
        }
    }
}

fun View.applyBottomNavigationMarginShort(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
    val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        context.getLegacyWorkaroundNavBarHeight()
    } else 0
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom +
                legacyWorkaround
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
        }
    }
}

fun Context.getLegacyWorkaroundNavBarHeight(): Int {
    val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}