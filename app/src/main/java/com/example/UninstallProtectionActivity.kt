package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
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
        const val EXTRA_CHALLENGE_MODE = "EXTRA_CHALLENGE_MODE"
        const val EXTRA_TARGET_APP_NAME = "EXTRA_TARGET_APP_NAME"
        const val MODE_UNINSTALL_APP = "UNINSTALL_APP"
        const val MODE_DEACTIVATE_ADMIN = "DEACTIVATE_ADMIN"
        const val MODE_UNBLOCK_INSTALLATION = "UNBLOCK_INSTALLATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            LockInTheme(customTextColor = customColor) {
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
                                DeviceAdminManager.setUninstallAuthorized(this, true)
                                DeviceAdminManager.deactivateDeviceAdmin(this)
                                Toast.makeText(
                                    this,
                                    "Administrador desativado com sucesso.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                            HighFrictionChallengeType.UNINSTALL_LOCK_IN -> {
                                Toast.makeText(
                                    this,
                                    "Proteção liberada. Iniciando desinstalação do Lock In...",
                                    Toast.LENGTH_LONG
                                ).show()
                                DeviceAdminManager.setUninstallAuthorized(this, true)
                                DeviceAdminManager.deactivateDeviceAdmin(this)
                                DeviceAdminManager.triggerSystemUninstall(this)
                                finish()
                            }
                            HighFrictionChallengeType.UNBLOCK_INSTALLATION -> {
                                if (!targetName.isNullOrBlank()) {
                                    com.example.service.PlayStoreBlacklistManager.removeApp(this, targetName)
                                }
                                Toast.makeText(
                                    this,
                                    "Aplicativo liberado do bloqueio de instalação.",
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
}
