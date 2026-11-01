package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.service.FocusTimeManager
import com.example.service.ScreenTimeManager
import com.example.service.StreakManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LockInWidgetManager {

    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val TAB_DASHBOARD = "dashboard"
    const val TAB_FOCUS = "focus"
    const val TAB_LIMITS = "limits"
    const val TAB_STATS = "statistics"

    private val widgetScope = CoroutineScope(Dispatchers.Default)

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return@launch

                // 1. Update Screen Time Widgets
                val screenTimeIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(appContext, LockInScreenTimeWidget::class.java)
                )
                if (screenTimeIds.isNotEmpty()) {
                    updateScreenTimeWidgets(appContext, appWidgetManager, screenTimeIds)
                }

                // 2. Update Focus Widgets
                val focusIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(appContext, LockInFocusWidget::class.java)
                )
                if (focusIds.isNotEmpty()) {
                    updateFocusWidgets(appContext, appWidgetManager, focusIds)
                }

                // 3. Update Quick Bar Widgets
                val barIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(appContext, LockInQuickBarWidget::class.java)
                )
                if (barIds.isNotEmpty()) {
                    updateQuickBarWidgets(appContext, appWidgetManager, barIds)
                }

                // 4. Update Adherence Consistency Widgets
                val adherenceIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(appContext, LockInAdherenceWidget::class.java)
                )
                if (adherenceIds.isNotEmpty()) {
                    updateAdherenceWidgets(appContext, appWidgetManager, adherenceIds)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateScreenTimeWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val limitMinutes = ScreenTimeManager.limitMinutesFlow.value
        val remainingSec = ScreenTimeManager.remainingSecondsFlow.value
        val isLockout = ScreenTimeManager.isLockoutActive(context)
        val streak = StreakManager.getStreak(context)

        val clockText = when {
            limitMinutes <= 0 -> "--:--"
            isLockout -> "00:00"
            else -> {
                val min = remainingSec / 60
                val sec = remainingSec % 60
                String.format("%02d:%02d", min, sec)
            }
        }

        val statusText = when {
            limitMinutes <= 0 -> "SEM LIMITE"
            isLockout -> "BLOQUEADO"
            else -> "ATIVO"
        }

        val statusColor = when {
            isLockout -> Color.parseColor("#FF3B30")
            limitMinutes <= 0 -> Color.parseColor("#8E8E93")
            else -> Color.parseColor("#00E676")
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_screen_time)

            views.setTextViewText(R.id.widget_screen_time_clock, clockText)
            views.setTextViewText(R.id.widget_status_badge, statusText)
            views.setTextColor(R.id.widget_status_badge, statusColor)
            views.setTextViewText(R.id.widget_streak_text, "$streak DIAS")

            // Main click intent: open MainActivity
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_DASHBOARD)
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_action_btn, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    fun updateFocusWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val isFocusActive = FocusTimeManager.isFocusTimeActive(context)
        val routineName = FocusTimeManager.getActiveRoutineName(context)
        val remainingSec = FocusTimeManager.getRemainingSeconds(context)

        val primaryText = if (isFocusActive) {
            val min = remainingSec / 60
            val sec = remainingSec % 60
            String.format("%02d:%02d", min, sec)
        } else {
            "100%"
        }

        val secondaryText = if (isFocusActive) {
            "SESSÃO DE FOCO ATIVA"
        } else {
            "LIMITE RESPEITADO"
        }

        val statusText = if (isFocusActive) "EM FOCO" else "PRONTO"
        val statusColor = if (isFocusActive) Color.parseColor("#00E676") else Color.parseColor("#FF5500")
        val routineLabel = routineName ?: if (isFocusActive) "Sessão Direta" else "Sem rotina ativa"
        val actionText = if (isFocusActive) "VER FOCO" else "INICIAR"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_focus)

            views.setTextViewText(R.id.widget_focus_primary_text, primaryText)
            views.setTextViewText(R.id.widget_focus_secondary_text, secondaryText)
            views.setTextViewText(R.id.widget_focus_status_badge, statusText)
            views.setTextColor(R.id.widget_focus_status_badge, statusColor)
            views.setTextViewText(R.id.widget_focus_routine_label, routineLabel)
            views.setTextViewText(R.id.widget_focus_action_btn, actionText)

            val focusIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_FOCUS)
            }
            val focusPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                focusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_focus_root, focusPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_focus_action_btn, focusPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    fun updateQuickBarWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val limitMinutes = ScreenTimeManager.limitMinutesFlow.value
        val remainingSec = ScreenTimeManager.remainingSecondsFlow.value
        val isLockout = ScreenTimeManager.isLockoutActive(context)
        val streak = StreakManager.getStreak(context)

        val clockText = when {
            limitMinutes <= 0 -> "--:--"
            isLockout -> "00:00"
            else -> {
                val min = remainingSec / 60
                val sec = remainingSec % 60
                String.format("%02d:%02d", min, sec)
            }
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_bar)

            views.setTextViewText(R.id.widget_bar_streak, "${streak}d")
            views.setTextViewText(R.id.widget_bar_clock, clockText)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_FOCUS)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bar_root, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_bar_action_btn, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    fun updateAdherenceWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val streak = StreakManager.getStreak(context)
        val isViolated = StreakManager.isTodayViolated(context)
        val isLockout = ScreenTimeManager.isLockoutActive(context)

        val scoreText = if (isViolated || isLockout) "0%" else "100%"
        val statusText = if (isViolated || isLockout) "VIOLADO" else "DISCIPLINA"
        val statusColor = if (isViolated || isLockout) Color.parseColor("#FF3B30") else Color.parseColor("#00E676")

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_adherence)

            views.setTextViewText(R.id.widget_adherence_score, scoreText)
            views.setTextViewText(R.id.widget_adherence_status_badge, statusText)
            views.setTextColor(R.id.widget_adherence_status_badge, statusColor)
            views.setTextViewText(R.id.widget_adherence_streak_text, "$streak DIAS")

            // Main click intent: open MainActivity to statistics
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_STATS)
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 3,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_adherence_root, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_adherence_action_btn, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
