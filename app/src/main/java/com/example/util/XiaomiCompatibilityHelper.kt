package com.example.util

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import java.lang.reflect.Method

/**
 * Helper to ensure full compatibility with Xiaomi, Redmi, and POCO devices running MIUI / HyperOS.
 *
 * Xiaomi phones require specific permissions that standard Android does not enforce:
 * 1. Autostart (Início Automático) - to prevent MIUI from killing background services.
 * 2. Display pop-up windows while running in the background (Exibir janelas pop-up ao executar em segundo plano) -
 *    essential for BlockedActivity and UninstallProtectionActivity to launch from background.
 * 3. No battery restrictions (Economia de bateria: Sem restrições) - to prevent powerkeeper from terminating timers.
 * 4. Lock in Recent Apps (Travar no menu de aplicativos recentes com cadeado) - prevents task cleaner from killing service.
 */
object XiaomiCompatibilityHelper {

    private const val TAG = "XiaomiCompat"
    private const val XIAOMI_FALLBACK_CHANNEL_ID = "lockin_screen_popup_channel"

    /**
     * Checks if the device is manufactured by Xiaomi, Redmi, POCO, or running MIUI / HyperOS.
     */
    fun isXiaomiOrRedmiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        val product = Build.PRODUCT?.lowercase() ?: ""
        val fingerprint = Build.FINGERPRINT?.lowercase() ?: ""

        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ||
            product.contains("xiaomi") || product.contains("redmi") || product.contains("poco") ||
            fingerprint.contains("xiaomi") || fingerprint.contains("redmi") || fingerprint.contains("poco")
        ) {
            return true
        }

        // Check MIUI and HyperOS system properties via reflection
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemPropertiesClass.getMethod("get", String::class.java)
            val miuiVersion = getMethod.invoke(null, "ro.miui.ui.version.name") as? String
            val miuiCode = getMethod.invoke(null, "ro.miui.ui.version.code") as? String
            val hyperOs = getMethod.invoke(null, "ro.miui.internal.storage") as? String
            if (!miuiVersion.isNullOrEmpty() || !miuiCode.isNullOrEmpty() || !hyperOs.isNullOrEmpty()) {
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    /**
     * Returns detected MIUI or HyperOS version string if available.
     */
    fun getMiuiOrHyperOsVersion(): String? {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemPropertiesClass.getMethod("get", String::class.java)
            val miuiName = getMethod.invoke(null, "ro.miui.ui.version.name") as? String
            val miuiCode = getMethod.invoke(null, "ro.miui.ui.version.code") as? String
            if (!miuiName.isNullOrEmpty()) {
                return if (!miuiCode.isNullOrEmpty()) "$miuiName ($miuiCode)" else miuiName
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Checks whether battery optimization is disabled for this app ("Sem restrições").
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if MIUI background popup permission (op 10021) is granted via AppOpsManager.
     */
    fun hasBackgroundPopupPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return true
            // MIUI Op 10021 = OP_BACKGROUND_START_ACTIVITY
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(appOps, 10021, Process.myUid(), context.packageName) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            // If reflection fails or non-MIUI, assume allowed or unverifiable
            true
        }
    }

    /**
     * Opens MIUI Autostart (Início Automático) settings.
     */
    fun openAutostartSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.securityadd.startup.StartupNormalAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
            ).putExtra("extra_pkgname", context.packageName)
        )

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }

        return openAppDetailsSettings(context)
    }

    /**
     * Opens MIUI "Exibir janelas pop-up ao executar em segundo plano" (Background Popups) permission settings.
     */
    fun openPopupInBackgroundSettings(context: Context): Boolean {
        val packageName = context.packageName
        val intents = listOf(
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            },
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            },
            Intent().apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            },
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", packageName)
            },
            Intent("miui.intent.action.APP_PERM_EDITOR_PRIVATE").apply {
                putExtra("extra_pkgname", packageName)
            }
        )

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }

        return openAppDetailsSettings(context)
    }

    /**
     * Opens MIUI Battery Saver ("Sem restrições") config for this app.
     */
    fun openBatterySaverSettings(context: Context): Boolean {
        val packageName = context.packageName
        val intents = listOf(
            Intent().apply {
                setComponent(
                    ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                    )
                )
                putExtra("package_name", packageName)
                putExtra("extra_pkgname", packageName)
            },
            Intent().apply {
                setComponent(
                    ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
                    )
                )
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            } else null,
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        ).filterNotNull()

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }

        return openAppDetailsSettings(context)
    }

    /**
     * Opens MIUI Security Center main dashboard.
     */
    fun openMiuiSecurityCenter(context: Context): Boolean {
        return try {
            val intent = Intent().setComponent(
                ComponentName("com.miui.securitycenter", "com.miui.securityscan.MainActivity")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    /**
     * Safely launches an activity intent. On Xiaomi devices where MIUI may block background activity
     * launches, catches failures and posts a high-priority FullScreenIntent notification fallback.
     */
    fun safelyStartActivity(context: Context, intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            if (isXiaomiOrRedmiDevice() && !hasBackgroundPopupPermission(context)) {
                // MIUI silently swallows startActivity without throwing! Post FullScreenIntent fallback immediately!
                launchWithNotificationFallback(context, intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity failed on MIUI/Android, triggering FullScreenIntent fallback: ${e.message}")
            launchWithNotificationFallback(context, intent)
        }
    }

    /**
     * Triggers a high-priority FullScreenIntent notification that displays the activity immediately on screen,
     * bypassing MIUI background activity start restrictions.
     */
    fun launchWithNotificationFallback(context: Context, targetIntent: Intent) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    XIAOMI_FALLBACK_CHANNEL_ID,
                    "Alertas de Bloqueio Instantâneo",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Garante exibição da tela de bloqueio e desinstalação em dispositivos Xiaomi/Redmi"
                    enableVibration(true)
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                999,
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, XIAOMI_FALLBACK_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Lock In - Ação Necessária")
                .setContentText("Toque para abrir a tela de proteção.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Notification fallback failed: ${e.message}")
        }
    }

    private fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details: ${e.message}")
            false
        }
    }
}
