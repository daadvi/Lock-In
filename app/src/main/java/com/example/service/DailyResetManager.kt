package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.LockInApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Robust Centralized Daily Reset Manager.
 * Ensures that app limits, shared budgets, website limits, and screen time lockout
 * are reliably reset automatically upon:
 * 1. Calendar day change (e.g. crossing midnight).
 * 2. User-configured interval elapsing (e.g. 24h reset cycle).
 * 3. Screen unlock / screen on / foreground app change when a new day arrives.
 * 4. Exact scheduled midnight alarm.
 */
object DailyResetManager {
    private const val TAG = "DailyResetManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val resetMutex = Mutex()

    @Volatile
    private var lastCheckedEpoch: Long = 0L

    /**
     * Synchronous check whether a daily/scheduled reset is currently overdue.
     */
    fun isResetDue(context: Context): Boolean {
        return try {
            val app = context.applicationContext as? LockInApplication ?: LockInApplication.instance
            val userPrefs = app.userPreferences
            val lastReset = userPrefs.getLastResetEpoch()
            if (lastReset <= 0L) {
                userPrefs.recordResetTimestamp(System.currentTimeMillis())
                return false
            }

            val now = System.currentTimeMillis()
            val intervalHours = userPrefs.resetIntervalHours.value
            val intervalMillis = intervalHours.coerceAtLeast(1) * 3600_000L

            // 1. Elapsed hours check
            if (now - lastReset >= intervalMillis) {
                return true
            }

            // 2. Calendar day check: if the calendar day has rolled over
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }
            val lastResetDay = userPrefs.getLastResetDayOfYear()
            val lastResetYear = userPrefs.getLastResetYear()

            if (lastResetDay != -1 && (nowCal.get(Calendar.DAY_OF_YEAR) != lastResetDay || nowCal.get(Calendar.YEAR) != lastResetYear)) {
                return true
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if reset is due", e)
            false
        }
    }

    /**
     * Triggers a reset check asynchronously if overdue.
     */
    fun checkResetIfDue(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastCheckedEpoch < 10_000L) {
            return
        }
        lastCheckedEpoch = now

        if (isResetDue(context)) {
            scope.launch {
                performDailyReset(context)
            }
        }
    }

    /**
     * Executes the daily reset across all subsystems.
     */
    suspend fun performDailyReset(context: Context) {
        resetMutex.withLock {
            try {
                val app = context.applicationContext as? LockInApplication ?: LockInApplication.instance
                val now = System.currentTimeMillis()
                Log.d(TAG, "Triggering automatic daily reset at $now")

                // 1. Reset Room database timers: apps, websites, app groups
                app.repository.resetAllAppTimers()
                app.repository.resetAllWebsiteTimers()
                app.repository.resetAllAppGroupTimers()

                // 2. Reset Shared limit in UserPreferences
                app.userPreferences.resetSharedTimer()

                // 3. Record reset timestamp and calendar day
                app.userPreferences.recordResetTimestamp(now)

                // 4. Reset ScreenTimeManager daily tracking
                ScreenTimeManager.resetDailyUsage(context)

                // 5. Update in-memory AppBlockEnforcer & LimitTimerManager caches
                AppBlockEnforcer.clearAllDepletedForToday()
                AppUnlockDailyTracker.resetForNewDay(context)
                val updatedApps = app.repository.getAllAppsList()
                AppBlockEnforcer.updateDepletedList(updatedApps)
                LimitTimerManager.onDailyReset()

                // 6. Update widgets
                com.example.widget.LockInWidgetManager.updateAllWidgets(context)

                // 7. Schedule next midnight alarm
                scheduleMidnightAlarm(context)

                Log.d(TAG, "Automatic daily reset completed successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Error performing daily reset", e)
            }
        }
    }

    /**
     * Schedules an AlarmManager alarm at the upcoming midnight to guarantee background wake-up.
     */
    fun scheduleMidnightAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, DailyResetAlarmReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 8882, intent, flags)

            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }

            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    alarmManager.canScheduleExactAlarms()
                } catch (_: Exception) {
                    false
                }
            } else {
                true
            }

            if (canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled next midnight reset for ${nextMidnight.time}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule midnight alarm: ${e.message}")
        }
    }
}

/**
 * Receiver for the midnight alarm.
 */
class DailyResetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DailyResetAlarmReceiver", "Midnight reset alarm fired!")
        DailyResetManager.checkResetIfDue(context)
    }
}
