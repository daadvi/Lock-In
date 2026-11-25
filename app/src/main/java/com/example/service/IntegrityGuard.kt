package com.example.service

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Detecta quando a protecao foi silenciosamente degradada durante um bloqueio
 * (servico de acessibilidade desligado ou permissao de sobreposicao revogada) e
 * traz a superficie de bloqueio de volta para a frente, para que a burla nao seja
 * silenciosa: o usuario cai novamente na tela de bloqueio e precisa restaurar a
 * protecao para seguir.
 */
object IntegrityGuard {
    private const val TAG = "IntegrityGuard"
    private const val CHECK_INTERVAL_MS = 30_000L
    private const val REOPEN_COOLDOWN_MS = 60_000L

    @Volatile
    private var started = false

    @Volatile
    private var lastReopenEpoch = 0L

    /** Degradado = bloqueio deveria estar ativo, mas o servico de acessibilidade ou o overlay caiu. */
    internal fun isDegraded(blockActive: Boolean, accessibilityOn: Boolean, overlayOn: Boolean): Boolean =
        blockActive && (!accessibilityOn || !overlayOn)

    internal fun shouldReopen(nowMs: Long, lastReopenMs: Long, cooldownMs: Long = REOPEN_COOLDOWN_MS): Boolean =
        nowMs - lastReopenMs >= cooldownMs

    fun ensureStarted(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val thread = Thread({
            while (true) {
                try {
                    checkOnce(app)
                } catch (e: Exception) {
                    Log.w(TAG, "Integrity check failed", e)
                }
                try {
                    Thread.sleep(CHECK_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }
        }, "integrity-guard")
        thread.isDaemon = true
        thread.start()
    }

    private fun checkOnce(context: Context) {
        val focusActive = FocusTimeManager.policySnapshot(context).active
        val lockoutActive = ScreenTimeManager.isLockoutActive(context)
        if (!focusActive && !lockoutActive) return

        val accessibilityOn = isAccessibilityServiceEnabled(context)
        val overlayOn = Settings.canDrawOverlays(context)
        if (!isDegraded(true, accessibilityOn, overlayOn)) return

        val now = System.currentTimeMillis()
        if (!shouldReopen(now, lastReopenEpoch)) return
        lastReopenEpoch = now

        Log.w(TAG, "Protection degraded while blocking (a11y=$accessibilityOn overlay=$overlayOn); pulling the lock back")
        if (focusActive) {
            FocusTimeManager.bringFocusLockToFront(context)
        } else {
            ScreenTimeManager.bringLockoutToFront(context)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = context.packageName + "/" + LockInAccessibilityService::class.java.name
        val enabled = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } catch (_: Exception) {
            null
        } ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
