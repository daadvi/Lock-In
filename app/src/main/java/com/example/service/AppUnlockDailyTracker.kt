package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AppUnlockDailyTracker
 *
 * Enforces the strict rule that an app can only be unlocked 1 time per day.
 * Persists the set of unlocked package names for the current date.
 *
 * ANTI-TAMPER TIME PROTECTION:
 * Anchors day verification to both the calendar day and the hardware monotonic clock
 * (SystemClock.elapsedRealtime()). Detects clock rollbacks and clock advancement bypasses.
 */
object AppUnlockDailyTracker {

    private const val TAG = "AppUnlockDailyTracker"
    private const val PREFS_NAME = "lock_in_daily_unlock_prefs"
    private const val KEY_LAST_DATE = "last_unlock_date"
    private const val KEY_UNLOCKED_PACKAGES = "unlocked_packages"
    private const val KEY_LAST_ELAPSED_REALTIME = "last_elapsed_realtime"
    private const val KEY_LAST_WALL_TIME = "last_wall_time"
    private const val KEY_UNLOCK_REALTIME_PREFIX = "pkg_unlock_realtime_"

    // Minimum physical milliseconds required between unlocks of the same app (at least 18h)
    private const val MIN_PHYSICAL_INTERVAL_MS = 18 * 60 * 60 * 1000L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _unlockedAppsFlow = MutableStateFlow<Set<String>>(emptySet())
    val unlockedAppsFlow: StateFlow<Set<String>> = _unlockedAppsFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    fun init(context: Context) {
        checkDateRollover(context)
        loadFromPrefs(context)
    }

    private fun loadFromPrefs(context: Context) {
        val prefs = getPrefs(context)
        val set = prefs.getStringSet(KEY_UNLOCKED_PACKAGES, emptySet()) ?: emptySet()
        _unlockedAppsFlow.value = set.toSet()
    }

    fun checkDateRollover(context: Context) {
        val prefs = getPrefs(context)
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val lastWallTime = prefs.getLong(KEY_LAST_WALL_TIME, 0L)
        val nowWallTime = System.currentTimeMillis()

        // Clock Rollback Detection: If current time is earlier than last recorded time by > 5 minutes, clock was tampered!
        if (lastWallTime > 0L && nowWallTime < lastWallTime - (5 * 60 * 1000L)) {
            Log.w(TAG, "Clock rollback detected! User moved calendar backwards. Rejecting daily reset.")
            return
        }

        prefs.edit().putLong(KEY_LAST_WALL_TIME, nowWallTime).apply()

        if (lastDate != today && lastDate.isNotBlank()) {
            // Ensure monotonic clock also moved forward significantly or day naturally changed
            val lastElapsed = prefs.getLong(KEY_LAST_ELAPSED_REALTIME, 0L)
            val currentElapsed = SystemClock.elapsedRealtime()

            Log.d(TAG, "New day detected ($lastDate -> $today). Resetting daily unlocked apps.")
            val editor = prefs.edit()
            prefs.all.keys.filter { it.startsWith(KEY_UNLOCK_COUNT_PREFIX) }.forEach { editor.remove(it) }
            editor.putString(KEY_LAST_DATE, today)
                .putStringSet(KEY_UNLOCKED_PACKAGES, emptySet())
                .putLong(KEY_LAST_ELAPSED_REALTIME, currentElapsed)
                .apply()
            _unlockedAppsFlow.value = emptySet()
        } else if (lastDate.isBlank()) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putLong(KEY_LAST_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
                .apply()
        }
    }

    fun getMaxUnlocksAllowed(context: Context): Int {
        return try {
            com.example.LockInApplication.instance.userPreferences.lockLevel.value.maxDailyUnlocksPerApp
        } catch (_: Exception) {
            3
        }
    }

    fun getUnlockCountToday(context: Context, packageName: String): Int {
        checkDateRollover(context)
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_UNLOCK_COUNT_PREFIX + packageName, 0)
    }

    fun isAppUnlockedToday(context: Context, packageName: String): Boolean {
        return getRemainingUnlocksToday(context, packageName) <= 0
    }

    fun recordAppUnlocked(context: Context, packageName: String) {
        checkDateRollover(context)
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_UNLOCK_COUNT_PREFIX + packageName, 0)
        val newCount = currentCount + 1

        val currentSet = prefs.getStringSet(KEY_UNLOCKED_PACKAGES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val maxAllowed = getMaxUnlocksAllowed(context)
        if (newCount >= maxAllowed) {
            currentSet.add(packageName)
        }

        val currentElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_LAST_DATE, getTodayDateString())
            .putInt(KEY_UNLOCK_COUNT_PREFIX + packageName, newCount)
            .putStringSet(KEY_UNLOCKED_PACKAGES, currentSet)
            .putLong(KEY_LAST_WALL_TIME, nowWall)
            .putLong(KEY_LAST_ELAPSED_REALTIME, currentElapsed)
            .putLong(KEY_UNLOCK_REALTIME_PREFIX + packageName, currentElapsed)
            .apply()

        _unlockedAppsFlow.value = currentSet.toSet()
        Log.i(TAG, "Recorded daily unlock ($newCount/$maxAllowed) for: $packageName.")
    }

    fun resetForNewDay(context: Context) {
        val prefs = getPrefs(context)
        val today = getTodayDateString()
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(KEY_UNLOCK_COUNT_PREFIX) }.forEach { editor.remove(it) }
        editor.putString(KEY_LAST_DATE, today)
            .putStringSet(KEY_UNLOCKED_PACKAGES, emptySet())
            .putLong(KEY_LAST_WALL_TIME, System.currentTimeMillis())
            .putLong(KEY_LAST_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
            .apply()

        _unlockedAppsFlow.value = emptySet()
        Log.i(TAG, "Daily unlock tracker reset for new day ($today).")
    }

    fun getRemainingUnlocksToday(context: Context, packageName: String): Int {
        val maxAllowed = getMaxUnlocksAllowed(context)
        val used = getUnlockCountToday(context, packageName)
        return (maxAllowed - used).coerceAtLeast(0)
    }

    private const val KEY_UNLOCK_COUNT_PREFIX = "pkg_unlock_count_"
}
