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
 * Regressão: apps liberados de uma ROTINA ativa devem ser permitidos e os demais barrados.
 * A lista é por rotina (anti-bypass: rotina não herda exceções globais).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusAllowedAppsTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun cleanup() {
        FocusTimeManager.setAllRoutines(context, emptyList())
        FocusTimeManager.setEmergencyAllowedPackages(context, emptyList())
    }

    private fun activeRoutine(allowed: List<String>): FocusRoutine {
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = ((minuteOfDay - 5) + 1440) % 1440
        val end = (minuteOfDay + 5) % 1440
        return FocusRoutine(
            name = "Rotina ativa",
            startHour = start / 60,
            startMinute = start % 60,
            endHour = end / 60,
            endMinute = end % 60,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            allowedPackages = allowed
        )
    }

    @Test
    fun routineAllowedAppsArePermittedDuringLock() {
        val allowedOne = "com.example.allowed.one"
        val allowedTwo = "com.example.allowed.two"
        FocusTimeManager.setAllRoutines(
            context,
            listOf(activeRoutine(listOf("com.google.android.dialer", allowedOne, allowedTwo)))
        )

        assertTrue(FocusTimeManager.isPackageAllowed(context, allowedOne))
        assertTrue(FocusTimeManager.isPackageAllowed(context, allowedTwo))
        val resolved = FocusTimeManager.getAllowedPackages(context)
        assertTrue(resolved.contains(allowedOne))
        assertTrue(resolved.contains(allowedTwo))
    }

    @Test
    fun packageOutsideRoutineAllowlistStaysBlocked() {
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine(listOf("com.google.android.dialer"))))

        assertFalse(FocusTimeManager.isPackageAllowed(context, "com.example.not.allowed"))
    }

    @Test
    fun routineAllowlistIsCappedAtThreeApps() {
        FocusTimeManager.setAllRoutines(
            context,
            listOf(activeRoutine(listOf("com.example.a", "com.example.b", "com.example.c", "com.example.d")))
        )

        assertTrue(FocusTimeManager.getAllowedPackages(context).size <= 3)
    }
}