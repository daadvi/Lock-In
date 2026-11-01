package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Listens for device wake up, screen on, and unlock events.
 * When the phone is turned on while Focus Time (Lock In mode) is active,
 * it immediately brings FocusLockActivity to the front.
 * It does NOT hold the screen awake, allowing the phone to turn off normally
 * when locked or timed out to save battery.
 */
class ScreenLockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("ScreenLockReceiver", "Action received: $action")
        if (action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) {
            DailyResetManager.checkResetIfDue(context)
            if (FocusTimeManager.isFocusTimeActive(context)) {
                Log.d("ScreenLockReceiver", "Focus Time is active, bringing lock screen immediately to front")
                FocusTimeManager.bringFocusLockToFront(context)
            } else if (ScreenTimeManager.isLockoutActive(context)) {
                Log.d("ScreenLockReceiver", "Screen time lockout active, bringing lock screen immediately to front")
                ScreenTimeManager.bringLockoutToFront(context)
            }
        }
    }
}
