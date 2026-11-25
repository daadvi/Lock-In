package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * DEBUG-ONLY (src/debug): blocks the main thread on demand so the ANR watchdog can be
 * exercised end to end from adb. Never shipped in release builds.
 */
class AnrWatchdogDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val seconds = intent.getIntExtra("seconds", 7).coerceIn(1, 30)
        Log.w("AnrWatchdogDebug", "Injecting a ${seconds}s main-thread stall")
        Handler(Looper.getMainLooper()).post {
            try {
                Thread.sleep(seconds * 1000L)
            } catch (_: InterruptedException) {
            }
            Log.w("AnrWatchdogDebug", "Main-thread stall finished")
        }
    }
}
