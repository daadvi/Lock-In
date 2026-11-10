package com.example.util

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.service.DeviceAdminManager
import com.example.service.LockInAccessibilityService
import com.example.service.LockScreenPermissionHelper

enum class AppPermission(val essential: Boolean = false) {
    ACCESSIBILITY(true), USAGE(true), OVERLAY(true), NOTIFICATIONS, BATTERY, DEVICE_ADMIN, LOCK_SCREEN
}

enum class PermissionStatus { GRANTED, PENDING, UNKNOWN }

data class PermissionSnapshot(
    val statuses: Map<AppPermission, PermissionStatus>,
    val restrictedSettingsApplicable: Boolean
) {
    fun isGranted(permission: AppPermission) = statuses[permission] == PermissionStatus.GRANTED
    val nextPending: AppPermission? get() = statuses.keys.firstOrNull { !isGranted(it) }
    val missingEssential: List<AppPermission> get() = statuses.keys.filter { it.essential && !isGranted(it) }
    val grantedCount: Int get() = statuses.values.count { it == PermissionStatus.GRANTED }
}

/** Reads run on IO; navigation runs on the main thread following a user tap. */
interface PermissionAccess {
    fun read(): PermissionSnapshot
    fun open(permission: AppPermission): Boolean
    fun openAppDetails(): Boolean
}

class AndroidPermissionAccess(private val context: Context) : PermissionAccess {
    override fun read(): PermissionSnapshot {
        val statuses = linkedMapOf<AppPermission, PermissionStatus>()
        fun check(permission: AppPermission, granted: () -> Boolean) {
            statuses[permission] = try {
                if (granted()) PermissionStatus.GRANTED else PermissionStatus.PENDING
            } catch (_: Exception) {
                PermissionStatus.UNKNOWN
            }
        }
        check(AppPermission.ACCESSIBILITY) { isAccessibilityEnabled(context) }
        check(AppPermission.USAGE) {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
                AppOpsManager.MODE_ALLOWED
        }
        check(AppPermission.OVERLAY) { Settings.canDrawOverlays(context) }
        check(AppPermission.NOTIFICATIONS) {
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        check(AppPermission.BATTERY) {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        }
        check(AppPermission.DEVICE_ADMIN) { DeviceAdminManager.isDeviceAdminActive(context) }
        // Before Android 14 there is no separate full-screen-intent access to grant.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            check(AppPermission.LOCK_SCREEN) { LockScreenPermissionHelper.hasLockScreenPermission(context) }
        }
        return PermissionSnapshot(statuses, RestrictedSettingsHelper.isRestrictedSettingsApplicable())
    }

    override fun open(permission: AppPermission): Boolean {
        if (permission == AppPermission.DEVICE_ADMIN) {
            return DeviceAdminManager.requestDeviceAdmin(context)
        }
        val packageUri = Uri.fromParts("package", context.packageName, null)
        val intents = when (permission) {
            AppPermission.ACCESSIBILITY -> (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) listOf(
                // Supported by AOSP Settings but not part of the public Settings constants.
                Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
                    putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(context, LockInAccessibilityService::class.java).flattenToString())
                }
            ) else emptyList()) + Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            AppPermission.USAGE -> listOf(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri),
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            )
            AppPermission.OVERLAY -> listOf(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri),
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            )
            AppPermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                listOf(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            } else emptyList()
            AppPermission.BATTERY -> listOf(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
            AppPermission.LOCK_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
            } else emptyList()
            AppPermission.DEVICE_ADMIN -> emptyList()
        }
        return openFirstAvailable(intents + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }

    override fun openAppDetails(): Boolean = openFirstAvailable(listOf(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
    ))

    private fun openFirstAvailable(intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                if (DeviceAdminManager.findActivity(context) == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: android.content.ActivityNotFoundException) {
                // Manufacturer-specific settings may not implement the direct screen.
            } catch (_: SecurityException) {
                // Try the public settings list or app details instead.
            }
        }
        return false
    }

    companion object {
        fun isAccessibilityEnabled(context: Context): Boolean {
            if (Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) return false
            val expected = ComponentName(context, LockInAccessibilityService::class.java)
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return enabled.orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
        }
    }
}
