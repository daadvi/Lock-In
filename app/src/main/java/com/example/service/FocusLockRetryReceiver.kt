package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot-time retry for the focus lock screen. Background activity launches can be
 * silently swallowed right after boot/reboot (OEM restrictions), so the boot
 * receiver schedules these retries: if the focus session is still running, the
 * lock screen is brought back until it is really on screen.
 */
class FocusLockRetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (FocusTimeManager.isFocusTimeActive(context)) {
                FocusTimeManager.bringFocusLockToFront(context)
            }
        } catch (e: Exception) {
            // Boot retries are best-effort, but a silent failure here leaves the user unlocked.
            Log.w("FocusLockRetry", "Failed to restore the focus lock screen", e)
        }
    }
}
