package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object LockScreenPermissionHelper {

    /**
     * Checks if the app has permission to appear on the lock screen / full screen intent.
     */
    fun hasLockScreenPermission(context: Context): Boolean {
        // On Android 14+ (API 34+), check if full screen intent is allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && !nm.canUseFullScreenIntent()) {
                return false
            }
        }
        return true
    }

    /**
     * Opens the optimal system screen for the user to grant permission to appear on the lock screen.
     * Handles MIUI / Xiaomi / HyperOS ("Exibir na tela de bloqueio"), Android 14+ Full Screen Intent,
     * and general Android Application Details.
     */
    fun requestLockScreenPermission(context: Context) {
        val packageName = context.packageName

        // 1. Try MIUI specific intent (Exibir na tela de bloqueio / Mostrar sobre tela de bloqueio)
        if (isMiuiDevice()) {
            try {
                val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(miuiIntent)
                return
            } catch (e: Exception) {
                Log.d("LockScreenPermission", "MIUI specific intent failed, trying fallback: ${e.message}")
            }
        }

        // 2. Android 14+ (API 34+) direct settings for USE_FULL_SCREEN_INTENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val fullScreenIntent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fullScreenIntent)
                return
            } catch (e: Exception) {
                Log.d("LockScreenPermission", "ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT failed: ${e.message}")
            }
        }

        // 3. Fallback to standard Application Details Settings where user can toggle permissions / lockscreen
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appDetailsIntent)
        } catch (e: Exception) {
            Log.e("LockScreenPermission", "All permission intents failed: ${e.message}")
        }
    }

    private fun isMiuiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
                brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
    }
}
