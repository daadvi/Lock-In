package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.example.data.model.LockLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lock_in_user_prefs", Context.MODE_PRIVATE)

    private val _isRegistered = MutableStateFlow(prefs.getBoolean(KEY_IS_REGISTERED, false))
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "Foco") ?: "Foco")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _textColorHex = MutableStateFlow(prefs.getString(KEY_TEXT_COLOR, "#FF5500") ?: "#FF5500")
    val textColorHex: StateFlow<String> = _textColorHex.asStateFlow()

    private val _isSharedLimitMode = MutableStateFlow(prefs.getBoolean(KEY_IS_SHARED_LIMIT, true))
    val isSharedLimitMode: StateFlow<Boolean> = _isSharedLimitMode.asStateFlow()

    private val _sharedLimitMinutes = MutableStateFlow(prefs.getInt(KEY_SHARED_MINUTES, 20))
    val sharedLimitMinutes: StateFlow<Int> = _sharedLimitMinutes.asStateFlow()

    private val _sharedRemainingSeconds = MutableStateFlow(prefs.getLong(KEY_SHARED_REMAINING, 20 * 60L))
    val sharedRemainingSeconds: StateFlow<Long> = _sharedRemainingSeconds.asStateFlow()

    private val _resetIntervalHours = MutableStateFlow(prefs.getInt(KEY_RESET_INTERVAL_HOURS, 24))
    val resetIntervalHours: StateFlow<Int> = _resetIntervalHours.asStateFlow()

    private val _resetHour = MutableStateFlow(prefs.getInt(KEY_RESET_HOUR, 0))
    val resetHour: StateFlow<Int> = _resetHour.asStateFlow()

    private val _floatingIslandSize = MutableStateFlow(prefs.getString(KEY_ISLAND_SIZE, "MEDIUM") ?: "MEDIUM")
    val floatingIslandSize: StateFlow<String> = _floatingIslandSize.asStateFlow()

    private val _floatingIslandOpacity = MutableStateFlow(prefs.getFloat(KEY_ISLAND_OPACITY, 0.95f))
    val floatingIslandOpacity: StateFlow<Float> = _floatingIslandOpacity.asStateFlow()

    private val _floatingIslandPosX = MutableStateFlow(prefs.getFloat(KEY_ISLAND_POS_X, 0.75f))
    val floatingIslandPosX: StateFlow<Float> = _floatingIslandPosX.asStateFlow()

    private val _floatingIslandPosY = MutableStateFlow(prefs.getFloat(KEY_ISLAND_POS_Y, 0.12f))
    val floatingIslandPosY: StateFlow<Float> = _floatingIslandPosY.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "LIGHT") ?: "LIGHT")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true))
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _streakReminders = MutableStateFlow(prefs.getBoolean(KEY_STREAK_REMINDERS, true))
    val streakReminders: StateFlow<Boolean> = _streakReminders.asStateFlow()

    private val _preLimitAlerts = MutableStateFlow(prefs.getBoolean(KEY_PRE_LIMIT_ALERTS, true))
    val preLimitAlerts: StateFlow<Boolean> = _preLimitAlerts.asStateFlow()

    private val _mindfulDelay = MutableStateFlow(prefs.getBoolean(KEY_MINDFUL_DELAY, false))
    val mindfulDelay: StateFlow<Boolean> = _mindfulDelay.asStateFlow()

    private val _lockLevel = MutableStateFlow(
        LockLevel.fromId(prefs.getString(KEY_LOCK_LEVEL, LockLevel.BASIC.id))
    )
    val lockLevel: StateFlow<LockLevel> = _lockLevel.asStateFlow()

    private val _hasSelectedInitialLockLevel = MutableStateFlow(
        prefs.getBoolean(KEY_HAS_SELECTED_INITIAL_LOCK_LEVEL, false)
    )
    val hasSelectedInitialLockLevel: StateFlow<Boolean> = _hasSelectedInitialLockLevel.asStateFlow()

    // Bloqueio absoluto: sempre ativo por padrão. Só pode ser desativado pelo desafio
    // do nível de lock (senha Nx + frase), e jamais no nível Sonho do Dev.
    private val _isAbsoluteInstallBlock = MutableStateFlow(
        prefs.getBoolean(KEY_IS_ABSOLUTE_INSTALL_BLOCK, true)
    )
    val isAbsoluteInstallBlock: StateFlow<Boolean> = _isAbsoluteInstallBlock.asStateFlow()

    fun setRegistration(name: String, password: String) {
        val securePassword = com.example.util.CryptoPasswordHelper.hashPassword(password)
        prefs.edit()
            .putBoolean(KEY_IS_REGISTERED, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_PASSWORD, securePassword)
            .apply()
        _isRegistered.value = true
        _userName.value = name
    }

    fun getTrustPassword(): String {
        return prefs.getString(KEY_PASSWORD, "") ?: ""
    }

    fun verifyTrustPassword(candidate: String): Boolean {
        val stored = getTrustPassword()
        return com.example.util.CryptoPasswordHelper.verifyPassword(candidate, stored)
    }

    fun setTextColorHex(hex: String) {
        prefs.edit().putString(KEY_TEXT_COLOR, hex).apply()
        _textColorHex.value = hex
    }

    fun setSharedLimitMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SHARED_LIMIT, enabled).apply()
        _isSharedLimitMode.value = enabled
    }

    fun setSharedLimitMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_SHARED_MINUTES, minutes)
            .putLong(KEY_SHARED_REMAINING, minutes * 60L)
            .apply()
        _sharedLimitMinutes.value = minutes
        _sharedRemainingSeconds.value = minutes * 60L
    }

    fun updateSharedRemainingSeconds(seconds: Long) {
        val safeSeconds = seconds.coerceAtLeast(0L)
        prefs.edit().putLong(KEY_SHARED_REMAINING, safeSeconds).apply()
        _sharedRemainingSeconds.value = safeSeconds
    }

    fun resetSharedTimer() {
        val full = _sharedLimitMinutes.value * 60L
        updateSharedRemainingSeconds(full)
    }

    fun setResetSchedule(intervalHours: Int, hourOfDay: Int = 0) {
        prefs.edit()
            .putInt(KEY_RESET_INTERVAL_HOURS, intervalHours)
            .putInt(KEY_RESET_HOUR, hourOfDay)
            .apply()
        _resetIntervalHours.value = intervalHours
        _resetHour.value = hourOfDay
    }

    fun setFloatingIslandSettings(size: String, opacity: Float, posX: Float, posY: Float) {
        prefs.edit()
            .putString(KEY_ISLAND_SIZE, size)
            .putFloat(KEY_ISLAND_OPACITY, opacity)
            .putFloat(KEY_ISLAND_POS_X, posX)
            .putFloat(KEY_ISLAND_POS_Y, posY)
            .apply()
        _floatingIslandSize.value = size
        _floatingIslandOpacity.value = opacity
        _floatingIslandPosX.value = posX
        _floatingIslandPosY.value = posY
    }

    fun getLastResetEpoch(): Long {
        return prefs.getLong(KEY_LAST_RESET_EPOCH, 0L)
    }

    fun setLastResetEpoch(epoch: Long) {
        recordResetTimestamp(epoch)
    }

    fun getLastResetDayOfYear(): Int {
        return prefs.getInt(KEY_LAST_RESET_DAY_OF_YEAR, -1)
    }

    fun getLastResetYear(): Int {
        return prefs.getInt(KEY_LAST_RESET_YEAR, -1)
    }

    fun recordResetTimestamp(epoch: Long) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epoch }
        prefs.edit()
            .putLong(KEY_LAST_RESET_EPOCH, epoch)
            .putInt(KEY_LAST_RESET_DAY_OF_YEAR, cal.get(java.util.Calendar.DAY_OF_YEAR))
            .putInt(KEY_LAST_RESET_YEAR, cal.get(java.util.Calendar.YEAR))
            .apply()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
        _hapticFeedback.value = enabled
    }

    fun setStreakReminders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STREAK_REMINDERS, enabled).apply()
        _streakReminders.value = enabled
    }

    fun setPreLimitAlerts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRE_LIMIT_ALERTS, enabled).apply()
        _preLimitAlerts.value = enabled
    }

    fun setMindfulDelay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MINDFUL_DELAY, enabled).apply()
        _mindfulDelay.value = enabled
    }

    fun updatePassword(newPass: String) {
        val secureHash = com.example.util.CryptoPasswordHelper.hashPassword(newPass)
        prefs.edit().putString(KEY_PASSWORD, secureHash).apply()
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim().ifBlank { "Foco" }
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
        _userName.value = trimmed
    }

    fun resetAllSettingsToDefault() {
        prefs.edit()
            .putString(KEY_TEXT_COLOR, "#FF5500")
            .putString(KEY_THEME_MODE, "LIGHT")
            .putBoolean(KEY_HAPTIC_FEEDBACK, true)
            .putBoolean(KEY_STREAK_REMINDERS, true)
            .putBoolean(KEY_PRE_LIMIT_ALERTS, true)
            .putBoolean(KEY_MINDFUL_DELAY, false)
            .putString(KEY_ISLAND_SIZE, "MEDIUM")
            .putFloat(KEY_ISLAND_OPACITY, 0.95f)
            .apply()
        _textColorHex.value = "#FF5500"
        _themeMode.value = "LIGHT"
        _hapticFeedback.value = true
        _streakReminders.value = true
        _preLimitAlerts.value = true
        _mindfulDelay.value = false
        _floatingIslandSize.value = "MEDIUM"
        _floatingIslandOpacity.value = 0.95f
    }

    fun getLockLevel(): LockLevel = _lockLevel.value

    fun setLockLevel(level: LockLevel) {
        prefs.edit()
            .putString(KEY_LOCK_LEVEL, level.id)
            .putBoolean(KEY_HAS_SELECTED_INITIAL_LOCK_LEVEL, true)
            .apply()
        _lockLevel.value = level
        _hasSelectedInitialLockLevel.value = true
        // No nível máximo (Sonho do Dev) nada pode sair da lista de bloqueio:
        // o bloqueio absoluto é reativado e nunca mais poderá ser desativado.
        if (level == LockLevel.DEV_DREAM) setAbsoluteInstallBlock(true)
    }

    fun setAbsoluteInstallBlock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ABSOLUTE_INSTALL_BLOCK, enabled).apply()
        _isAbsoluteInstallBlock.value = enabled
    }

    fun isAbsoluteInstallBlockActive(): Boolean = _isAbsoluteInstallBlock.value

    fun deactivateAbsoluteInstallBlockWithChallenge() {
        if (_lockLevel.value == LockLevel.DEV_DREAM) return // Nível máximo: permanente
        prefs.edit().putBoolean(KEY_IS_ABSOLUTE_INSTALL_BLOCK, false).apply()
        _isAbsoluteInstallBlock.value = false
    }

    companion object {
        private const val KEY_IS_REGISTERED = "key_is_registered"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_TEXT_COLOR = "key_text_color"
        private const val KEY_IS_SHARED_LIMIT = "key_is_shared_limit"
        private const val KEY_SHARED_MINUTES = "key_shared_minutes"
        private const val KEY_SHARED_REMAINING = "key_shared_remaining"
        private const val KEY_RESET_INTERVAL_HOURS = "key_reset_interval_hours"
        private const val KEY_RESET_HOUR = "key_reset_hour"
        private const val KEY_ISLAND_SIZE = "key_island_size"
        private const val KEY_ISLAND_OPACITY = "key_island_opacity"
        private const val KEY_ISLAND_POS_X = "key_island_pos_x"
        private const val KEY_ISLAND_POS_Y = "key_island_pos_y"
        private const val KEY_LAST_RESET_EPOCH = "key_last_reset_epoch"
        private const val KEY_LAST_RESET_DAY_OF_YEAR = "key_last_reset_day_of_year"
        private const val KEY_LAST_RESET_YEAR = "key_last_reset_year"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"
        private const val KEY_STREAK_REMINDERS = "key_streak_reminders"
        private const val KEY_PRE_LIMIT_ALERTS = "key_pre_limit_alerts"
        private const val KEY_MINDFUL_DELAY = "key_mindful_delay"
        private const val KEY_LOCK_LEVEL = "key_lock_level"
        private const val KEY_HAS_SELECTED_INITIAL_LOCK_LEVEL = "key_has_selected_initial_lock_level"
        private const val KEY_IS_ABSOLUTE_INSTALL_BLOCK = "key_is_absolute_install_block"
    }
}
