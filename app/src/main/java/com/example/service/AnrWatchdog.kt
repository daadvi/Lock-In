package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.MainActivity

/**
 * Detecta travamento da main thread (padrão de ANR) por batimento perdido e reabre o app
 * 5 s depois da detecção. A abertura usa a permissão de sobreposição que o Lock In já exige,
 * o que libera start de activity a partir do background.
 */
object AnrWatchdog {
    private const val TAG = "AnrWatchdog"
    private const val BEAT_INTERVAL_MS = 1_000L
    private const val STALL_THRESHOLD_MS = 4_500L
    private const val REOPEN_DELAY_MS = 5_000L
    private const val RECOVERY_COOLDOWN_MS = 120_000L

    @Volatile
    private var started = false

    @Volatile
    private var lastBeat = 0L

    @Volatile
    private var lastRecovery = 0L

    /** Regra pura (testável): a main thread perdeu o batimento por tempo demais. */
    internal fun isStalled(lastBeatMs: Long, nowMs: Long, thresholdMs: Long = STALL_THRESHOLD_MS): Boolean =
        lastBeatMs > 0L && nowMs - lastBeatMs >= thresholdMs

    fun ensureStarted(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val handler = Handler(Looper.getMainLooper())

        val beat = object : Runnable {
            override fun run() {
                lastBeat = System.currentTimeMillis()
                handler.postDelayed(this, BEAT_INTERVAL_MS)
            }
        }
        handler.post(beat)

        Thread({
            while (true) {
                try {
                    Thread.sleep(BEAT_INTERVAL_MS)
                    val now = System.currentTimeMillis()
                    if (!isStalled(lastBeat, now)) continue
                    if (now - lastRecovery < RECOVERY_COOLDOWN_MS) continue
                    lastRecovery = now
                    Log.w(TAG, "Main thread stalled ${now - lastBeat}ms; reopening in ${REOPEN_DELAY_MS}ms")
                    Thread.sleep(REOPEN_DELAY_MS)
                    reopen(app)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }
        }, "anr-watchdog").apply { isDaemon = true }.start()
    }

    private fun reopen(app: Context) {
        try {
            val intent = Intent(app, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            com.example.util.XiaomiCompatibilityHelper.safelyStartActivity(app, intent)
            Log.w(TAG, "Recovery launch requested")
        } catch (e: Exception) {
            Log.e(TAG, "Recovery launch failed", e)
        }
    }
}
