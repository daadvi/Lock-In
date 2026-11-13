package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.LockInApplication

/**
 * Ensures Lock In restrictions, streak verification, and floating island services
 * remain active and persistent across device reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED || action == "android.intent.action.QUICKBOOT_POWERON") {
            // 1. Advance daily streak if limits were obeyed without password
            StreakManager.init(context)

            // 1.1 Verify and guarantee monthly emergency unlock quota reset
            EmergencyUnlockManager.checkAndResetMonthlyUsage(context)

            // 2. Check if total lock / focus time is active. If so, immediately display lock screen!
            if (FocusTimeManager.isFocusTimeActive(context)) {
                FocusTimeManager.bringFocusLockToFront(context)
            }

            // 2.1 Retry shortly after boot: OEM restrictions can silently swallow the
            // first background activity launch, which would leave the lock screen gone.
            scheduleLockScreenRetries(context)

            // 2.2 Re-arm routine start alarms lost to the reboot
            FocusTimeManager.scheduleRoutineAlarms(context)

            // 3. Start Floating Island Service if overlay permission is granted
            if (Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, FloatingIslandService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (_: Exception) {
                    // Ignored on background execution limit if any
                }
            }

            // 4. Resume Network Shield (VPN / Firewall) if user enabled it and system permission was granted
            try {
                LockInVpnManager.initialize(context)
                if (LockInVpnManager.isVpnEnabled.value && !LockInVpnManager.isPermissionRequired(context)) {
                    LockInVpnManager.startVpn(context)
                }
            } catch (_: Exception) {}
        }
    }

    private fun scheduleLockScreenRetries(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = System.currentTimeMillis()
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try { alarmManager.canScheduleExactAlarms() } catch (_: Exception) { false }
            } else true
            listOf(20_000L, 120_000L).forEachIndexed { index, delayMs ->
                val pendingIntent = PendingIntent.getBroadcast(context, 9100 + index,
                    Intent(context, FocusLockRetryReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                if (canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + delayMs, pendingIntent)
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + delayMs, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + delayMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, now + delayMs, pendingIntent)
                }
            }
        } catch (_: Exception) {}
    }
}
