package com.sualtikasifi.cizimhafiza.util

import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Cold-start stopwatch: logs how long after the process was created each milestone of the launch was reached
 * (`adb logcat -s StartupTiming`). It costs a log line per milestone and exists so a slow start can be pinned to a
 * specific stretch instead of guessed at.
 */
object StartupTrace {
    private const val TAG = "StartupTiming"

    fun mark(label: String) {
        val sinceProcessStart = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
        Log.i(TAG, "$label +${sinceProcessStart}ms")
    }
}
