package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import com.example.BlockedActivity
import com.example.LockInApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages the absolute device screen time budget and the lockout & reset cycle.
 * Example: User sets 23 minutes daily screen time. Once 23 minutes of active phone use are spent,
 * the device enters lockout mode and will ONLY unlock after the configured reset duration (e.g. 2 hours,
 * or default 1 day / 24 hours), resetting the timer back to 23 minutes.
 */
object ScreenTimeManager {
    private const val TAG = "ScreenTimeManager"
    private const val PREFS_NAME = "screen_time_prefs"
    private const val KEY_ENABLED = "screen_time_enabled"
    private const val KEY_LIMIT_MINUTES = "screen_time_limit_minutes"
    private const val KEY_REMAINING_SECONDS = "screen_time_remaining_seconds"
    private const val KEY_RESET_INTERVAL_HOURS = "screen_time_reset_interval_hours"
    private const val KEY_LOCKOUT_UNTIL_EPOCH = "screen_time_lockout_until_epoch"
    private const val KEY_LAST_RESET_EPOCH = "screen_time_last_reset_epoch"

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    private val _limitMinutesFlow = MutableStateFlow(0)
    val limitMinutesFlow: StateFlow<Int> = _limitMinutesFlow.asStateFlow()

    private val _remainingSecondsFlow = MutableStateFlow(0L)
    val remainingSecondsFlow: StateFlow<Long> = _remainingSecondsFlow.asStateFlow()

    // Default reset interval = 24 hours (1 dia)
    private val _resetIntervalHoursFlow = MutableStateFlow(24)
    val resetIntervalHoursFlow: StateFlow<Int> = _resetIntervalHoursFlow.asStateFlow()

    private val _isLockoutActiveFlow = MutableStateFlow(false)
    val isLockoutActiveFlow: StateFlow<Boolean> = _isLockoutActiveFlow.asStateFlow()

    private val _lockoutUntilEpochFlow = MutableStateFlow(0L)
    val lockoutUntilEpochFlow: StateFlow<Long> = _lockoutUntilEpochFlow.asStateFlow()

