package com.example.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.BlockedActivity

object InstallationBlockerHelper {
    private const val TAG = "InstallationBlocker"

    const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
    const val EXTRA_APP_NAME = "EXTRA_APP_NAME"
    const val EXTRA_BLOCKED_REASON = "EXTRA_BLOCKED_REASON"

    const val DEFAULT_APP_NAME = "Instalação de Apps / Play Store"
    const val DEFAULT_BLOCKED_REASON = "Acesso bloqueado pelo Lock In."

    val BLOCKED_INSTALLER_PACKAGES = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    @Volatile
    private var lastBlockTriggerEpoch: Long = 0L

    /**
     * Checks if a given package name corresponds to a known direct app package installer.
     * Note: com.android.vending (Google Play Store) is NOT an installer package.
     */
    fun isInstallerPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return BLOCKED_INSTALLER_PACKAGES.contains(packageName) ||
                packageName == "com.android.packageinstaller" ||
                packageName == "com.google.android.packageinstaller"
    }

    /**
     * Identifies the foreground application package in the last 5 seconds using UsageStatsManager.
     */
    fun getForegroundPackageLast5Seconds(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val beginTime = now - 5000L // 5 seconds window

        // 1. First attempt: Query UsageEvents for accurate foreground events
        try {
            val events = usm.queryEvents(beginTime, now)
            val event = UsageEvents.Event()
            var latestForegroundPkg: String? = null
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }

                if (isForeground && event.timeStamp > latestTimestamp) {
                    latestForegroundPkg = event.packageName
                    latestTimestamp = event.timeStamp
                }
            }

            if (!latestForegroundPkg.isNullOrBlank()) {
                return latestForegroundPkg
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying usage events: ${e.message}")
        }

        // 2. Fallback attempt: Query recent usage stats in the 5s interval
        try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, now)
            val mostRecent = stats?.filter { it.lastTimeUsed >= beginTime }
                ?.maxByOrNull { it.lastTimeUsed }
            if (mostRecent != null) {
                return mostRecent.packageName
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying usage stats: ${e.message}")
        }

        return null
    }

    /**
     * Builds and launches the BlockedActivity intent targeting the detected installer package.
     */
    fun triggerBlockedActivity(
        context: Context,
        detectedPackage: String,
        appName: String = DEFAULT_APP_NAME,
        blockedReason: String = DEFAULT_BLOCKED_REASON
    ) {
        val now = System.currentTimeMillis()
        // Debounce block activity launches by 1.5 seconds to prevent intent flooding
        if (now - lastBlockTriggerEpoch < 1500L) {
            return
        }
        lastBlockTriggerEpoch = now

        val service = LockInAccessibilityService.instance
        if (service != null && service.isUninstallDialogActive()) {
            return
        }
        if (LockInAccessibilityService.isAnyUninstallActive()) {
            return
        }

        try {
            LockInAccessibilityService.closeActiveAppWindow()
            val intent = Intent(context, BlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(EXTRA_PACKAGE_NAME, detectedPackage)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_BLOCKED_REASON, blockedReason)
                // Also supply legacy extras for complete backward compatibility
                putExtra(BlockedActivity.EXTRA_PROHIBITED_PACKAGE, detectedPackage)
                putExtra(BlockedActivity.EXTRA_BLOCKED_TARGET, appName)
                putExtra(BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
            }
            com.example.util.XiaomiCompatibilityHelper.safelyStartActivity(context, intent)
            Log.i(TAG, "Blocked installation/Play Store detected: $detectedPackage. Launched BlockedActivity.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch BlockedActivity for package $detectedPackage", e)
        }
    }

    /**
     * Checks if the foreground application in the last 5 seconds (or supplied package)
     * belongs to a known app installer or Google Play Store.
     * If detected, immediately launches BlockedActivity.
     *
     * @return true if an installer package was detected and blocked, false otherwise.
     */
    fun checkAndBlockInstallation(context: Context, detectedPackage: String? = null): Boolean {
        if (DeviceAdminManager.isUninstallAuthorized(context)) {
            return false
        }
        val service = LockInAccessibilityService.instance
        if (service != null && service.isUninstallDialogActive()) {
            return false
        }
        if (LockInAccessibilityService.isAnyUninstallActive()) {
            return false
        }
        val targetPackage = detectedPackage ?: getForegroundPackageLast5Seconds(context) ?: return false
        if (FriendsPartyManager.isPackageTemporarilyAllowed(targetPackage)) {
            return false
        }
        val matchedBlacklist = PlayStoreBlacklistManager.isAppOrPackageBlacklisted(context, targetPackage)
        if (matchedBlacklist != null) {
            if (service != null && service.isUninstallDialogActive()) {
                return false
            }
            triggerBlockedActivity(
                context = context,
                detectedPackage = targetPackage,
                appName = matchedBlacklist,
                blockedReason = "A instalação de '$matchedBlacklist' está bloqueada pela sua lista de bloqueio do Lock In."
            )
            return true
        }
        return false
    }
}
