package com.kieronquinn.app.ambientmusicmod.app.ui.views

import android.animation.AnimatorInflater
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.kieronquinn.app.ambientmusicmod.R
import kotlin.math.abs

class ElevationAppBarLayout: AppBarLayout {

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defaultStyleRes: Int): super(context, attrs, defaultStyleRes)

    private enum class State {
        EXPANDED, COLLAPSED, IDLE
    }

    private var state = State.IDLE

    private val elevationDisabled by lazy {
        AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_elevation_disabled)
    }

    private val elevationEnabled by lazy {
        AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_elevation_enabled)
    }

    private val offsetListener = OnOffsetChangedListener { appBarLayout, verticalOffset ->
        when {
            verticalOffset == 0 -> {
                if (state != State.EXPANDED) {
                    onStateChanged(State.EXPANDED);
                }
                state = State.EXPANDED;
            }
            abs(verticalOffset) >= appBarLayout.totalScrollRange -> {
                if (state != State.COLLAPSED) {
                    onStateChanged(State.COLLAPSED);
                }
                state = State.COLLAPSED;
            }
            else -> {
                if (state != State.IDLE) {
                    onStateChanged(State.IDLE);
                }
                state = State.IDLE;
            }
        }
    }

    init {
        addOnOffsetChangedListener(offsetListener)
    }

    private fun onStateChanged(state: State){
        stateListAnimator = when(state){
            State.COLLAPSED -> {
                elevationEnabled
            }
            else -> {
                elevationDisabled
            }
        }
    }

}