    private val _isEnabledFlow = MutableStateFlow(false)
    val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    @Volatile
    private var cachedLastResetEpoch: Long = System.currentTimeMillis()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = getPrefs(context)
        val hasLimit = prefs.contains(KEY_LIMIT_MINUTES)
        val enabled = if (hasLimit) prefs.getBoolean(KEY_ENABLED, true) else false
        val limitMinutes = prefs.getInt(KEY_LIMIT_MINUTES, 0)
        val remainingSeconds = prefs.getLong(KEY_REMAINING_SECONDS, limitMinutes * 60L)
        val resetIntervalHours = prefs.getInt(KEY_RESET_INTERVAL_HOURS, 24) // Default: 1 dia
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_EPOCH, 0L)
        val lastReset = prefs.getLong(KEY_LAST_RESET_EPOCH, System.currentTimeMillis())
        cachedLastResetEpoch = lastReset

        _isEnabledFlow.value = enabled
        _limitMinutesFlow.value = limitMinutes
        _resetIntervalHoursFlow.value = resetIntervalHours
        _lockoutUntilEpochFlow.value = lockoutUntil

        val now = System.currentTimeMillis()

        if (!enabled || limitMinutes <= 0) {
            _remainingSecondsFlow.value = 0L
            _isLockoutActiveFlow.value = false
            startMonitoring(context.applicationContext)
            return
        }

        // 1. Check if lockout was active and whether it expired
        if (lockoutUntil > 0L) {
            if (now >= lockoutUntil) {
                // Lockout time ended: reset screen time and unlock!
                performReset(context, limitMinutes)
            } else {
                _remainingSecondsFlow.value = 0L
                _isLockoutActiveFlow.value = true
            }
        } else {
            // 2. Check if regular scheduled reset cycle elapsed (e.g. 24h passed)
            val intervalMillis = resetIntervalHours * 3600_000L
            if (now - lastReset >= intervalMillis) {
                performReset(context, limitMinutes)
            } else {
                _remainingSecondsFlow.value = remainingSeconds.coerceAtLeast(0L)
                _isLockoutActiveFlow.value = remainingSeconds <= 0L
            }
        }

        startMonitoring(context.applicationContext)
    }

    fun stopMonitoring() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun startMonitoring(context: Context) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            var saveCounter = 0

            while (isActive) {
                delay(1000L)

                if (DeviceAdminManager.isUninstallAuthorized(context)) {
                    continue
                }

                val isInteractive = powerManager?.isInteractive ?: true


                if (!_isEnabledFlow.value) continue

                val now = System.currentTimeMillis()
                val lockoutUntil = _lockoutUntilEpochFlow.value

                // If currently locked out: check if reset time arrived
                if (_isLockoutActiveFlow.value) {
                    if (lockoutUntil > 0L && now >= lockoutUntil) {
                        Log.d(TAG, "Reset interval elapsed! Resetting screen time and unlocking.")
                        performReset(context, _limitMinutesFlow.value)
                    }
                    continue
                }

                // If phone screen is interactive / active
                if (!isInteractive) continue

                // Check background limit depletion
                val currentRem = _remainingSecondsFlow.value
                if (currentRem > 0) {
                    val nextRem = currentRem - 1
                    _remainingSecondsFlow.value = nextRem
                    saveCounter++

                    if (saveCounter % 15 == 0 || nextRem <= 0L) {
                        getPrefs(context).edit().putLong(KEY_REMAINING_SECONDS, nextRem).apply()
                        com.example.widget.LockInWidgetManager.updateAllWidgets(context)
                        DailyResetManager.checkResetIfDue(context)
                    }

                    if (nextRem <= 0L) {
                        // Limit reached! Trigger lockout!
                        triggerLockout(context)
                    }
                }
            }
        }
    }

    private fun triggerLockout(context: Context) {
        val now = System.currentTimeMillis()
        val intervalHours = _resetIntervalHoursFlow.value
        val lockoutUntil = now + (intervalHours * 3600_000L)

        _remainingSecondsFlow.value = 0L
        _isLockoutActiveFlow.value = true
        _lockoutUntilEpochFlow.value = lockoutUntil

        getPrefs(context).edit()
            .putLong(KEY_REMAINING_SECONDS, 0L)
            .putLong(KEY_LOCKOUT_UNTIL_EPOCH, lockoutUntil)
            .apply()

        Log.d(TAG, "Screen time exhausted! Locked out until $lockoutUntil ($intervalHours hours)")
        com.example.widget.LockInWidgetManager.updateAllWidgets(context)
        bringLockoutToFront(context)
    }

    private fun performReset(context: Context, limitMinutes: Int) {
        val fullSeconds = limitMinutes * 60L
        val now = System.currentTimeMillis()

        cachedLastResetEpoch = now
        _remainingSecondsFlow.value = fullSeconds
        _isLockoutActiveFlow.value = false
        _lockoutUntilEpochFlow.value = 0L

        getPrefs(context).edit()
            .putLong(KEY_REMAINING_SECONDS, fullSeconds)
            .putLong(KEY_LOCKOUT_UNTIL_EPOCH, 0L)
            .putLong(KEY_LAST_RESET_EPOCH, now)
            .apply()

        com.example.widget.LockInWidgetManager.updateAllWidgets(context)
    }

    fun resetDailyUsage(context: Context) {
        val limit = _limitMinutesFlow.value
        if (limit > 0) {
            performReset(context, limit)
        }
    }

    fun isLockoutActive(context: Context): Boolean {
        if (!_isEnabledFlow.value || _limitMinutesFlow.value <= 0) return false
        val lockoutUntil = _lockoutUntilEpochFlow.value
        if (lockoutUntil > 0L) {
            val now = System.currentTimeMillis()
            if (now >= lockoutUntil) {
                performReset(context, _limitMinutesFlow.value)
                return false
            }
            return true
        }
        return _isLockoutActiveFlow.value
    }

    fun bringLockoutToFront(context: Context) {
        try {
            val intent = Intent(context, BlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(BlockedActivity.EXTRA_BLOCKED_TARGET, "Tempo Limite de Tela")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error bringing lockout to front", e)
        }
    }

    fun setLimitMinutes(context: Context, minutes: Int) {
        if (minutes <= 0) {
            _limitMinutesFlow.value = 0
            _remainingSecondsFlow.value = 0L
            _isEnabledFlow.value = false
            _isLockoutActiveFlow.value = false
            _lockoutUntilEpochFlow.value = 0L
            getPrefs(context).edit()
                .putBoolean(KEY_ENABLED, false)
                .putInt(KEY_LIMIT_MINUTES, 0)
                .putLong(KEY_REMAINING_SECONDS, 0L)
                .putLong(KEY_LOCKOUT_UNTIL_EPOCH, 0L)
                .apply()
            stopMonitoring()
            return
        }

        val safeMinutes = minutes.coerceAtLeast(1)
        _limitMinutesFlow.value = safeMinutes
        _isEnabledFlow.value = true
        val fullSeconds = safeMinutes * 60L

        if (!_isLockoutActiveFlow.value) {
            val currentRem = _remainingSecondsFlow.value
            val newRem = if (currentRem <= 0 || currentRem > fullSeconds) fullSeconds else currentRem
            _remainingSecondsFlow.value = newRem
            getPrefs(context).edit()
                .putBoolean(KEY_ENABLED, true)
                .putInt(KEY_LIMIT_MINUTES, safeMinutes)
                .putLong(KEY_REMAINING_SECONDS, newRem)
                .apply()
        } else {
            getPrefs(context).edit()
                .putBoolean(KEY_ENABLED, true)
                .putInt(KEY_LIMIT_MINUTES, safeMinutes)
                .apply()
        }
        startMonitoring(context.applicationContext)
        try {
            com.example.LockInApplication.instance.applicationScope.launch {
                CloudSyncManager.syncDataToCloud(context)
            }
        } catch (_: Exception) {}
    }

    fun setResetIntervalHours(context: Context, hours: Int) {
        val safeHours = hours.coerceAtLeast(1)
        _resetIntervalHoursFlow.value = safeHours
        getPrefs(context).edit()
            .putInt(KEY_RESET_INTERVAL_HOURS, safeHours)
            .apply()
        try {
            com.example.LockInApplication.instance.applicationScope.launch {
                CloudSyncManager.syncDataToCloud(context)
            }
        } catch (_: Exception) {}
    }

    fun resetScreenTimeNow(context: Context) {
        performReset(context, _limitMinutesFlow.value)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _isEnabledFlow.value = enabled
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) {
            _isLockoutActiveFlow.value = false
            _lockoutUntilEpochFlow.value = 0L
            getPrefs(context).edit().putLong(KEY_LOCKOUT_UNTIL_EPOCH, 0L).apply()
        }
    }

    fun getSecondsUntilReset(): Long {
        val lockoutUntil = _lockoutUntilEpochFlow.value
        val now = System.currentTimeMillis()
        if (lockoutUntil > now) {
            return (lockoutUntil - now) / 1000L
        }
        val lastReset = cachedLastResetEpoch
        val intervalMillis = _resetIntervalHoursFlow.value * 3600_000L
        val nextScheduled = lastReset + intervalMillis
        return ((nextScheduled - now) / 1000L).coerceAtLeast(0L)
    }
}
