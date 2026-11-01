package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * Helper to detect and guide users through "Configurações restritas" ("Restricted settings")
 * on Android 13+ (API 33+) sideloaded apps and Xiaomi / MIUI / HyperOS devices.
 */
object RestrictedSettingsHelper {

    /**
     * Checks whether the current device is running Android 13+ (API 33+)
     * or a Xiaomi/Redmi/POCO device where restricted settings might be enforced.
     */
    fun isRestrictedSettingsApplicable(): Boolean {
        return Build.VERSION.SDK_INT >= 33 || XiaomiCompatibilityHelper.isXiaomiOrRedmiDevice()
    }

    /**
     * Checks if the device is specifically Android 13 or newer.
     */
    fun isAndroid13OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }

    /**
     * Checks if the device is a Xiaomi/Redmi/POCO device.
     */
    fun isXiaomiDevice(): Boolean {
        return XiaomiCompatibilityHelper.isXiaomiOrRedmiDevice()
    }

    /**
     * Opens the App Details / Info screen for Lock In so the user can tap
     * the 3 dots (⋮) in the top-right and select "Permitir configurações restritas".
     */
    fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                Toast.makeText(context, "Não foi possível abrir as configurações do app.", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    /**
     * Opens the system Accessibility settings screen.
     */
    fun openAccessibilitySettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir as configurações de acessibilidade.", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
