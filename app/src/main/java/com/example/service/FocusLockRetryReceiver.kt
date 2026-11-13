package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
        } catch (_: Exception) {
        }
    }
}
