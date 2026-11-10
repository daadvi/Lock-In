package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Vibrator
import android.provider.Settings
import com.example.LockInApplication
import com.example.MainActivity
import com.example.util.AppHaptics
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusReminderTest {
    private val app get() = LockInApplication.instance
    private fun enable() {
        app.userPreferences.setRegistration("Teste", "Teste123!")
        app.userPreferences.setStreakReminders(true)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }
    @Test fun enablingSchedulesOneAlarmAndDisablingCancelsIt() {
        enable()
        repeat(3) { FocusReminder.updateSchedule(app) }
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
        fun reminders() = alarms.scheduledAlarms.filter {
            shadowOf(it.operation).savedIntent.action == FocusReminder.ACTION_REMIND
        }
        assertEquals(1, reminders().size)
        assertTrue(reminders().single().isAllowWhileIdle)
        app.userPreferences.setStreakReminders(false)
        FocusReminder.updateSchedule(app)
        assertTrue(reminders().isEmpty())
    }
    @Test fun reminderOpensAppAndIsSuppressedWhenDisabledOrPermissionDenied() {
        enable()
        val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
        FocusReminder.showIfEnabled(app)
        val notification = manager.getNotification(FocusReminder.NOTIFICATION_ID)
        assertNotNull(notification)
        assertEquals(MainActivity::class.java.name, shadowOf(notification.contentIntent).savedIntent.component?.className)
        app.userPreferences.setStreakReminders(false)
        FocusReminder.updateSchedule(app)
        FocusReminder.showIfEnabled(app)
        assertNull(manager.getNotification(FocusReminder.NOTIFICATION_ID))
        app.userPreferences.setStreakReminders(true)
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        FocusReminder.showIfEnabled(app)
        assertNull(manager.getNotification(FocusReminder.NOTIFICATION_ID))
    }
    @Test fun activeFocusSuppressesReminder() {
        enable()
        FocusTimeManager.startFocusSession(app, 30, emptyList())
        FocusReminder.showIfEnabled(app)
        assertNull(shadowOf(app.getSystemService(NotificationManager::class.java)).getNotification(FocusReminder.NOTIFICATION_ID))
    }
    @Test fun eveningAlarmMovesToNextDayAfterTwentyHours() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 3, 20, 1, 0) }
        val next = Calendar.getInstance().apply { timeInMillis = FocusReminder.nextReminderTime(now.timeInMillis) }
        assertEquals(4, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, next.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, next.get(Calendar.MINUTE))
    }
    @Test fun hapticsRespectAppAndSystemPreferences() {
        val vibrator = app.getSystemService(Vibrator::class.java)
        val shadow = shadowOf(vibrator)
        shadow.setHasVibrator(true)
        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        app.userPreferences.setHapticFeedback(true)
        AppHaptics.tap(app)
        assertTrue(shadow.isVibrating)
        vibrator.cancel()
        app.userPreferences.setHapticFeedback(false)
        AppHaptics.tap(app)
        assertFalse(shadow.isVibrating)
        app.userPreferences.setHapticFeedback(true)
        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        AppHaptics.tap(app)
        assertFalse(shadow.isVibrating)
    }
}
