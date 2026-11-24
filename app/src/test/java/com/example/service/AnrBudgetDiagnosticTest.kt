package com.example.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FocusRoutine
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Diagnóstico de ANR: cada cenário mede o custo de um caminho que roda na MAIN THREAD
 * em produção (evento de acessibilidade, composição da tela de bloqueio, widgets).
 * Cada teste é executado DUAS vezes (repeat(2)) para expor variância/regressão.
 *
 * Limites são folgados de propósito: o objetivo é flagrar custo patológico
 * (I/O, IPC ou tree walk) e não medir microbenchmark.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnrBudgetDiagnosticTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun cleanup() {
        FocusTimeManager.setAllRoutines(context, emptyList())
        FocusTimeManager.setEmergencyAllowedPackages(context, emptyList())
        FocusTimeManager.stopFocusSession(context)
    }

    private fun activeRoutine(allowed: List<String> = emptyList()): FocusRoutine {
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = ((minuteOfDay - 5) + 1440) % 1440
        val end = (minuteOfDay + 5) % 1440
        return FocusRoutine(
            name = "Rotina ANR",
            startHour = start / 60, startMinute = start % 60,
            endHour = end / 60, endMinute = end % 60,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            allowedPackages = allowed
        )
    }

    /** Roda o corpo duas vezes e falha se qualquer execução estourar o orçamento. */
    private fun twice(label: String, budgetMs: Long, body: () -> Unit) {
        repeat(2) { round ->
            val started = System.nanoTime()
            body()
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue(
                "ANR budget excedido em '$label' (execução ${round + 1}): ${elapsedMs}ms > ${budgetMs}ms",
                elapsedMs <= budgetMs
            )
        }
    }

    @Test
    fun focusCascadeLookupStaysCheap() {
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine()))
        twice("cascata de foco (isFocusTimeActive + isPackageAllowed) x500", 3000) {
            repeat(500) {
                FocusTimeManager.isFocusTimeActive(context)
                FocusTimeManager.isPackageAllowed(context, "com.instagram.android")
            }
        }
    }

    @Test
    fun allowedPackagesLookupStaysCheap() {
        FocusTimeManager.setAllRoutines(
            context,
            listOf(activeRoutine(listOf("com.example.a", "com.example.b", "com.example.c")))
        )
        twice("getAllowedPackages x500", 3000) {
            repeat(500) { FocusTimeManager.getAllowedPackages(context) }
        }
    }

    @Test
    fun blacklistCheckIsCached() {
        twice("PlayStoreBlacklistManager.isAppOrPackageBlacklisted x500", 3000) {
            repeat(500) {
                PlayStoreBlacklistManager.isAppOrPackageBlacklisted(context, "com.instagram.android")
            }
        }
    }

    @Test
    fun appDepletedLookupIsMemoryOnly() {
        twice("AppBlockEnforcer.isAppDepleted x5000", 2000) {
            repeat(5000) { AppBlockEnforcer.isAppDepleted("com.instagram.android") }
        }
    }

    @Test
    fun screenLockoutCheckIsMemoryOnly() {
        twice("ScreenTimeManager.isLockoutActive x500", 2000) {
            repeat(500) { ScreenTimeManager.isLockoutActive(context) }
        }
    }

    @Test
    fun dailyResetCheckIsThrottled() {
        twice("DailyResetManager.checkResetIfDue x200", 2000) {
            repeat(200) { DailyResetManager.checkResetIfDue(context) }
        }
    }

    @Test
    fun routineListUsesMemoryCache() {
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine()))
        twice("FocusTimeManager.getRoutines x200", 2000) {
            repeat(200) { FocusTimeManager.getRoutines(context) }
        }
    }

    @Test
    fun emergencyUnlockCounterIsCheap() {
        twice("EmergencyUnlockManager.getRemainingUnlocksThisMonth x200", 2000) {
            repeat(200) { EmergencyUnlockManager.getRemainingUnlocksThisMonth(context) }
        }
    }
}