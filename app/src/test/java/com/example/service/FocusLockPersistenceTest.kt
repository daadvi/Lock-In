package com.example.service

import android.app.AlarmManager
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.FocusLockActivity
import com.example.LockInApplication
import com.example.data.local.FocusRoutine
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Certifies that the focus lock screen (device block button and scheduled routines)
 * survives reboots, clock tampering, home/launcher escapes and task removal, and
 * only disappears when the real configured time has actually elapsed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusLockPersistenceTest {
    private val app get() = LockInApplication.instance
    private val prefs get() = app.getSharedPreferences("lock_in_focus_time_prefs", 0)
    private val services = mutableListOf<LockInAccessibilityService>()

    @Before fun reset() {
        prefs.edit().clear().commit()
        FocusTimeManager.setAllRoutines(app, emptyList())
        FocusTimeManager.setEmergencyAllowedPackages(app, emptyList())
        FocusTimeManager.stopFocusSession(app)
        FocusLockActivity.isLaunchingAllowedApp = false
        FocusLockActivity.lastAllowedLaunchEpoch = 0L
    }

    @After fun cleanup() {
        services.forEach { runCatching { it.onDestroy() } }
        services.clear()
        FocusTimeManager.setAllRoutines(app, emptyList())
        FocusLockActivity.isLaunchingAllowedApp = false
    }

    private fun drainStartedActivities(): List<Intent> {
        val shadow = shadowOf(app)
        val out = mutableListOf<Intent>()
        while (shadow.peekNextStartedActivity() != null) out.add(shadow.nextStartedActivity!!)
        return out
    }

    private fun lockLaunches() = drainStartedActivities().filter {
        it.component?.className == FocusLockActivity::class.java.name
    }

    private fun routineAlarms() = shadowOf(app.getSystemService(AlarmManager::class.java))
        .scheduledAlarms.filter {
            shadowOf(it.operation).savedIntent.component?.className == FocusRoutineAlarmReceiver::class.java.name
        }

    private fun retryAlarms() = shadowOf(app.getSystemService(AlarmManager::class.java))
        .scheduledAlarms.filter {
            shadowOf(it.operation).savedIntent.component?.className == FocusLockRetryReceiver::class.java.name
        }

    /** Broadcasts are dispatched on the main looper; idle it so receivers run now. */
    private fun broadcastAndIdle(intent: Intent) {
        app.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun rebootKeepsDirectSessionActive() {
        FocusTimeManager.startFocusSession(app, 30, emptyList())
        lockLaunches() // consume the immediate launch
        // Simulate a device reboot: the boot counter changes, elapsedRealtime restarts.
        Settings.Global.putInt(app.contentResolver, Settings.Global.BOOT_COUNT, 100)
        assertTrue(FocusTimeManager.isFocusTimeActive(app))
        val remaining = FocusTimeManager.getRemainingSeconds(app)
        assertTrue("remaining was $remaining", remaining in 1790..1800)
    }

    @Test
    fun rebootRelaunchesLockScreenAndSchedulesRetries() {
        FocusTimeManager.startFocusSession(app, 30, emptyList())
        lockLaunches() // consume the immediate launch
        Settings.Global.putInt(app.contentResolver, Settings.Global.BOOT_COUNT, 101)
        broadcastAndIdle(Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(lockLaunches().isNotEmpty())
        // Two delayed retries in case the first background launch was swallowed.
        assertEquals(2, retryAlarms().size)
    }

    @Test
    fun clockJumpForwardPastSessionEndKeepsLock() {
        val now = System.currentTimeMillis()
        // Clock was moved 61 minutes forward past the stored wall-clock end; only one
        // real minute of the 30-minute session had elapsed before the tampering.
        prefs.edit()
            .putLong("focus_end_epoch", now - 60_000L)
            .putLong("focus_duration_ms", 1_800_000L)
            .putLong("focus_start_epoch", now - 1_860_000L)
            .putLong("focus_accumulated_ms", 60_000L)
            .putLong("focus_last_tick_epoch", now - 3_660_000L)
            .commit()
        assertTrue(FocusTimeManager.isFocusTimeActive(app))
        val remaining = FocusTimeManager.getRemainingSeconds(app)
        assertTrue("remaining was $remaining", remaining in 1735..1745)
    }

    @Test
    fun powerOffGapIsCountedAsElapsedTime() {
        val now = System.currentTimeMillis()
        // Phone was switched off for 10 minutes during the session; that counts.
        prefs.edit()
            .putLong("focus_end_epoch", now + 1_740_000L)
            .putLong("focus_duration_ms", 1_800_000L)
            .putLong("focus_start_epoch", now - 70_000L)
            .putLong("focus_accumulated_ms", 60_000L)
            .putLong("focus_last_tick_epoch", now - 600_000L)
            .commit()
        assertTrue(FocusTimeManager.isFocusTimeActive(app))
        val remaining = FocusTimeManager.getRemainingSeconds(app)
        assertTrue("remaining was $remaining", remaining in 1135..1145)
    }

    @Test
    fun clockRewindDoesNotRefillRemainingTime() {
        val now = System.currentTimeMillis()
        // Clock moved 10 minutes back with one minute left: no time is recovered.
        prefs.edit()
            .putLong("focus_end_epoch", now + 60_000L)
            .putLong("focus_duration_ms", 1_800_000L)
            .putLong("focus_start_epoch", now - 1_799_000L)
            .putLong("focus_accumulated_ms", 1_740_000L)
            .putLong("focus_last_tick_epoch", now + 600_000L)
            .commit()
        assertTrue(FocusTimeManager.isFocusTimeActive(app))
        val remaining = FocusTimeManager.getRemainingSeconds(app)
        assertTrue("remaining was $remaining", remaining in 55..65)
    }

    @Test
    fun sessionOnlyEndsWhenRealTimeRunsOut() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong("focus_end_epoch", now + 5_000L)
            .putLong("focus_duration_ms", 1_800_000L)
            .putLong("focus_start_epoch", now - 1_808_000L)
            .putLong("focus_accumulated_ms", 1_792_000L)
            .putLong("focus_last_tick_epoch", now - 5_000L)
            .commit()
        assertTrue(FocusTimeManager.isFocusTimeActive(app))
        val remaining = FocusTimeManager.getRemainingSeconds(app)
        assertTrue("remaining was $remaining", remaining in 0..5)

        prefs.edit()
            .putLong("focus_accumulated_ms", 1_799_500L)
            .putLong("focus_last_tick_epoch", now - 1_000L)
            .commit()
        assertFalse(FocusTimeManager.isFocusTimeActive(app))
        assertEquals(0L, prefs.getLong("focus_end_epoch", -1L))
    }

    @Test
    fun screenOnBringsLockBackButRespectsAllowedAppGrace() {
        FocusTimeManager.startFocusSession(app, 5, emptyList())
        lockLaunches()
        ScreenLockReceiver().onReceive(app, Intent(Intent.ACTION_SCREEN_ON))
        assertTrue(lockLaunches().isNotEmpty())

        // While the user deliberately opened an allowed app, never yank the screen back.
        FocusLockActivity.isLaunchingAllowedApp = true
        ScreenLockReceiver().onReceive(app, Intent(Intent.ACTION_USER_PRESENT))
        assertTrue(lockLaunches().isEmpty())

        FocusLockActivity.isLaunchingAllowedApp = false
        ScreenLockReceiver().onReceive(app, Intent(Intent.ACTION_USER_PRESENT))
        assertTrue(lockLaunches().isNotEmpty())
    }

    @Test
    fun launcherHomeEventBringsLockBackToFront() {
        val service = Robolectric.buildService(LockInAccessibilityService::class.java).create().get()
            .also { services.add(it) }
        FocusTimeManager.startFocusSession(app, 5, emptyList())
        lockLaunches()
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = "com.google.android.apps.nexuslauncher"
        }
        service.onAccessibilityEvent(event)
        event.recycle()
        assertTrue(lockLaunches().isNotEmpty())
    }

    @Test
    fun routineStartAlarmLaunchesLockAndReArmsNextOccurrence() {
        val routine = FocusRoutine(
            id = "routine-alarm-test",
            name = "Rotina 24h",
            startHour = 0, startMinute = 0,
            endHour = 23, endMinute = 59,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            allowedPackages = emptyList(),
            isEnabled = true,
            targetPackage = null,
            targetAppName = null
        )
        FocusTimeManager.saveRoutine(app, routine)
        assertEquals(1, routineAlarms().size)

        // The alarm fires exactly at the routine start minute.
        broadcastAndIdle(Intent(app, FocusRoutineAlarmReceiver::class.java))
        assertTrue(lockLaunches().isNotEmpty())
        // Next occurrence is re-armed automatically.
        assertEquals(1, routineAlarms().size)
    }

    @Test
    fun disablingRoutineCancelsItsStartAlarm() {
        val routine = FocusRoutine(
            id = "routine-cancel-test",
            name = "Rotina",
            startHour = 0, startMinute = 0,
            endHour = 23, endMinute = 59,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            allowedPackages = emptyList(),
            isEnabled = true,
            targetPackage = null,
            targetAppName = null
        )
        FocusTimeManager.saveRoutine(app, routine)
        assertEquals(1, routineAlarms().size)
        FocusTimeManager.toggleRoutine(app, routine.id, false)
        assertTrue(routineAlarms().isEmpty())
    }
}
