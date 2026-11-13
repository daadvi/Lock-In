package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.local.LockInDatabase
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.data.local.UserPreferencesRepository
import com.example.service.ScreenLockReceiver
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import android.os.Handler
import android.os.Looper

class LockInApplication : Application() {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("LockInApplication", "Intercepted async coroutine exception: ${throwable.message}", throwable)
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    val database by lazy { LockInDatabase.getDatabase(this) }
    val repository by lazy { database.lockInDao() }
    val userPreferences by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Preserve Android's crash reporting and process shutdown after recording the error.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("LockInApplication", "Uncaught exception on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        com.example.service.BankingSecurityHelper.init(this)
        com.example.service.StreakManager.init(this)
        com.example.service.LimitTimerManager.init(this)
        com.example.service.AppUnlockDailyTracker.init(this)
        com.example.service.ScreenTimeManager.init(this)
        com.example.service.AppBlockEnforcer.initialize(this)
        com.example.service.PlayStoreBlacklistManager.initialize(this)
        com.example.service.WebsiteBlacklistManager.initialize(this)
        // Catch-up: apps já existentes na blacklist de instalação têm seus sites bloqueados
        com.example.service.PlayStoreBlacklistManager.syncBlockedAppWebsites(this)
        com.example.service.LockInVpnManager.initialize(this)
        com.example.service.CloudSyncManager.initialize(this)
        com.example.service.FriendsPartyManager.init(this)
        com.example.service.DailyResetManager.checkResetIfDue(this)
        com.example.service.DailyResetManager.scheduleMidnightAlarm(this)
        applicationScope.launch {
            combine(userPreferences.isRegistered, userPreferences.streakReminders) { registered, enabled ->
                registered && enabled
            }.distinctUntilChanged().collect {
                com.example.service.FocusReminder.updateSchedule(this@LockInApplication)
            }
        }

        if (com.example.service.LockInVpnManager.isVpnEnabled.value && !com.example.service.LockInVpnManager.isPermissionRequired(this)) {
            com.example.service.LockInVpnManager.startVpn(this)
        }
        cleanFactoryDefaultsIfPresent()
    }

    private fun cleanFactoryDefaultsIfPresent() {
        val cleanPrefs = getSharedPreferences("app_clean_init_prefs", Context.MODE_PRIVATE)
        if (!cleanPrefs.getBoolean("cleaned_factory_defaults_v4", false)) {
            cleanPrefs.edit().putBoolean("cleaned_factory_defaults_v4", true).apply()
            applicationScope.launch {
                try {
                    val apps = repository.getAllAppsList()
                    for (app in apps) {
                        if (!app.isLimited || app.totalLimitMinutes <= 0) {
                            repository.deleteApp(app.packageName)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun checkScheduledReset() {
        com.example.service.DailyResetManager.checkResetIfDue(this)
    }

    companion object {
        lateinit var instance: LockInApplication
            private set
    }
}
