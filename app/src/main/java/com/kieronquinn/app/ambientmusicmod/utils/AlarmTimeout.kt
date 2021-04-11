/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.kieronquinn.app.ambientmusicmod.utils

import android.app.AlarmManager
import android.app.AlarmManager.OnAlarmListener
import android.os.Handler
import android.os.SystemClock

/**
 * Schedules a timeout through AlarmManager. Ensures that the timeout is called even when
 * the device is asleep.
 */
class AlarmTimeout(private val mAlarmManager: AlarmManager, private val mListener: OnAlarmListener,
                   private val mTag: String, private val mHandler: Handler) : OnAlarmListener {
    var isScheduled = false
        private set

    /**
     * Schedules an alarm in `timeout` milliseconds in the future.
     *
     * @param timeout How long to wait from now.
     * @param mode [.MODE_CRASH_IF_SCHEDULED], [.MODE_IGNORE_IF_SCHEDULED] or
     * [.MODE_RESCHEDULE_IF_SCHEDULED].
     * @return `true` when scheduled successfully, `false` otherwise.
     */
    fun schedule(timeout: Long, mode: Int): Boolean {
        when (mode) {
            MODE_CRASH_IF_SCHEDULED -> check(!isScheduled) { "$mTag timeout is already scheduled" }
            MODE_IGNORE_IF_SCHEDULED -> if (isScheduled) {
                return false
            }
            MODE_RESCHEDULE_IF_SCHEDULED -> if (isScheduled) {
                cancel()
            }
            else -> throw IllegalArgumentException("Illegal mode: $mode")
        }
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timeout, mTag, this, mHandler)
        isScheduled = true
        return true
    }

    fun cancel() {
        if (isScheduled) {
            mAlarmManager.cancel(this)
            isScheduled = false
        }
    }

    override fun onAlarm() {
        if (!isScheduled) {
            // We canceled the alarm, but it still fired. Ignore.
            return
        }
        isScheduled = false
        mListener.onAlarm()
    }

    companion object {
        const val MODE_CRASH_IF_SCHEDULED = 0
        const val MODE_IGNORE_IF_SCHEDULED = 1
        const val MODE_RESCHEDULE_IF_SCHEDULED = 2
    }

}