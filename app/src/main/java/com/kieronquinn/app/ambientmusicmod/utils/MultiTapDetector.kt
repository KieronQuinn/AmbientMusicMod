package com.kieronquinn.app.ambientmusicmod.utils

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/*
 * Detects an arbitrary number of taps in rapid succession
 *
 * The passed callback will be called for each tap, with two parameters:
 *  - the number of taps detected in rapid succession so far
 *  - a boolean flag indicating whether this is last tap of the sequence
 *
 * https://stackoverflow.com/a/51489085
 */
class MultiTapDetector(view: View, callback: (Int, Boolean) -> Unit) {
    private var numberOfTaps = 0
    private val handler = Handler()

    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()
    private val tapTimeout = ViewConfiguration.getTapTimeout().toLong()
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

    private val viewConfig = ViewConfiguration.get(view.context)

    private var downEvent = Event()
    private var lastTapUpEvent = Event()

    data class Event(var time: Long = 0, var x: Float = 0f, var y: Float = 0f) {
        fun copyFrom(motionEvent: MotionEvent) {
            time = motionEvent.eventTime
            x = motionEvent.x
            y = motionEvent.y
        }

        fun clear() {
            time = 0
        }
    }


    init {
         view.setOnTouchListener { v, event ->
             when(event.action) {
                 MotionEvent.ACTION_DOWN -> {
                     if(event.pointerCount == 1) {
                         downEvent.copyFrom(event)
                     } else {
                         downEvent.clear()
                     }
                 }
                 MotionEvent.ACTION_MOVE -> {
                     // If a move greater than the allowed slop happens before timeout, then this is a scroll and not a tap
                     if(event.eventTime - event.downTime < tapTimeout
                             && abs(event.x - downEvent.x) > viewConfig.scaledTouchSlop
                             && abs(event.y - downEvent.y) > viewConfig.scaledTouchSlop) {
                         downEvent.clear()
                     }
                 }
                 MotionEvent.ACTION_UP -> {
                     val downEvent = this.downEvent
                     val lastTapUpEvent = this.lastTapUpEvent

                     if(downEvent.time > 0 && event.eventTime - event.downTime < longPressTimeout) {
                         // We have a tap
                         if(lastTapUpEvent.time > 0
                                 && event.eventTime - lastTapUpEvent.time < doubleTapTimeout
                                 && abs(event.x - lastTapUpEvent.x) < viewConfig.scaledDoubleTapSlop
                                 && abs(event.y - lastTapUpEvent.y) < viewConfig.scaledDoubleTapSlop) {
                             // Double tap
                             numberOfTaps++
                         } else {
                             numberOfTaps = 1
                         }
                         this.lastTapUpEvent.copyFrom(event)

                         // Send event
                         val taps = numberOfTaps
                         handler.postDelayed({
                             // When this callback runs, we know if it is the final tap of a sequence
                             // if the number of taps has not changed
                             callback(taps, taps == numberOfTaps)
                         }, doubleTapTimeout)
                     }
                 }
             }
             true
         }
     }
}