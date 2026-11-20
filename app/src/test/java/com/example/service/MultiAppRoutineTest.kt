package com.example.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FocusRoutine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Rotinas por app devem bloquear VÁRIOS aplicativos de uma vez.
 * Também garante que rotinas antigas (1 app apenas) continuam funcionando.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiAppRoutineTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun cleanup() {
        FocusTimeManager.setAllRoutines(context, emptyList())
    }

    /** Janela de 10 minutos em volta do "agora" (para o teste não depender da hora fixa). */
    private fun windowAroundNow(): RoutineWindow {
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = ((minuteOfDay - 5) + 1440) % 1440
        val end = (minuteOfDay + 5) % 1440
        return RoutineWindow(start / 60, start % 60, end / 60, end % 60)
    }

    private data class RoutineWindow(
        val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int
    )

    private fun multiAppRoutine(
        name: String = "Bloqueio noturno",
        packages: List<String>,
        names: List<String>
    ): FocusRoutine {
        val w = windowAroundNow()
        return FocusRoutine(
            name = name,
            startHour = w.startHour,
            startMinute = w.startMinute,
            endHour = w.endHour,
            endMinute = w.endMinute,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            targetPackages = packages,
            targetAppNames = names
        )
    }

    @Test
    fun multiAppRoutineBlocksEveryTargetedApp() {
        val packages = listOf("com.instagram.android", "com.zhiliaoapp.musically", "com.kwai.video")
        val names = listOf("Instagram", "TikTok", "Kwai")
        FocusTimeManager.setAllRoutines(context, listOf(multiAppRoutine(packages = packages, names = names)))

        packages.forEach { pkg ->
            assertNotNull("Rotina deve bloquear $pkg", FocusTimeManager.getActiveAppRoutine(context, pkg))
        }
        // App fora da rotina não é bloqueado por ela
        assertNull(FocusTimeManager.getActiveAppRoutine(context, "com.whatsapp"))
    }

    @Test
    fun legacySingleAppRoutineStillBlocksItsApp() {
        val w = windowAroundNow()
        val legacy = FocusRoutine(
            name = "Só Instagram",
            startHour = w.startHour,
            startMinute = w.startMinute,
            endHour = w.endHour,
            endMinute = w.endMinute,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            targetPackage = "com.instagram.android",
            targetAppName = "Instagram"
        )
        FocusTimeManager.setAllRoutines(context, listOf(legacy))

        assertNotNull(FocusTimeManager.getActiveAppRoutine(context, "com.instagram.android"))
        assertNull(FocusTimeManager.getActiveAppRoutine(context, "com.zhiliaoapp.musically"))
    }

    @Test
    fun multiAppRoutineSurvivesPersistenceRoundTrip() {
        val routine = multiAppRoutine(
            packages = listOf("com.instagram.android", "com.kwai.video"),
            names = listOf("Instagram", "Kwai")
        )
        FocusTimeManager.saveRoutine(context, routine)

        val saved = FocusTimeManager.getRoutines(context).first { it.id == routine.id }
        assertTrue(saved.isAppSpecific)
        assertEquals(listOf("com.instagram.android", "com.kwai.video"), saved.resolvedTargetPackages)
        assertEquals(listOf("Instagram", "Kwai"), saved.resolvedTargetNames)
        assertEquals(listOf("com.instagram.android", "com.kwai.video"), FocusTimeManager.getPackagesWithRoutines(context))
    }

    @Test
    fun deviceWideRoutineDoesNotBlockSpecificApps() {
        val w = windowAroundNow()
        val deviceWide = FocusRoutine(
            name = "Foco total",
            startHour = w.startHour,
            startMinute = w.startMinute,
            endHour = w.endHour,
            endMinute = w.endMinute,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        FocusTimeManager.setAllRoutines(context, listOf(deviceWide))

        assertNull(FocusTimeManager.getActiveAppRoutine(context, "com.instagram.android"))
        assertNull(FocusTimeManager.getActiveAppRoutine(context, "com.zhiliaoapp.musically"))
        assertFalse(deviceWide.isAppSpecific)
    }

    @Test
    fun removingOneAppFromMultiRoutineLeavesTheOthersBlocked() {
        val routine = multiAppRoutine(
            packages = listOf("com.instagram.android", "com.kwai.video"),
            names = listOf("Instagram", "Kwai")
        )
        FocusTimeManager.saveRoutine(context, routine)

        FocusTimeManager.saveRoutine(
            context,
            routine.copy(targetPackages = listOf("com.instagram.android"), targetAppNames = listOf("Instagram"))
        )

        assertNotNull(FocusTimeManager.getActiveAppRoutine(context, "com.instagram.android"))
        assertNull(FocusTimeManager.getActiveAppRoutine(context, "com.kwai.video"))
        assertEquals(1, FocusTimeManager.getRoutinesForApp(context, "com.instagram.android").size)
        assertTrue(FocusTimeManager.getRoutinesForApp(context, "com.kwai.video").isEmpty())
    }
}