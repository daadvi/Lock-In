package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Exact alarm fired at the start time of a device-wide focus routine. Guarantees the
 * lock screen appears at the scheduled minute even when the phone sits idle on the
 * home screen (no accessibility events) or right after a reboot. Re-arms the next
 * occurrence after firing.
 */
class FocusRoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (FocusTimeManager.isFocusTimeActive(context)) {
                FocusTimeManager.bringFocusLockToFront(context)
            }
            FocusTimeManager.scheduleRoutineAlarms(context)
        } catch (e: Exception) {
            Log.w("FocusRoutineAlarm", "Failed to enforce routine start", e)
        }
    }
}
