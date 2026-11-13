package com.example

import androidx.compose.runtime.getValue
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.addCallback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.example.service.DeviceAdminManager
import com.example.ui.components.HighFrictionChallengeType
import com.example.ui.components.HighFrictionSecurityChallengeFlow
import com.example.ui.theme.LockInTheme

class UninstallProtectionActivity : ComponentActivity() {

    companion object {
        internal const val EXTRA_VERIFIED_OVERLAY = "VERIFIED_UNINSTALL_OVERLAY"
        const val EXTRA_CHALLENGE_MODE = "EXTRA_CHALLENGE_MODE"
        const val EXTRA_TARGET_APP_NAME = "EXTRA_TARGET_APP_NAME"
        const val MODE_UNINSTALL_APP = "UNINSTALL_APP"
        const val MODE_DEACTIVATE_ADMIN = "DEACTIVATE_ADMIN"
        const val MODE_UNBLOCK_INSTALLATION = "UNBLOCK_INSTALLATION"
    }

    private var uninstallInProgress = false
    private var systemUninstallLaunched = false
    private val systemUninstall = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // If we are still installed, close the temporary authorization even on cancellation.
        DeviceAdminManager.setUninstallAuthorized(this, false)
        uninstallInProgress = false
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (consumeVerifiedOverlay(intent)) return
        if (!uninstallInProgress && intent.getStringExtra(EXTRA_CHALLENGE_MODE) !=
            this.intent.getStringExtra(EXTRA_CHALLENGE_MODE)) {
            setIntent(intent)
            recreate()
        }
    }

    private fun consumeVerifiedOverlay(request: Intent): Boolean {
        if (!request.getBooleanExtra(EXTRA_VERIFIED_OVERLAY, false)) return false
        request.removeExtra(EXTRA_VERIFIED_OVERLAY)
        if (!DeviceAdminManager.isUninstallAuthorized(this)) return false
        if (request.getStringExtra(EXTRA_CHALLENGE_MODE) == MODE_DEACTIVATE_ADMIN) {
            deactivateAdminAfterChallenge()
        } else {
            uninstallAfterChallenge()
        }
        return true
    }

    private fun deactivateAdminAfterChallenge() {
        try {
            DeviceAdminManager.setUninstallAuthorized(this, true)
            DeviceAdminManager.deactivateDeviceAdmin(this)
            Toast.makeText(this, "Administrador desativado com sucesso.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            DeviceAdminManager.setUninstallAuthorized(this, false)
            Toast.makeText(this, "O Android não concluiu a desativação. Tente novamente.", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun uninstallAfterChallenge() {
        if (uninstallInProgress) return
        uninstallInProgress = true
        DeviceAdminManager.setUninstallAuthorized(this, true)
        lifecycleScope.launch {
            try {
                DeviceAdminManager.deactivateDeviceAdmin(this@UninstallProtectionActivity)
                // Removing active admin can complete asynchronously in the system process.
                var attempts = 0
                while (DeviceAdminManager.isDeviceAdminActive(this@UninstallProtectionActivity) && attempts++ < 30) {
                    delay(100)
                }
                check(!DeviceAdminManager.isDeviceAdminActive(this@UninstallProtectionActivity))
                systemUninstallLaunched = true
                systemUninstall.launch(DeviceAdminManager.systemUninstallIntent(this@UninstallProtectionActivity))
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (!systemUninstallLaunched) DeviceAdminManager.setUninstallAuthorized(this@UninstallProtectionActivity, false)
                throw e
            } catch (e: Exception) {
                DeviceAdminManager.setUninstallAuthorized(this@UninstallProtectionActivity, false)
                uninstallInProgress = false
                Toast.makeText(this@UninstallProtectionActivity,
                    "Não foi possível abrir a desinstalação. Tente novamente pelas configurações do Android.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        uninstallInProgress = savedInstanceState?.getBoolean("uninstall_in_progress") ?: false
        systemUninstallLaunched = uninstallInProgress
        if (consumeVerifiedOverlay(intent)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        window.decorView.filterTouchesWhenObscured = true

        // Strict back press handling: pressing back cancels action and sends user home
        onBackPressedDispatcher.addCallback(this) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }

        val prefs = LockInApplication.instance.userPreferences
        val trustPassword = prefs.getTrustPassword()
        val customColor = try {
            Color(prefs.textColorHex.value.toColorInt())
        } catch (_: Exception) {
            Color(0xFFFF5500)
        }

        val modeStr = intent.getStringExtra(EXTRA_CHALLENGE_MODE) ?: MODE_UNINSTALL_APP
        val targetName = intent.getStringExtra(EXTRA_TARGET_APP_NAME)
        val challengeType = when (modeStr) {
            MODE_DEACTIVATE_ADMIN -> HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN
            MODE_UNBLOCK_INSTALLATION -> HighFrictionChallengeType.UNBLOCK_INSTALLATION
            else -> HighFrictionChallengeType.UNINSTALL_LOCK_IN
        }

        setContent {
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle()
            LockInTheme(customTextColor = customColor, themeMode = themeMode) {
                HighFrictionSecurityChallengeFlow(
                    challengeType = challengeType,
                    targetItemName = targetName,
                    correctPassword = trustPassword,
                    accentColor = customColor,
                    onCancel = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onSuccess = {
                        when (challengeType) {
                            HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN -> {
                                deactivateAdminAfterChallenge()
                            }
                            HighFrictionChallengeType.UNINSTALL_LOCK_IN -> {
                                Toast.makeText(
                                    this,
                                    "Proteção liberada. Iniciando desinstalação do Lock In...",
                                    Toast.LENGTH_LONG
                                ).show()
                                uninstallAfterChallenge()
                            }
                            HighFrictionChallengeType.UNBLOCK_INSTALLATION -> {
                                val removed = !targetName.isNullOrBlank() &&
                                    com.example.service.PlayStoreBlacklistManager.removeApp(this, targetName)
                                Toast.makeText(
                                    this,
                                    if (removed) "Aplicativo liberado do bloqueio de instalação."
                                    else "Bloqueio absoluto ativo: não é possível remover apps da lista.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("uninstall_in_progress", systemUninstallLaunched)
        super.onSaveInstanceState(outState)
    }

}
