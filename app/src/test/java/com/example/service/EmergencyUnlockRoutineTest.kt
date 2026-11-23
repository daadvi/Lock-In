package com.example.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FocusRoutine
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Regressão: concluir o desbloqueio emergencial (senha + frase) deve realmente liberar o
 * dispositivo, inclusive quando o bloqueio vem de uma ROTINA agendada (antes a rotina
 * continuava ativa e a tela de bloqueio voltava na hora).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencyUnlockRoutineTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun cleanup() {
        FocusTimeManager.setAllRoutines(context, emptyList())
        FocusTimeManager.setEmergencyAllowedPackages(context, emptyList())
    }

    private fun activeRoutine(): FocusRoutine {
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = ((minuteOfDay - 5) + 1440) % 1440
        val end = (minuteOfDay + 5) % 1440
        return FocusRoutine(
            name = "Rotina bloqueante",
            startHour = start / 60,
            startMinute = start % 60,
            endHour = end / 60,
            endMinute = end % 60,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
        )
    }

    @Test
    fun emergencyUnlockSuppressesActiveRoutineUntilItEnds() {
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine()))
        assertTrue(FocusTimeManager.isFocusTimeActive(context))

        FocusTimeManager.suppressActiveRoutineAfterUnlock(context)
        FocusTimeManager.stopFocusSession(context)

        assertFalse("Dispositivo deve ficar liberado após o desbloqueio emergencial",
            FocusTimeManager.isFocusTimeActive(context))
    }

    @Test
    fun otherRoutinesStillApplyAfterSuppressingOne() {
        val blocking = activeRoutine()
        val second = activeRoutine().copy(id = "routine-2", name = "Outra rotina")
        FocusTimeManager.setAllRoutines(context, listOf(blocking, second))

        FocusTimeManager.suppressActiveRoutineAfterUnlock(context)

        // A primeira rotina encontrada ativa é suprimida; a segunda continua valendo
        // (nenhuma brecha além da ocorrência desbloqueada).
        assertTrue(FocusTimeManager.isFocusTimeActive(context))
    }
}