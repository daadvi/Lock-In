package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.LockInApplication
import com.example.MainActivity
import com.example.R
import java.util.Calendar

/** A single inexact evening alarm; no polling loop or foreground service. */
object FocusReminder {
    internal const val CHANNEL_ID = "focus_reminders"
    internal const val NOTIFICATION_ID = 2201
    internal const val ACTION_REMIND = "com.example.lockin.REMIND_FOCUS"

    fun notificationsAllowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
                ?.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE)

    internal fun nextReminderTime(now: Long): Long = Calendar.getInstance().run {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        timeInMillis
    }

    fun updateSchedule(context: Context, now: Long = System.currentTimeMillis()) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val alarm = PendingIntent.getBroadcast(context, NOTIFICATION_ID,
            Intent(context, FocusReminderReceiver::class.java).setAction(ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val prefs = (context.applicationContext as LockInApplication).userPreferences
        if (!prefs.isRegistered.value || !prefs.streakReminders.value) {
            alarmManager.cancel(alarm)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        // Android may defer this alarm to preserve battery; the UI says "por volta das 20h".
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextReminderTime(now), alarm)
    }

    fun showIfEnabled(context: Context) {
        val prefs = (context.applicationContext as LockInApplication).userPreferences
        if (!prefs.isRegistered.value || !prefs.streakReminders.value || !notificationsAllowed(context)) return
        if (FocusTimeManager.isFocusTimeActive(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Lembretes de foco",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lembrete noturno para revisar sua sequência e suas metas"
                enableVibration(false)
            })
        }
        val openApp = PendingIntent.getActivity(context, NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_clock)
            .setContentTitle("Reserve um momento para o seu foco")
            .setContentText("Revise sua sequência e planeje uma sessão de foco para hoje.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try { manager.notify(NOTIFICATION_ID, notification) } catch (_: SecurityException) {
            // The user can revoke notification access between checking and publishing.
        }
    }
}

class FocusReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == FocusReminder.ACTION_REMIND) FocusReminder.showIfEnabled(context)
        FocusReminder.updateSchedule(context)
    }
}
