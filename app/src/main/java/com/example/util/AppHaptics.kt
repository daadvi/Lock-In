package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import com.example.LockInApplication

/** One preference for feedback in Compose controls and floating views. */
object AppHaptics {
    fun isEnabled(context: Context): Boolean =
        (context.applicationContext as? LockInApplication)?.userPreferences?.hapticFeedback?.value != false &&
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    fun tap(context: Context) {
        if (!isEnabled(context)) return
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: SecurityException) {
            // Feedback is optional; a device restriction must not interrupt the action.
        }
    }
}
