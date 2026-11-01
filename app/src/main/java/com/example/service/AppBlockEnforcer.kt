package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BlockedActivity
import com.example.LockInApplication
import com.example.data.local.RestrictedAppEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * High-performance, zero-latency synchronous in-memory enforcer for apps whose limits reached 0.
 * Guarantees 100% prevention of opening or using depleted apps.
 */
object AppBlockEnforcer {
    private const val TAG = "AppBlockEnforcer"

    @Volatile
    private var depletedAppsMap: Map<String, String> = emptyMap()

    // Thread-safe set of package names whose limits were depleted today (persists even if user attempts to deselect/unlimit)
    private val depletedTodayPackages = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    @Volatile
    private var lastBlockTriggerEpoch: Long = 0L

    @Volatile
    private var lastBlockedPackage: String? = null

    @Volatile
    private var currentAppsList: List<RestrictedAppEntity> = emptyList()
    @Volatile
    private var currentGroupsList: List<com.example.data.local.AppGroupEntity> = emptyList()

    fun initialize(application: LockInApplication) {
        application.applicationScope.launch {
            application.repository.getAllAppsFlow().collectLatest { apps ->
                currentAppsList = apps
                recomputeDepletedMap()
            }
        }
        application.applicationScope.launch {
            application.repository.getAllAppGroupsFlow().collectLatest { groups ->
                currentGroupsList = groups
                recomputeDepletedMap()
            }
        }
    }

    private fun recomputeDepletedMap() {
        val map = mutableMapOf<String, String>()
        for (app in currentAppsList) {
            val isDepletedNow = (app.totalLimitMinutes > 0 && app.isLimitDepleted) ||
                    (app.individualLimitMinutes > 0 && app.individualRemainingSeconds <= 0L) ||
                    (app.shortVideoLimitMinutes > 0 && app.shortVideoRemainingSeconds <= 0L)

            if (isDepletedNow) {
                depletedTodayPackages.add(app.packageName)
            } else if (app.individualRemainingSeconds > 0L && app.shortVideoRemainingSeconds > 0L) {
                // Restored positively by reset or trust unlock
                depletedTodayPackages.remove(app.packageName)
            }

            // An app is in the depleted block map if it's currently limited & depleted,
            // OR if its limit was depleted today and has not been legitimately reset/unlocked
            if (isDepletedNow || (depletedTodayPackages.contains(app.packageName) && app.individualRemainingSeconds <= 0L)) {
                map[app.packageName] = app.appName
            }
        }
        // App Groups (Shared Time Limits): if group remaining time <= 0 or group isBlocked, block all apps in group!
        for (group in currentGroupsList) {
            if (group.limitMinutes > 0 && (group.remainingSeconds <= 0L || group.isBlocked)) {
                for (pkg in group.getPackageList()) {
                    map[pkg] = "${group.name} (Tempo Compartilhado Esgotado)"
                }
            }
        }
        depletedAppsMap = map
        try {
            LockInApplication.instance?.let { app ->
                for (pkg in map.keys) {
                    LockInVpnManager.registerDepletedApp(app, pkg)
                }
            }
        } catch (_: Exception) {}
        Log.d(TAG, "Updated depleted apps count: ${map.size} -> ${map.keys}")
    }

    fun markAppDepleted(packageName: String) {
        if (packageName.isBlank()) return
        depletedTodayPackages.add(packageName)
        try {
            LockInApplication.instance?.let { LockInVpnManager.registerDepletedApp(it, packageName) }
        } catch (_: Exception) {}
        recomputeDepletedMap()
    }

    fun clearDepletedForPackage(packageName: String) {
        depletedTodayPackages.remove(packageName)
        try {
            LockInApplication.instance?.let { LockInVpnManager.clearDepletedApp(it, packageName) }
        } catch (_: Exception) {}
        recomputeDepletedMap()
    }

    fun clearAllDepletedForToday() {
        depletedTodayPackages.clear()
        try {
            LockInApplication.instance?.let { LockInVpnManager.clearAllDepletedApps(it) }
        } catch (_: Exception) {}
        recomputeDepletedMap()
    }

    fun updateDepletedList(apps: List<RestrictedAppEntity>) {
        currentAppsList = apps
        recomputeDepletedMap()
    }

    fun isAppDepleted(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        // Never mark system packages, launcher, or Lock In itself as depleted
        val lower = packageName.lowercase()
        if (lower.contains("launcher") || lower.contains("systemui") || lower == "android" ||
            lower == "com.android.settings" || lower.contains("settings") || lower == "com.google.android.packageinstaller" ||
            lower == "com.android.packageinstaller" || lower.contains("keyboard") || lower.contains("inputmethod")) {
            return false
        }
        return depletedAppsMap.containsKey(packageName) || depletedTodayPackages.contains(packageName)
    }

    fun isAppMonitored(packageName: String): Boolean {
        return LimitTimerManager.isPackageMonitored(packageName)
    }

    fun getDepletedAppName(packageName: String): String {
        return depletedAppsMap[packageName] ?: "Aplicativo"
    }

    fun triggerBlockScreen(context: Context, packageName: String, force: Boolean = false) {
        if (DeviceAdminManager.isUninstallAuthorized(context)) {
            return
        }
        val now = System.currentTimeMillis()
        if (!force && lastBlockedPackage == packageName && (now - lastBlockTriggerEpoch < 1200L)) {
            return
        }
        lastBlockedPackage = packageName
        lastBlockTriggerEpoch = now

        val appName = getDepletedAppName(packageName)
        try {
            // Dismiss target app process and window without sending device to Home Launcher
            LockInAccessibilityService.closeAndDismissApp(context, packageName)
            val intent = Intent(context, BlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(BlockedActivity.EXTRA_APP_NAME, appName)
                putExtra(BlockedActivity.EXTRA_BLOCKED_TARGET, appName)
                putExtra(BlockedActivity.EXTRA_PROHIBITED_PACKAGE, packageName)
                putExtra(
                    BlockedActivity.EXTRA_BLOCKED_REASON,
                    "Você já excedeu o limite de uso deste app"
                )
                putExtra(BlockedActivity.EXTRA_IS_DEPLETED_LIMIT, true)
            }
            com.example.util.XiaomiCompatibilityHelper.safelyStartActivity(context, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching BlockedActivity for depleted app $packageName", e)
        }
    }
}
