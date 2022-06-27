package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val TAP_DEBOUNCE = 250L

suspend fun View.awaitPost() = suspendCancellableCoroutine<View> {
    post {
        if(isAttachedToWindow){
            it.resume(this)
        }else{
            it.cancel()
        }
    }
}

fun View.onClicked() = callbackFlow {
    setOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

fun View.onFocused() = callbackFlow {
    setOnFocusChangeListener { _, isFocused ->
        if(isFocused) trySend(Unit)
    }
    awaitClose {
        onFocusChangeListener = null
    }
}

@SuppressLint("ClickableViewAccessibility")
fun View.onTouchUp() = callbackFlow {
    setOnTouchListener { _, motionEvent ->
        if(motionEvent.action == MotionEvent.ACTION_UP){
            trySend(Unit)
        }
        false
    }
    awaitClose {
        setOnTouchListener(null)
    }
}

fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

fun View.removeRipple() {
    setBackgroundResource(0)
}

fun View.delayPreDrawUntilFlow(flow: Flow<Boolean>, lifecycle: Lifecycle) {
    val listener = ViewTreeObserver.OnPreDrawListener {
        false
    }
    val removeListener = {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    lifecycle.runOnDestroy {
        removeListener()
    }
    viewTreeObserver.addOnPreDrawListener(listener)
    lifecycle.coroutineScope.launchWhenResumed {
        flow.collect {
            removeListener()
        }
    }
}

fun View.hideIme() {
    ViewCompat.getWindowInsetsController(this)?.hide(WindowInsetsCompat.Type.ime())
}


fun View.measureSize(windowManager: WindowManager): Pair<Int, Int> {
    val display = windowManager.defaultDisplay
    val height = display.height
    val width = display.width
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
    measure(widthSpec, heightSpec)
    return Pair(measuredWidth, measuredHeight)
}

fun View.getCenterXExactY(windowManager: WindowManager, yPos: Int): Pair<Int, Int> {
    val size = measureSize(windowManager)
    val centerX = windowManager.defaultDisplay.width / 2f
    val centerY = windowManager.defaultDisplay.height / 2f
    val left = centerX + (size.first / 2f)
    val top = centerY + (size.second / 2f)
    return Pair(left.toInt(), top.toInt())
}

@SuppressLint("ClickableViewAccessibility")
fun View.blockTouches() {
    setOnTouchListener { view, motionEvent ->
        true
    }
}

@SuppressLint("ClickableViewAccessibility")
fun View.unblockTouches() {
    setOnTouchListener(null)
}

val View.screenLocation
    get(): IntArray {
        val point = IntArray(2)
        getLocationOnScreen(point)
        return point
    }

val View.boundingBox
    get(): Rect {
        val (x, y) = screenLocation
        return Rect(x, y, x + width, y + height)
    }