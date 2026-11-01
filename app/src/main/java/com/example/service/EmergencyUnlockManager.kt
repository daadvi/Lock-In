package com.example.service

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object EmergencyUnlockManager {
    private const val PREFS_NAME = "lock_in_emergency_unlock_prefs"
    private const val KEY_LAST_MONTH = "emergency_last_month"
    private const val KEY_MONTH_USAGE_COUNT = "emergency_month_usage_count"
    private const val KEY_UNLOCK_TIMESTAMPS = "emergency_unlock_timestamps"
    const val MAX_MONTHLY_UNLOCKS = 2
    const val CONFIRMATION_PHRASE = "Estou seguro que quero desbloquear meu dispositivo"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the standardized current year-month key (e.g., "2026-10")
     * using Calendar to avoid any locale/timezone date format discrepancies.
     */
    private fun getCurrentMonthKey(): String {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    /**
     * Returns the start epoch millis of the 1st day of the current month at 00:00:00.000.
     */
    private fun getStartOfCurrentMonthEpoch(): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Checks and guarantees that monthly usage count is always reset to 0 whenever
     * a new calendar month begins, using both calendar month keys and epoch timestamps.
     */
    @Synchronized
    fun checkAndResetMonthlyUsage(context: Context) {
        val prefs = getPrefs(context)
        val currentMonthKey = getCurrentMonthKey()
        val savedMonthKey = prefs.getString(KEY_LAST_MONTH, null)
        val monthStartEpoch = getStartOfCurrentMonthEpoch()

        // 1. Primary check: Has the calendar month key changed?
        val monthChanged = (savedMonthKey == null || savedMonthKey != currentMonthKey)

        // 2. Secondary check: Filter timestamp list to only keep unlocks in current month
        val rawTimestamps = prefs.getString(KEY_UNLOCK_TIMESTAMPS, "") ?: ""
        val currentMonthTimestamps = rawTimestamps.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it >= monthStartEpoch }

        if (monthChanged || currentMonthTimestamps.size != prefs.getInt(KEY_MONTH_USAGE_COUNT, 0)) {
            val validCount = if (monthChanged) currentMonthTimestamps.size else currentMonthTimestamps.size
            prefs.edit()
                .putString(KEY_LAST_MONTH, currentMonthKey)
                .putInt(KEY_MONTH_USAGE_COUNT, validCount)
                .putString(KEY_UNLOCK_TIMESTAMPS, currentMonthTimestamps.joinToString(","))
                .apply()
        }
    }

    @Synchronized
    fun getUsedCountThisMonth(context: Context): Int {
        checkAndResetMonthlyUsage(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_MONTH_USAGE_COUNT, 0)
        return count.coerceIn(0, MAX_MONTHLY_UNLOCKS)
    }

    @Synchronized
    fun getRemainingUnlocksThisMonth(context: Context): Int {
        val used = getUsedCountThisMonth(context)
        return (MAX_MONTHLY_UNLOCKS - used).coerceAtLeast(0)
    }

    @Synchronized
    fun canUseEmergencyUnlock(context: Context): Boolean {
        return getRemainingUnlocksThisMonth(context) > 0
    }

    /**
     * Executes the emergency unlock:
     * 1. Records timestamp and increments current month usage count (max 2 per month)
     * 2. Resets the streak / sequência to 0 immediately
     * 3. Stops focus lock session and resets screen time lock
     */
    @Synchronized
    fun recordUnlockUsed(context: Context) {
        checkAndResetMonthlyUsage(context)
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_MONTH_USAGE_COUNT, 0)
        if (currentCount >= MAX_MONTHLY_UNLOCKS) {
            return
        }
        val now = System.currentTimeMillis()

        val rawTimestamps = prefs.getString(KEY_UNLOCK_TIMESTAMPS, "") ?: ""
        val updatedTimestamps = if (rawTimestamps.isBlank()) {
            now.toString()
        } else {
            "$rawTimestamps,$now"
        }

        prefs.edit()
            .putInt(KEY_MONTH_USAGE_COUNT, currentCount + 1)
            .putString(KEY_UNLOCK_TIMESTAMPS, updatedTimestamps)
            .putString(KEY_LAST_MONTH, getCurrentMonthKey())
            .apply()

        // 1. Streak / sequência perdida e resetada para 0!
        StreakManager.markViolation(context)

        // 2. Stop focus lock session
        FocusTimeManager.stopFocusSession(context)

        // 3. Reset screen time lockout if active
        ScreenTimeManager.resetScreenTimeNow(context)
    }
}
