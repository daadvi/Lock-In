package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.BlockedActivity
import com.example.LockInApplication
import com.example.data.local.RestrictedAppEntity
import kotlinx.coroutines.launch

/**
 * Real-time Antivirus/Guardian style receiver:
 * Listens for ACTION_PACKAGE_ADDED and checks whether the newly installed package
 * matches any item in the Play Store / Installation Blacklist (fuzzy, alias, or package).
 * If blacklisted, immediately locks the app, alerts the user, and triggers uninstallation!
 */
class PackageAddedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) {
            return
        }

        val pkgName = intent.data?.schemeSpecificPart ?: intent.data?.encodedSchemeSpecificPart ?: return
        if (pkgName == context.packageName) return // Ignore self

        Log.d("PackageAddedReceiver", "New package installed/updated: $pkgName")

        val matchedBlacklist = PlayStoreBlacklistManager.isAppOrPackageBlacklisted(context, pkgName)
        if (matchedBlacklist != null) {
            Log.w("PackageAddedReceiver", "INTERCEPTED BLACKLISTED APP INSTALLATION FINISHED: $pkgName (matched '$matchedBlacklist')")

            // Immediately dismiss/close any attempt to foreground the newly installed blacklisted app
            try {
                LockInAccessibilityService.closeAndDismissApp(context, pkgName)
            } catch (_: Exception) {}

            // 1. Immediately launch BlockedActivity pop-up with uninstall-only mode
            val blockIntent = Intent(context, BlockedActivity::class.java).apply {
                putExtra(BlockedActivity.EXTRA_BLOCKED_TARGET, matchedBlacklist)
                putExtra(BlockedActivity.EXTRA_APP_NAME, matchedBlacklist)
                putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, pkgName)
                putExtra(BlockedActivity.EXTRA_PROHIBITED_PACKAGE, pkgName)
                putExtra(
                    BlockedActivity.EXTRA_BLOCKED_REASON,
                    "A instalação de '$matchedBlacklist' foi finalizada, mas este aplicativo está na sua lista de bloqueio de instalação. Todo e qualquer acesso está totalmente bloqueado.\n\nNão é possível remover da lista nem desbloquear. A única ação disponível é desinstalar o aplicativo no botão abaixo."
                )
                putExtra(BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            try {
                com.example.util.XiaomiCompatibilityHelper.safelyStartActivity(context, blockIntent)
            } catch (e: Exception) {
                Log.e("PackageAddedReceiver", "Failed to launch BlockedActivity: ${e.message}")
            }

            try {
                Toast.makeText(
                    context,
                    "LOCK IN: '$matchedBlacklist' está na lista de bloqueio de instalação! Acesso bloqueado.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {}
        }
    }
}
