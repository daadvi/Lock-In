package com.example.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StreakManager {
    private const val PREFS_NAME = "lock_in_streak_prefs"
    private const val KEY_STREAK_DAYS = "streak_days_count"
    private const val KEY_RECORD_STREAK = "streak_record_days"
    private const val KEY_LAST_OBEYED_DATE = "streak_last_obeyed_date"
    private const val KEY_TOTAL_OBEYED_DAYS = "streak_total_obeyed"
    private const val KEY_TODAY_VIOLATED = "streak_today_violated"

    private val _streakFlow = MutableStateFlow(0)
    val streakFlow: StateFlow<Int> = _streakFlow.asStateFlow()

    private val _streakJustIncreased = MutableStateFlow(false)
    val streakJustIncreased: StateFlow<Boolean> = _streakJustIncreased.asStateFlow()

    fun dismissStreakCelebration() {
        _streakJustIncreased.value = false
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Volatile
    private var isCheckingNewDay = false

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_STREAK_DAYS, 0)
        _streakFlow.value = current
        checkNewDay(context)
    }

    fun getStreak(context: Context): Int {
        checkNewDay(context)
        val prefs = getPrefs(context)
        val streak = prefs.getInt(KEY_STREAK_DAYS, 0)
        _streakFlow.value = streak
        return streak
    }

    fun getRecordStreak(context: Context): Int {
        val prefs = getPrefs(context)
        val record = prefs.getInt(KEY_RECORD_STREAK, 0)
        val current = prefs.getInt(KEY_STREAK_DAYS, 0)
        return maxOf(record, current)
    }

    fun getTotalObeyedDays(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_TOTAL_OBEYED_DAYS, 0)
    }

    /**
     * Called when the user bypasses limits using the Trust Unlock security password.
     * Discipline is broken: Streak drops immediately to 0 and cannot be manually restored!
     */
    fun markViolation(context: Context) {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        prefs.edit()
            .putBoolean(KEY_TODAY_VIOLATED, true)
            .putString(KEY_LAST_OBEYED_DATE, todayStr)
            .putInt(KEY_STREAK_DAYS, 0)
            .apply()
        _streakFlow.value = 0
        com.example.widget.LockInWidgetManager.updateAllWidgets(context)
    }

    fun isTodayViolated(context: Context): Boolean {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        val lastDate = prefs.getString(KEY_LAST_OBEYED_DATE, "")
        if (lastDate != todayStr) return false
        return prefs.getBoolean(KEY_TODAY_VIOLATED, false)
    }

    /**
     * Evaluates automatic streak progression.
     * Automatically increments +1 for each day limits were respected without Trust Unlock.
     */
    fun checkNewDay(context: Context) {
        if (isCheckingNewDay) return
        isCheckingNewDay = true
        try {
            val prefs = getPrefs(context)
            val todayStr = dateFormat.format(Date())
            val lastDate = prefs.getString(KEY_LAST_OBEYED_DATE, null)

            if (lastDate == null) {
                // First run on new device: initialize starting point
                prefs.edit()
                    .putString(KEY_LAST_OBEYED_DATE, todayStr)
                    .putInt(KEY_STREAK_DAYS, 0)
                    .putInt(KEY_RECORD_STREAK, 0)
                    .putInt(KEY_TOTAL_OBEYED_DAYS, 0)
                    .putBoolean(KEY_TODAY_VIOLATED, false)
                    .apply()
                _streakFlow.value = 0
                return
            }

            if (lastDate != todayStr) {
                val wasViolated = prefs.getBoolean(KEY_TODAY_VIOLATED, false)
                val currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0)
                val currentRecord = prefs.getInt(KEY_RECORD_STREAK, 0)
                val totalObeyed = prefs.getInt(KEY_TOTAL_OBEYED_DAYS, 0)

                val daysElapsed = try {
                    val lastD = dateFormat.parse(lastDate)
                    val todayD = dateFormat.parse(todayStr)
                    if (lastD != null && todayD != null) {
                        val diff = ((todayD.time - lastD.time) / (1000 * 60 * 60 * 24)).toInt()
                        diff.coerceAtLeast(1)
                    } else 1
                } catch (_: Exception) { 1 }

                val newStreak = if (wasViolated) {
                    // If the last tracked day had a violation, that day was 0.
                    // Subsequent days without violation (if more than 1 day passed) count:
                    (daysElapsed - 1).coerceAtLeast(0)
                } else {
                    // Limits were obeyed without trust unlock: streak increases automatically!
                    currentStreak + daysElapsed
                }

                if (!wasViolated && newStreak > currentStreak) {
                    _streakJustIncreased.value = true
                }

                val newRecord = maxOf(currentRecord, newStreak)
                val addedObeyed = if (!wasViolated) daysElapsed else (daysElapsed - 1).coerceAtLeast(0)
                val newTotal = totalObeyed + addedObeyed

                prefs.edit()
                    .putInt(KEY_STREAK_DAYS, newStreak)
                    .putInt(KEY_RECORD_STREAK, newRecord)
                    .putInt(KEY_TOTAL_OBEYED_DAYS, newTotal)
                    .putString(KEY_LAST_OBEYED_DATE, todayStr)
                    .putBoolean(KEY_TODAY_VIOLATED, false)
                    .apply()

                _streakFlow.value = newStreak
                com.example.widget.LockInWidgetManager.updateAllWidgets(context)
            }
        } finally {
            isCheckingNewDay = false
        }
    }

    /**
     * Represents a single tile/cell in the consistency tower.
     */
    enum class TowerCellState {
        COMPLETED, // Dark slate/charcoal block
        CURRENT,   // Bright accent color block
        EMPTY      // Light subtle gray block
    }

    /**
     * Generates a 10-column tower grid representation based on current streak.
     */
    fun getTowerGrid(streakDays: Int, columns: Int = 10, visibleRows: Int = 8): List<List<TowerCellState>> {
        val completedInRow = if (streakDays > 0) (streakDays - 1) % columns else 0
        val currentRowIndex = visibleRows - 1

        val grid = mutableListOf<List<TowerCellState>>()

        for (row in 0 until visibleRows) {
            val rowCells = mutableListOf<TowerCellState>()
            val isCurrentRow = (row == currentRowIndex)
            val isBelowCurrentRow = (row < currentRowIndex)

            for (col in 0 until columns) {
                val state = when {
                    isBelowCurrentRow -> TowerCellState.COMPLETED
                    isCurrentRow -> when {
                        col < completedInRow -> TowerCellState.COMPLETED
                        col == completedInRow -> TowerCellState.CURRENT
                        else -> TowerCellState.EMPTY
                    }
                    else -> TowerCellState.EMPTY
                }
                rowCells.add(state)
            }
            grid.add(rowCells)
        }

        return grid.reversed()
    }
}
