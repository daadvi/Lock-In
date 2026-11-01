package com.example.service

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object DeviceAdminManager {

    const val REQUEST_CODE_DEVICE_ADMIN = 10091

    @Volatile
    var lastAdminRequestEpoch: Long = 0L

    private const val PREFS_ADMIN = "lock_in_device_admin_prefs"
    private const val KEY_UNINSTALL_AUTHORIZED = "key_uninstall_authorized"

    @Volatile
    var isUninstallAuthorized: Boolean = false

    fun setUninstallAuthorized(context: Context, authorized: Boolean) {
        isUninstallAuthorized = authorized
        try {
            context.getSharedPreferences(PREFS_ADMIN, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_UNINSTALL_AUTHORIZED, authorized)
                .commit()
        } catch (_: Exception) {}
    }

    fun isUninstallAuthorized(context: Context): Boolean {
        if (isUninstallAuthorized) return true
        return try {
            context.getSharedPreferences(PREFS_ADMIN, Context.MODE_PRIVATE)
                .getBoolean(KEY_UNINSTALL_AUTHORIZED, false)
        } catch (_: Exception) {
            false
        }
    }

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, LockInDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val adminComponent = getAdminComponent(context)
        return try {
            dpm.isAdminActive(adminComponent)
        } catch (e: Exception) {
            false
        }
    }

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun requestDeviceAdmin(context: Context) {
        lastAdminRequestEpoch = System.currentTimeMillis()
        val adminComponent = getAdminComponent(context)

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "A permissão de Administrador do Dispositivo protege o Lock In contra desinstalações impulsivas. O app não poderá ser removido sem passar pelo protocolo de segurança estrito."
            )
        }

        val activity = findActivity(context)

        // Attempt 1: Call startActivityForResult from Activity context.
        // Android requires ACTION_ADD_DEVICE_ADMIN to have a valid calling Activity;
        // calling it without startActivityForResult or with FLAG_ACTIVITY_NEW_TASK
        // causes Settings to immediately finish() without showing the screen.
        if (activity != null) {
            try {
                activity.startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN)
                return
            } catch (e: Exception) {
                Log.w("DeviceAdminManager", "Activity.startActivityForResult failed: ${e.message}")
            }
        }

        // Attempt 2: Start with FLAG_ACTIVITY_NEW_TASK from context
        try {
            val taskIntent = Intent(intent).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(taskIntent)
            return
        } catch (e: Exception) {
            Log.w("DeviceAdminManager", "Context.startActivity with ACTION_ADD_DEVICE_ADMIN failed: ${e.message}")
        }

        // Attempt 3: Open Device Admin Settings list directly in System Settings
        openDeviceAdminSettings(context)
    }

    fun openDeviceAdminSettings(context: Context) {
        lastAdminRequestEpoch = System.currentTimeMillis()
        val actions = listOf(
            "android.settings.DEVICE_ADMIN_SETTINGS",
            "android.app.action.DEVICE_ADMIN_SETTINGS",
            Settings.ACTION_SECURITY_SETTINGS,
            Settings.ACTION_SETTINGS
        )

        for (action in actions) {
            try {
                val intent = Intent(action).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "Encontre 'Lock in' e ative como Administrador do Dispositivo",
                    Toast.LENGTH_LONG
                ).show()
                return
            } catch (_: Exception) {
                // Try next
            }
        }
    }

    fun deactivateDeviceAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
        val adminComponent = getAdminComponent(context)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.removeActiveAdmin(adminComponent)
        }
    }

    fun triggerSystemUninstall(context: Context) {
        // First ensure device admin is deactivated so system allows uninstall
        deactivateDeviceAdmin(context)
        setUninstallAuthorized(context, true)

        try {
            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(uninstallIntent)
        } catch (e: Exception) {
            Log.e("DeviceAdminManager", "Failed to launch ACTION_DELETE intent", e)
        }
    }
}

