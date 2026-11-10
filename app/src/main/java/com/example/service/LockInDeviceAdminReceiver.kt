package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class LockInDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "Proteção Contra Desinstalação Lock In ATIVADA com sucesso!",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        if (!DeviceAdminManager.isUninstallAuthorized(context)) {
            LockInAccessibilityService.instance?.showUninstallChallenge(com.example.UninstallProtectionActivity.MODE_DEACTIVATE_ADMIN)
        }
        val passwordRepeats = com.example.LockInApplication.instance.userPreferences.getLockLevel().passwordRepeatCount
        return "ATENÇÃO: A desativação do Lock In exige digitar a senha $passwordRepeats vez${if (passwordRepeats > 1) "es" else ""} e completar as frases de confirmação, conforme seu nível de Lock."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
