package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.example.FocusLockActivity
import com.example.data.local.FocusRoutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FocusTimeManager {
    private const val PREFS_NAME = "lock_in_focus_time_prefs"
    private const val KEY_IS_ACTIVE = "focus_is_active"
    private const val KEY_END_EPOCH = "focus_end_epoch"
    private const val KEY_END_ELAPSED = "focus_end_elapsed"
    private const val KEY_START_BOOT = "focus_start_boot"
    private const val KEY_TOTAL_MINUTES = "focus_total_minutes"
    private const val KEY_ACTIVE_ROUTINE_NAME = "focus_active_routine_name"
    private const val KEY_ALLOWED_PACKAGES = "focus_allowed_packages"
    private const val KEY_ALLOWED_PACKAGES_JSON = "focus_allowed_packages_json"
    private const val KEY_ROUTINES_JSON = "focus_routines_json"
    private const val KEY_CUSTOM_PRESETS_JSON = "focus_custom_presets_json"

    private val _isFocusActiveFlow = MutableStateFlow(false)
    val isFocusActiveFlow: StateFlow<Boolean> = _isFocusActiveFlow.asStateFlow()

    private val _remainingSecondsFlow = MutableStateFlow(0L)
    val remainingSecondsFlow: StateFlow<Long> = _remainingSecondsFlow.asStateFlow()

    @Volatile
    private var cachedAllowedList: List<String>? = null
    @Volatile
    private var cachedAllowedSet: Set<String>? = null
    @Volatile
    private var cachedRoutinesList: List<FocusRoutine>? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Wall time is used only for reboot/legacy recovery. During one boot, changing
    // the clock cannot shorten or prolong a direct duration-based focus session.
    private fun directRemainingMillis(context: Context): Long {
        val prefs = getPrefs(context)
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        val elapsedEnd = prefs.getLong(KEY_END_ELAPSED, 0L)
        return if (boot >= 0 && prefs.getInt(KEY_START_BOOT, -2) == boot && elapsedEnd > 0) {
            (elapsedEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        } else (prefs.getLong(KEY_END_EPOCH, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun init(context: Context) {
        checkStatus(context)
    }

    /**
     * Checks if a direct focus session or a routine is active right now.
     */
    fun checkStatus(context: Context): Boolean {
        val prefs = getPrefs(context)
        val endEpoch = prefs.getLong(KEY_END_EPOCH, 0L)
        val remaining = directRemainingMillis(context)

        if (remaining > 0) {
            _isFocusActiveFlow.value = true
            _remainingSecondsFlow.value = remaining / 1000L
            return true
        } else if (endEpoch > 0L) {
            // Direct focus session expired
            prefs.edit()
                .putBoolean(KEY_IS_ACTIVE, false)
                .putLong(KEY_END_EPOCH, 0L)
            .remove(KEY_END_ELAPSED).remove(KEY_START_BOOT)
                .putString(KEY_ACTIVE_ROUTINE_NAME, "")
                .apply()
        }

        // Check if any scheduled routine is active at this moment
        val activeRoutine = findActiveRoutine(context)
        if (activeRoutine != null) {
            _isFocusActiveFlow.value = true
            _remainingSecondsFlow.value = calculateRoutineRemainingSeconds(activeRoutine)
            return true
        }

        _isFocusActiveFlow.value = false
        _remainingSecondsFlow.value = 0L
        return false
    }

    fun isFocusTimeActive(context: Context): Boolean {
        return checkStatus(context)
    }

    fun getRemainingSeconds(context: Context): Long {
        checkStatus(context)
        val prefs = getPrefs(context)
        val endEpoch = prefs.getLong(KEY_END_EPOCH, 0L)
        val remaining = directRemainingMillis(context)
        if (remaining > 0) return remaining / 1000L
        val activeRoutine = findActiveRoutine(context)
        if (activeRoutine != null) {
            return calculateRoutineRemainingSeconds(activeRoutine)
        }
        return 0L
    }

    fun getTotalFocusMinutes(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_TOTAL_MINUTES, 15)
    }

    fun getActiveRoutineName(context: Context): String? {
        val prefs = getPrefs(context)
        val directName = prefs.getString(KEY_ACTIVE_ROUTINE_NAME, null)
        if (!directName.isNullOrBlank()) return directName
        return findActiveRoutine(context)?.name
    }

    fun getEndTimeFormatted(context: Context): String {
        val prefs = getPrefs(context)
        val endEpoch = prefs.getLong(KEY_END_EPOCH, 0L)
        val remaining = directRemainingMillis(context)
        if (remaining > 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(System.currentTimeMillis() + remaining))
        }

        val activeRoutine = findActiveRoutine(context)
        if (activeRoutine != null) {
            return String.format("%02d:%02d", activeRoutine.endHour, activeRoutine.endMinute)
        }
        return "--:--"
    }

    /**
     * Start a direct Focus Time session immediately.
     */
    fun startFocusSession(
        context: Context,
        durationMinutes: Int,
        allowedPackages: List<String>,
        routineName: String? = null
    ) {
        val safeDuration = durationMinutes.coerceAtLeast(1)
        val endEpoch = System.currentTimeMillis() + (safeDuration * 60L * 1000L)
        val safeAllowed = if (allowedPackages.isNotEmpty()) {
            allowedPackages.distinct().take(3)
        } else {
            getAllowedPackages(context).take(3)
        }

        // Persist allowed packages
        setEmergencyAllowedPackages(context, safeAllowed)

        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, true)
            .putLong(KEY_END_EPOCH, endEpoch)
            .putLong(KEY_END_ELAPSED, SystemClock.elapsedRealtime() + safeDuration * 60_000L)
            .putInt(KEY_START_BOOT, Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1))
            .putInt(KEY_TOTAL_MINUTES, safeDuration)
            .putString(KEY_ACTIVE_ROUTINE_NAME, routineName ?: "Focus Time")
            .putStringSet(KEY_ALLOWED_PACKAGES, safeAllowed.toSet())
            .apply()

        cachedAllowedList = safeAllowed
        cachedAllowedSet = safeAllowed.toSet()

        _isFocusActiveFlow.value = true
        _remainingSecondsFlow.value = safeDuration * 60L

        com.example.widget.LockInWidgetManager.updateAllWidgets(context)

        // Immediately launch the full-screen Nothing OS lock screen
        bringFocusLockToFront(context)
    }

    fun stopFocusSession(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, false)
            .putLong(KEY_END_EPOCH, 0L)
            .remove(KEY_END_ELAPSED).remove(KEY_START_BOOT)
            .putString(KEY_ACTIVE_ROUTINE_NAME, "")
            .apply()
        _isFocusActiveFlow.value = false
        _remainingSecondsFlow.value = 0L
        com.example.widget.LockInWidgetManager.updateAllWidgets(context)
    }

    /**
     * Retrieves the emergency allowed packages saved by the user (max 3 apps).
     */
    fun getAllowedPackages(context: Context): List<String> {
        // Routine permissions belong only to the currently active routine. Never cache
        // them as the user's default/direct-session list across schedule transitions.
        if (directRemainingMillis(context) <= 0) {
            findActiveRoutine(context)?.let { return it.allowedPackages.distinct().take(3) }
        }
        val cached = cachedAllowedList
        if (cached != null) return cached

        val prefs = getPrefs(context)

        // First check JSON ordered list
        val jsonStr = prefs.getString(KEY_ALLOWED_PACKAGES_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                if (list.isNotEmpty()) {
                    val result = list.distinct().take(3)
                    cachedAllowedList = result
                    cachedAllowedSet = result.toSet()
                    return result
                }
            } catch (_: Exception) {}
        }

        val endEpoch = prefs.getLong(KEY_END_EPOCH, 0L)
        if (directRemainingMillis(context) > 0) {
            val set = prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
            if (set.isNotEmpty()) {
                val result = set.take(3)
                cachedAllowedList = result
                cachedAllowedSet = result.toSet()
                return result
            }
        }
        val fallbackSet = prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
        val result = fallbackSet.take(3)
        cachedAllowedList = result
        cachedAllowedSet = result.toSet()
        return result
    }

    /**
     * Set representation for O(1) membership check during accessibility service events.
     */
    fun getAllowedPackagesSet(context: Context): Set<String> {
        return getAllowedPackages(context).toSet()
    }

    fun setEmergencyAllowedPackages(context: Context, packages: List<String>) {
        val safe = packages.distinct().take(3)
        cachedAllowedList = safe
        cachedAllowedSet = safe.toSet()

        val jsonArray = JSONArray()
        safe.forEach { jsonArray.put(it) }

        getPrefs(context).edit()
            .putString(KEY_ALLOWED_PACKAGES_JSON, jsonArray.toString())
            .putStringSet(KEY_ALLOWED_PACKAGES, safe.toSet())
            .apply()
    }

    // Launching an app never grants an exception beyond the active policy.
    fun notifyAllowedAppLaunched(packageName: String) = Unit

    fun isPackageAllowed(context: Context, packageName: String): Boolean {
        if (PhoneAppHelper.isPhoneDialerApp(context, packageName)) return true
        return packageName in getAllowedPackagesSet(context)
    }

    /**
     * Brings FocusLockActivity to the front immediately.
     */
    fun bringFocusLockToFront(context: Context) {
        try {
            val intent = Intent(context, FocusLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    // --- Routine Management ---

    fun getRoutines(context: Context): List<FocusRoutine> {
        val cached = cachedRoutinesList
        if (cached != null) return cached

        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_ROUTINES_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            cachedRoutinesList = emptyList()
            return emptyList()
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FocusRoutine>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysArr = obj.optJSONArray("daysOfWeek")
                val days = mutableListOf<Int>()
                if (daysArr != null) {
                    for (d in 0 until daysArr.length()) {
                        days.add(daysArr.getInt(d))
                    }
                } else {
                    days.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                }

                val pkgsArr = obj.optJSONArray("allowedPackages")
                val pkgs = mutableListOf<String>()
                if (pkgsArr != null) {
                    for (p in 0 until pkgsArr.length()) {
                        pkgs.add(pkgsArr.getString(p))
                    }
                }

                val targetPackage = obj.optString("targetPackage", "").takeIf { it.isNotBlank() }
                val targetAppName = obj.optString("targetAppName", "").takeIf { it.isNotBlank() }

                list.add(
                    FocusRoutine(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        startHour = obj.getInt("startHour"),
                        startMinute = obj.getInt("startMinute"),
                        endHour = obj.getInt("endHour"),
                        endMinute = obj.getInt("endMinute"),
                        daysOfWeek = days,
                        allowedPackages = pkgs.take(3),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        targetPackage = targetPackage,
                        targetAppName = targetAppName
                    )
                )
            }
            cachedRoutinesList = list
            list
        } catch (e: Exception) {
            Log.e("FocusTimeManager", "Error parsing routines", e)
            emptyList()
        }
    }

    fun saveRoutine(context: Context, routine: FocusRoutine) {
        val clampedRoutine = routine.copy(allowedPackages = routine.allowedPackages.distinct().take(3))
        val current = getRoutines(context).toMutableList()
        val index = current.indexOfFirst { it.id == clampedRoutine.id }
        if (index >= 0) {
            current[index] = clampedRoutine
        } else {
            current.add(clampedRoutine)
        }
        cachedAllowedList = null
        cachedAllowedSet = null
        saveRoutines(context, current)
    }

    fun deleteRoutine(context: Context, id: String) {
        cachedAllowedList = null
        cachedAllowedSet = null
        val current = getRoutines(context).filter { it.id != id }
        saveRoutines(context, current)
    }

    fun toggleRoutine(context: Context, id: String, isEnabled: Boolean) {
        cachedAllowedList = null
        cachedAllowedSet = null
        val current = getRoutines(context).map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveRoutines(context, current)
        checkStatus(context)
    }

    fun setAllRoutines(context: Context, routines: List<FocusRoutine>) {
        cachedAllowedList = null
        cachedAllowedSet = null
        saveRoutines(context, routines)
        checkStatus(context)
    }

    private fun saveRoutines(context: Context, routines: List<FocusRoutine>) {
        val array = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("startHour", r.startHour)
                put("startMinute", r.startMinute)
                put("endHour", r.endHour)
                put("endMinute", r.endMinute)
                put("isEnabled", r.isEnabled)
                val dArr = JSONArray()
                r.daysOfWeek.forEach { dArr.put(it) }
                put("daysOfWeek", dArr)
                val pArr = JSONArray()
                r.allowedPackages.take(3).forEach { pArr.put(it) }
                put("allowedPackages", pArr)
                put("targetPackage", r.targetPackage ?: "")
                put("targetAppName", r.targetAppName ?: "")
            }
            array.put(obj)
        }
        cachedRoutinesList = routines
        getPrefs(context).edit().putString(KEY_ROUTINES_JSON, array.toString()).apply()
        try {
            com.example.LockInApplication.instance.applicationScope.launch {
                CloudSyncManager.syncDataToCloud(context)
            }
        } catch (_: Exception) {}
    }

    /**
     * Preset for quick focus duration selection.
     */
    data class FocusPreset(
        val id: String,
        val label: String,
        val minutes: Int,
        val isDefault: Boolean = false
    )

    fun getPresets(context: Context): List<FocusPreset> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_CUSTOM_PRESETS_JSON, null)
        val defaultPresets = listOf(
            FocusPreset("p15", "15m", 15, true),
            FocusPreset("p30", "30m", 30, true),
            FocusPreset("p60", "1h", 60, true),
            FocusPreset("p120", "2h", 120, true)
        )

        if (jsonStr.isNullOrBlank()) {
            return defaultPresets
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FocusPreset>()
            val oldDefaultIdsToRemove = setOf("p45", "p180", "p240")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                if (id in oldDefaultIdsToRemove) continue
                list.add(
                    FocusPreset(
                        id = id,
                        label = obj.getString("label"),
                        minutes = obj.getInt("minutes"),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
            if (list.isEmpty()) defaultPresets else list
        } catch (_: Exception) {
            defaultPresets
        }
    }

    fun savePreset(context: Context, label: String, minutes: Int) {
        val current = getPresets(context).toMutableList()
        val newPreset = FocusPreset(
            id = "preset_" + System.currentTimeMillis(),
            label = if (label.isNotBlank()) label else if (minutes >= 60 && minutes % 60 == 0) "${minutes / 60}h" else "${minutes}m",
            minutes = minutes,
            isDefault = false
        )
        current.add(newPreset)
        savePresetsList(context, current)
    }

    fun deletePreset(context: Context, id: String) {
        val current = getPresets(context).filter { it.id != id }
        savePresetsList(context, current)
    }

    private fun savePresetsList(context: Context, presets: List<FocusPreset>) {
        val array = JSONArray()
        for (p in presets) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("label", p.label)
                put("minutes", p.minutes)
                put("isDefault", p.isDefault)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_CUSTOM_PRESETS_JSON, array.toString()).apply()
    }

    private fun findActiveRoutine(context: Context): FocusRoutine? {
        // Only device-wide routines (targetPackage null or blank) trigger full phone focus lockdown
        val routines = getRoutines(context).filter { it.isEnabled && it.targetPackage.isNullOrBlank() }
        val now = Calendar.getInstance()
        val currentDay = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        val currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val prevDay = if (currentDay == 1) 7 else currentDay - 1

        for (r in routines) {
            val startMin = r.startHour * 60 + r.startMinute
            val endMin = r.endHour * 60 + r.endMinute

            val isActive = if (startMin <= endMin) {
                r.daysOfWeek.contains(currentDay) && (currentMinutesOfDay in startMin until endMin)
            } else {
                // Spans midnight, e.g. 21:00 to 05:30
                (currentMinutesOfDay >= startMin && r.daysOfWeek.contains(currentDay)) ||
                (currentMinutesOfDay < endMin && r.daysOfWeek.contains(prevDay))
            }

            if (isActive) return r
        }
        return null
    }

    /**
     * Checks if a specific app is currently within an active scheduled blocking routine.
     */
    fun getActiveAppRoutine(context: Context, packageName: String?): FocusRoutine? {
        if (packageName.isNullOrBlank()) return null
        val routines = getRoutines(context).filter { it.isEnabled && it.targetPackage == packageName }
        if (routines.isEmpty()) return null

        val now = Calendar.getInstance()
        val currentDay = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        val currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val prevDay = if (currentDay == 1) 7 else currentDay - 1

        for (r in routines) {
            val startMin = r.startHour * 60 + r.startMinute
            val endMin = r.endHour * 60 + r.endMinute

            val isActive = if (startMin <= endMin) {
                r.daysOfWeek.contains(currentDay) && (currentMinutesOfDay in startMin until endMin)
            } else {
                (currentMinutesOfDay >= startMin && r.daysOfWeek.contains(currentDay)) ||
                (currentMinutesOfDay < endMin && r.daysOfWeek.contains(prevDay))
            }

            if (isActive) return r
        }
        return null
    }

    fun isAppBlockedByRoutine(context: Context, packageName: String?): Boolean {
        return getActiveAppRoutine(context, packageName) != null
    }

    fun getRoutinesForApp(context: Context, packageName: String): List<FocusRoutine> {
        return getRoutines(context).filter { it.targetPackage == packageName }
    }

    fun getAppRoutines(context: Context): List<FocusRoutine> {
        return getRoutines(context).filter { !it.targetPackage.isNullOrBlank() }
    }

    fun getDeviceRoutines(context: Context): List<FocusRoutine> {
        return getRoutines(context).filter { it.targetPackage.isNullOrBlank() }
    }

    fun getPackagesWithRoutines(context: Context): List<String> {
        return getAppRoutines(context).mapNotNull { it.targetPackage }.distinct()
    }

    private fun calculateRoutineRemainingSeconds(routine: FocusRoutine): Long {
        val now = Calendar.getInstance()
        val currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val currentSecOfMin = now.get(Calendar.SECOND)

        val endMin = routine.endHour * 60 + routine.endMinute
        val minutesRemaining = if (currentMinutesOfDay < endMin) {
            endMin - currentMinutesOfDay
        } else {
            (24 * 60 - currentMinutesOfDay) + endMin
        }
        return (minutesRemaining * 60L - currentSecOfMin).coerceAtLeast(0L)
    }

    /**
     * Checks whether an app is a game to strictly enforce user's requirement:
     * "O usuário não pode escolher os apps da categoria 'jogos' para entrarem na exceção."
     */
    fun isGameApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                    return true
                }
            }
            if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return true
            }

            val label = appInfo.loadLabel(pm).toString().lowercase()
            val pkg = packageName.lowercase()
            val gameKeywords = listOf(
                "game", "jogo", "craft", "clash", "candy", "subway",
                "roblox", "brawl", "pokemon", "supercell", "pubg",
                "freefire", "genshin", "fifa", "racing", "runner", "puzzle",
                "playrix", "rovio", "gameloft", "blizzard"
            )
            gameKeywords.any { pkg.contains(it) || label.contains(it) }
        } catch (_: Exception) {
            false
        }
    }
}
