package com.example.service

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FocusRoutine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FocusPolicySecurityTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val prefs get() = context.getSharedPreferences("lock_in_focus_time_prefs", 0)
    @Before fun reset() {
        prefs.edit().clear().commit()
        FocusTimeManager.setAllRoutines(context, emptyList())
        FocusTimeManager.setEmergencyAllowedPackages(context, emptyList())
    }
    private fun activeRoutine(packages: List<String>): FocusRoutine {
        val now = Calendar.getInstance()
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = (minute + 1439) % 1440
        val end = (minute + 2) % 1440
        return FocusRoutine(name = "Routine", startHour = start / 60, startMinute = start % 60,
            endHour = end / 60, endMinute = end % 60, allowedPackages = packages)
    }
    @Test fun routineDoesNotInheritGlobalOrRecentlyOpenedExceptions() {
        FocusTimeManager.setEmergencyAllowedPackages(context, listOf("test.global"))
        FocusTimeManager.notifyAllowedAppLaunched("test.global")
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine(listOf("test.routine"))))
        assertEquals(listOf("test.routine"), FocusTimeManager.getAllowedPackages(context))
        assertFalse(FocusTimeManager.isPackageAllowed(context, "test.global"))
        assertTrue(FocusTimeManager.isPackageAllowed(context, "test.routine"))
    }
    @Test fun directSessionUsesItsOwnListAndExpiredSessionUsesRoutine() {
        FocusTimeManager.setEmergencyAllowedPackages(context, listOf("test.direct"))
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine(listOf("test.routine"))))
        prefs.edit().putLong("focus_end_epoch", System.currentTimeMillis() + 60000).commit()
        assertEquals(listOf("test.direct"), FocusTimeManager.getAllowedPackages(context))
        prefs.edit().putLong("focus_end_epoch", 1L).commit()
        assertEquals(listOf("test.routine"), FocusTimeManager.getAllowedPackages(context))
    }
    @Test fun emptyRoutineAllowlistDoesNotFallBackToGlobalList() {
        FocusTimeManager.setEmergencyAllowedPackages(context, listOf("test.global"))
        FocusTimeManager.setAllRoutines(context, listOf(activeRoutine(emptyList())))
        assertTrue(FocusTimeManager.getAllowedPackages(context).isEmpty())
        FocusTimeManager.setAllRoutines(context, emptyList())
        assertEquals(listOf("test.global"), FocusTimeManager.getAllowedPackages(context))
    }
    @Test fun sameBootDeadlineIgnoresWallClockChangesAndRebootUsesRecoveryTime() {
        android.provider.Settings.Global.putInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 5)
        prefs.edit().putLong("focus_end_epoch", 1L)
            .putLong("focus_end_elapsed", android.os.SystemClock.elapsedRealtime() + 60_000L)
            .putInt("focus_start_boot", 5).commit()
        assertTrue(FocusTimeManager.isFocusTimeActive(context))
        assertEquals(60L, FocusTimeManager.getRemainingSeconds(context))
        android.provider.Settings.Global.putInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 6)
        assertFalse(FocusTimeManager.isFocusTimeActive(context))
    }

}
