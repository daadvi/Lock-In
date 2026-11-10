package com.example

import com.example.ui.tutorial.tutorialTarget
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.ui.components.AppBottomDock
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.ClockLimitScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusTimeScreen
import com.example.ui.screens.FriendsPanelScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.ResetSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.StylingScreen
import com.example.ui.screens.TrustUnlockScreen
import com.example.ui.screens.WebLinksScreen
import com.example.data.local.AppGroupEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.service.FocusTimeManager
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LockInTheme
import kotlinx.coroutines.launch

enum class Screen : java.io.Serializable {
    DASHBOARD,
    APP_SELECTION,
    CLOCK_LIMIT,
    WEB_LINKS,
    RESET_SETTINGS,
    STYLING,
    TRUST_UNLOCK,
    PERMISSIONS,
    STATISTICS,
    FOCUS_TIME,
    FRIENDS_PANEL,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    companion object {
        var lastSavedScreen: Screen? = null
        var lastSavedTargetPackage: String? = null
        var lastSavedTargetAppName: String? = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastSavedScreen?.let { outState.putString("KEY_SAVED_SCREEN", it.name) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getString("KEY_SAVED_SCREEN")?.let { name ->
            try {
                lastSavedScreen = Screen.valueOf(name)
            } catch (_: Exception) {}
        }
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val startAtTrustUnlock = intent?.getBooleanExtra("NAV_TO_TRUST_UNLOCK", false) == true

        setContent {
            val app = LockInApplication.instance
            val userPrefs = app.userPreferences
            val repository = app.repository

            val isRegistered by userPrefs.isRegistered.collectAsStateWithLifecycle()
            // Restoring a backup can mark the account registered before permission setup finishes.
            var onboardingInProgress by rememberSaveable { mutableStateOf(!isRegistered) }
            val userName by userPrefs.userName.collectAsStateWithLifecycle()
            val textColorHex by userPrefs.textColorHex.collectAsStateWithLifecycle()
            val screenTimeLimitMinutes by com.example.service.ScreenTimeManager.limitMinutesFlow.collectAsStateWithLifecycle()
            val screenTimeResetHours by com.example.service.ScreenTimeManager.resetIntervalHoursFlow.collectAsStateWithLifecycle()
            val resetIntervalHours by userPrefs.resetIntervalHours.collectAsStateWithLifecycle()
            val islandSize by userPrefs.floatingIslandSize.collectAsStateWithLifecycle()
            val islandOpacity by userPrefs.floatingIslandOpacity.collectAsStateWithLifecycle()
            val islandPosX by userPrefs.floatingIslandPosX.collectAsStateWithLifecycle()
            val islandPosY by userPrefs.floatingIslandPosY.collectAsStateWithLifecycle()

            val themeMode by userPrefs.themeMode.collectAsStateWithLifecycle()
            val hapticFeedback by userPrefs.hapticFeedback.collectAsStateWithLifecycle()
            val streakReminders by userPrefs.streakReminders.collectAsStateWithLifecycle()
            val preLimitAlerts by userPrefs.preLimitAlerts.collectAsStateWithLifecycle()
            val mindfulDelay by userPrefs.mindfulDelay.collectAsStateWithLifecycle()
            val strictMode by userPrefs.strictMode.collectAsStateWithLifecycle()

            val restrictedApps by remember(repository) { repository.getAllAppsFlow() }.collectAsStateWithLifecycle(initialValue = emptyList())
            val appGroups by remember(repository) { repository.getAllAppGroupsFlow() }.collectAsStateWithLifecycle(initialValue = emptyList())
            val restrictedWebsites by remember(repository) { repository.getAllWebsitesFlow() }.collectAsStateWithLifecycle(initialValue = emptyList())

            val openTab = intent?.getStringExtra(com.example.widget.LockInWidgetManager.EXTRA_OPEN_TAB)
            val openScreen = intent?.getStringExtra("OPEN_SCREEN")
            val computedScreen = when {
                startAtTrustUnlock -> Screen.TRUST_UNLOCK
                openScreen == "FRIENDS_PANEL" -> Screen.FRIENDS_PANEL
                openTab == com.example.widget.LockInWidgetManager.TAB_FOCUS -> Screen.FOCUS_TIME
                openTab == com.example.widget.LockInWidgetManager.TAB_LIMITS -> Screen.APP_SELECTION
                openTab == com.example.widget.LockInWidgetManager.TAB_STATS -> Screen.STATISTICS
                else -> Screen.DASHBOARD
            }
            val initialScreen = lastSavedScreen ?: computedScreen

            val screenSaver = androidx.compose.runtime.saveable.Saver<Screen, String>(
                save = { it.name },
                restore = { name -> try { Screen.valueOf(name) } catch (_: Exception) { Screen.DASHBOARD } }
            )

            var currentScreen by rememberSaveable(stateSaver = screenSaver) {
                mutableStateOf(initialScreen)
            }
            var previousScreen by rememberSaveable(stateSaver = screenSaver) { mutableStateOf(Screen.DASHBOARD) }
            var targetAppRoutinePackage by rememberSaveable { mutableStateOf(lastSavedTargetPackage) }
            var targetAppRoutineName by rememberSaveable { mutableStateOf(lastSavedTargetAppName) }
            var targetUnlockAppPackage by rememberSaveable { mutableStateOf<String?>(null) }
            var targetUnlockAppName by rememberSaveable { mutableStateOf<String?>(null) }

            LaunchedEffect(currentScreen) {
                lastSavedScreen = currentScreen
            }
            LaunchedEffect(targetAppRoutinePackage, targetAppRoutineName) {
                lastSavedTargetPackage = targetAppRoutinePackage
                lastSavedTargetAppName = targetAppRoutineName
            }

            val customAccentColor = try {
                Color(textColorHex.toColorInt())
            } catch (_: Exception) {
                Color(0xFFFF5500)
            }

            LaunchedEffect(Unit) {
                com.example.service.EmergencyUnlockManager.checkAndResetMonthlyUsage(this@MainActivity)
                app.applicationScope.launch {
                    try {
                        repository.getAllAppsList().forEach { appEntity ->
                            if (com.example.util.AppFilterHelper.isLockInSelfApp(this@MainActivity, appEntity.packageName, appEntity.appName)) {
                                repository.deleteApp(appEntity.packageName)
                            }
                        }
                        com.example.util.AppWebsiteMapper.syncAllAppsAndWebsites(repository)
                    } catch (_: Exception) {}
                }
                try {
                    if (android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                        com.example.service.FloatingIslandService.ensureStarted(this@MainActivity)
                    }
                } catch (_: Exception) {}
            }

            var tutorialReturnScreen by rememberSaveable { mutableStateOf<String?>(null) }
            LockInTheme(customTextColor = customAccentColor, themeMode = themeMode) {
                com.example.ui.tutorial.TutorialHost(
                    onStart = { tutorialReturnScreen = currentScreen.name },
                    onNavigate = { chapter ->
                        if (isRegistered && !onboardingInProgress) currentScreen = com.example.ui.tutorial.tutorialScreen(chapter)
                    },
                    onFinish = {
                        tutorialReturnScreen?.let { currentScreen = Screen.valueOf(it) }
                        tutorialReturnScreen = null
                    }
                ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isRegistered || onboardingInProgress) {
                        OnboardingScreen(
                            onCompleteRegistration = { name, password ->
                                userPrefs.setRegistration(name, password)
                                onboardingInProgress = false
                                currentScreen = Screen.DASHBOARD
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (currentScreen) {
                                Screen.DASHBOARD -> DashboardScreenContainer(
                                    userName = userName,
                                    screenTimeLimitMinutes = screenTimeLimitMinutes,
                                    screenTimeResetHours = screenTimeResetHours,
                                    limitedApps = restrictedApps,
                                    appGroups = appGroups,
                                    websites = restrictedWebsites,
                                    resetIntervalHours = resetIntervalHours,
                                    floatingIslandSize = islandSize,
                                    floatingIslandOpacity = islandOpacity,
                                    onNavigateToApps = { currentScreen = Screen.APP_SELECTION },
                                    onNavigateToLimits = { currentScreen = Screen.CLOCK_LIMIT },
                                    onNavigateToWebsites = { currentScreen = Screen.WEB_LINKS },
                                    onNavigateToStyling = {
                                        previousScreen = Screen.DASHBOARD
                                        currentScreen = Screen.STYLING
                                    },
                                    onNavigateToResetSettings = {
                                        previousScreen = Screen.DASHBOARD
                                        currentScreen = Screen.RESET_SETTINGS
                                    },
                                    onNavigateToTrustUnlock = { pkg, name ->
                                        targetUnlockAppPackage = pkg
                                        targetUnlockAppName = name
                                        currentScreen = Screen.TRUST_UNLOCK
                                    },
                                    onNavigateToPermissions = {
                                        previousScreen = Screen.DASHBOARD
                                        currentScreen = Screen.PERMISSIONS
                                    },
                                    onNavigateToStatistics = { currentScreen = Screen.STATISTICS },
                                    onNavigateToFocusTime = { currentScreen = Screen.FOCUS_TIME },
                                    onNavigateToAppRoutine = { pkg, name ->
                                        targetAppRoutinePackage = pkg
                                        targetAppRoutineName = name
                                        currentScreen = Screen.FOCUS_TIME
                                    },
                                    onUpdateAppEntity = { updated ->
                                        app.applicationScope.launch {
                                            repository.updateApp(updated)
                                            com.example.util.AppWebsiteMapper.syncWebsitesForApp(repository, updated)
                                        }
                                    },
                                    onSaveAppGroup = { group ->
                                        app.applicationScope.launch {
                                            repository.insertOrUpdateAppGroup(group)
                                        }
                                    },
                                    onDeleteAppGroup = { groupId ->
                                        app.applicationScope.launch {
                                            repository.deleteAppGroup(groupId)
                                        }
                                    },
                                    onUpdateScreenTimeLimit = { minutes ->
                                        com.example.service.ScreenTimeManager.setLimitMinutes(this@MainActivity, minutes)
                                    },
                                    onUpdateResetIntervalHours = { hours ->
                                        com.example.service.ScreenTimeManager.setResetIntervalHours(this@MainActivity, hours)
                                        userPrefs.setResetSchedule(hours)
                                    },
                                    onResetScreenTimeNow = {
                                        com.example.service.ScreenTimeManager.resetScreenTimeNow(this@MainActivity)
                                    }
                                )

                                Screen.APP_SELECTION -> AppSelectionScreen(
                                    currentRestrictedApps = restrictedApps,
                                    appGroups = appGroups,
                                    trustPassword = userPrefs.getTrustPassword(),
                                    onSaveApp = { appEntity ->
                                        app.applicationScope.launch {
                                            repository.insertOrUpdateApp(appEntity)
                                            com.example.util.AppWebsiteMapper.syncWebsitesForApp(repository, appEntity)
                                            com.example.service.CloudSyncManager.syncDataToCloud(this@MainActivity)
                                        }
                                    },
                                    onRemoveApp = { pkg ->
                                        app.applicationScope.launch {
                                            val existing = repository.getApp(pkg)
                                            if (existing != null) {
                                                com.example.util.AppWebsiteMapper.syncWebsitesForApp(repository, existing.copy(isLimited = false))
                                            }
                                            repository.deleteApp(pkg)
                                            com.example.service.CloudSyncManager.syncDataToCloud(this@MainActivity)
                                        }
                                    },
                                    onSaveAppGroup = { group ->
                                        app.applicationScope.launch {
                                            repository.insertOrUpdateAppGroup(group)
                                            com.example.service.CloudSyncManager.syncDataToCloud(this@MainActivity)
                                        }
                                    },
                                    onDeleteAppGroup = { groupId ->
                                        app.applicationScope.launch {
                                            repository.deleteAppGroup(groupId)
                                            com.example.service.CloudSyncManager.syncDataToCloud(this@MainActivity)
                                        }
                                    },
                                    onNavigateToAppRoutines = { pkg, name ->
                                        targetAppRoutinePackage = pkg
                                        targetAppRoutineName = name
                                        currentScreen = Screen.FOCUS_TIME
                                    },
                                    onNavigateToTrustUnlock = { pkg, name ->
                                        targetUnlockAppPackage = pkg
                                        targetUnlockAppName = name
                                        currentScreen = Screen.TRUST_UNLOCK
                                    },
                                    onNavigateToWebsites = { currentScreen = Screen.WEB_LINKS },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )

                                Screen.CLOCK_LIMIT -> ClockLimitScreen(
                                    screenTimeLimitMinutes = screenTimeLimitMinutes,
                                    screenTimeResetHours = screenTimeResetHours,
                                    onUpdateScreenTimeLimit = { minutes ->
                                        com.example.service.ScreenTimeManager.setLimitMinutes(this@MainActivity, minutes)
                                    },
                                    onUpdateResetIntervalHours = { hours ->
                                        com.example.service.ScreenTimeManager.setResetIntervalHours(this@MainActivity, hours)
                                        userPrefs.setResetSchedule(hours)
                                    },
                                    onResetScreenTimeNow = {
                                        com.example.service.ScreenTimeManager.resetScreenTimeNow(this@MainActivity)
                                    },
                                    restrictedApps = restrictedApps,
                                    onUpdateAppLimit = { updated ->
                                        app.applicationScope.launch {
                                            repository.updateApp(updated)
                                            com.example.util.AppWebsiteMapper.syncWebsitesForApp(repository, updated)
                                        }
                                    },
                                    onNavigateToTrustUnlock = { pkg, name ->
                                        targetUnlockAppPackage = pkg
                                        targetUnlockAppName = name
                                        currentScreen = Screen.TRUST_UNLOCK
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )

                                Screen.WEB_LINKS -> WebLinksScreen(
                                    websites = restrictedWebsites,
                                    restrictedApps = restrictedApps,
                                    onAddWebsite = { web ->
                                        app.applicationScope.launch {
                                            repository.insertWebsite(web)
                                        }
                                    },
                                    onDeleteWebsite = { web ->
                                        app.applicationScope.launch {
                                            repository.deleteWebsite(web)
                                        }
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )

                                Screen.RESET_SETTINGS -> ResetSettingsScreen(
                                    currentIntervalHours = screenTimeResetHours,
                                    onSaveInterval = { hours ->
                                        userPrefs.setResetSchedule(hours)
                                        com.example.service.ScreenTimeManager.setResetIntervalHours(this@MainActivity, hours)
                                    },
                                    onManualResetAll = {
                                        app.applicationScope.launch {
                                            com.example.service.ScreenTimeManager.resetScreenTimeNow(this@MainActivity)
                                            repository.resetAllAppTimers()
                                            repository.resetAllWebsiteTimers()
                                        }
                                    },
                                    onBack = { currentScreen = previousScreen }
                                )

                                Screen.STYLING -> StylingScreen(
                                    currentTextColorHex = textColorHex,
                                    currentIslandSize = islandSize,
                                    currentIslandOpacity = islandOpacity,
                                    currentPosXRatio = islandPosX,
                                    currentPosYRatio = islandPosY,
                                    onSaveTextColorHex = { hex ->
                                        userPrefs.setTextColorHex(hex)
                                    },
                                    onSaveIslandSettings = { size, opacity, rx, ry ->
                                        userPrefs.setFloatingIslandSettings(size, opacity, rx, ry)
                                    },
                                    onBack = { currentScreen = previousScreen }
                                )

                                Screen.TRUST_UNLOCK -> TrustUnlockScreen(
                                    correctTrustPassword = userPrefs.getTrustPassword(),
                                    targetAppName = targetUnlockAppName,
                                    targetPackageName = targetUnlockAppPackage,
                                    onUnlockConfirmed = {
                                        com.example.service.StreakManager.markViolation(this@MainActivity)
                                        val pkgToUnlock = targetUnlockAppPackage
                                        if (!pkgToUnlock.isNullOrBlank()) {
                                            com.example.service.AppBlockEnforcer.clearDepletedForPackage(pkgToUnlock)
                                        } else {
                                            com.example.service.AppBlockEnforcer.clearAllDepletedForToday()
                                        }
                                        app.applicationScope.launch {
                                            if (!pkgToUnlock.isNullOrBlank()) {
                                                repository.resetAppTimer(pkgToUnlock)
                                            } else {
                                                userPrefs.resetSharedTimer()
                                                repository.resetAllAppTimers()
                                                repository.resetAllWebsiteTimers()
                                            }
                                        }
                                    },
                                    onBack = {
                                        targetUnlockAppPackage = null
                                        targetUnlockAppName = null
                                        currentScreen = Screen.DASHBOARD
                                    }
                                )


                                Screen.PERMISSIONS -> PermissionsScreen(
                                    onBack = { currentScreen = previousScreen }
                                )

                                Screen.STATISTICS -> StatisticsScreen(
                                    limitedApps = restrictedApps,
                                    websites = restrictedWebsites,
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )

                                Screen.FOCUS_TIME -> FocusTimeScreen(
                                    initialTargetPackage = targetAppRoutinePackage,
                                    initialTargetAppName = targetAppRoutineName,
                                    onClearTargetApp = {
                                        targetAppRoutinePackage = null
                                        targetAppRoutineName = null
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )

                                Screen.FRIENDS_PANEL -> FriendsPanelScreen(
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )

                                Screen.SETTINGS -> SettingsScreen(
                                    currentThemeMode = themeMode,
                                    hapticFeedbackEnabled = hapticFeedback,
                                    streakRemindersEnabled = streakReminders,
                                    preLimitAlertsEnabled = preLimitAlerts,
                                    mindfulDelayEnabled = mindfulDelay,
                                    strictModeEnabled = strictMode,
                                    userName = userName,
                                    currentPassword = { userPrefs.getTrustPassword() },
                                    onSaveThemeMode = { mode -> userPrefs.setThemeMode(mode) },
                                    onSaveHapticFeedback = { enabled -> userPrefs.setHapticFeedback(enabled) },
                                    onSaveStreakReminders = { enabled -> userPrefs.setStreakReminders(enabled) },
                                    onSavePreLimitAlerts = { enabled -> userPrefs.setPreLimitAlerts(enabled) },
                                    onSaveMindfulDelay = { enabled -> userPrefs.setMindfulDelay(enabled) },
                                    onSaveStrictMode = { enabled -> userPrefs.setStrictMode(enabled) },
                                    onUpdateUserName = { newName -> userPrefs.updateUserName(newName) },
                                    onUpdatePassword = { newPass -> userPrefs.updatePassword(newPass) },
                                    onResetToDefaults = { userPrefs.resetAllSettingsToDefault() },
                                    onNavigateToStyling = {
                                        previousScreen = Screen.SETTINGS
                                        currentScreen = Screen.STYLING
                                    },
                                    onNavigateToResetSettings = {
                                        previousScreen = Screen.SETTINGS
                                        currentScreen = Screen.RESET_SETTINGS
                                    },
                                    onNavigateToPermissions = {
                                        previousScreen = Screen.SETTINGS
                                        currentScreen = Screen.PERMISSIONS
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }

                            // Bottom Navigation Dock across the entire app
                            if (currentScreen != Screen.TRUST_UNLOCK) {
                                AppBottomDock(
                                    currentScreen = currentScreen,
                                    accentColor = customAccentColor,
                                    onNavigate = { screen -> currentScreen = screen },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(bottom = 10.dp)
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.service.FloatingIslandService.setLockInInForeground(true)
    }

    override fun onResume() {
        super.onResume()
        com.example.service.FloatingIslandService.setLockInInForeground(true)
        com.example.service.DailyResetManager.checkResetIfDue(this)
        // Update all home screen widgets
        com.example.widget.LockInWidgetManager.updateAllWidgets(this)

        // If Focus Time is active right now, bring FocusLockActivity immediately!
        if (FocusTimeManager.isFocusTimeActive(this)) {
            FocusTimeManager.bringFocusLockToFront(this)
        }
    }

    override fun onStop() {
        super.onStop()
        com.example.service.FloatingIslandService.setLockInInForeground(false)
    }
}

@Composable
private fun DashboardScreenContainer(
    userName: String,
    screenTimeLimitMinutes: Int,
    screenTimeResetHours: Int,
    limitedApps: List<RestrictedAppEntity>,
    appGroups: List<AppGroupEntity>,
    websites: List<RestrictedWebsiteEntity>,
    resetIntervalHours: Int,
    floatingIslandSize: String,
    floatingIslandOpacity: Float,
    onNavigateToApps: () -> Unit,
    onNavigateToLimits: () -> Unit,
    onNavigateToWebsites: () -> Unit,
    onNavigateToStyling: () -> Unit,
    onNavigateToResetSettings: () -> Unit,
    onNavigateToTrustUnlock: ((packageName: String?, appName: String?) -> Unit)?,
    onNavigateToPermissions: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFocusTime: () -> Unit,
    onNavigateToAppRoutine: ((packageName: String, appName: String) -> Unit)?,
    onUpdateAppEntity: (RestrictedAppEntity) -> Unit,
    onSaveAppGroup: (AppGroupEntity) -> Unit,
    onDeleteAppGroup: (String) -> Unit,
    onUpdateScreenTimeLimit: (Int) -> Unit,
    onUpdateResetIntervalHours: (Int) -> Unit,
    onResetScreenTimeNow: () -> Unit
) {
    val screenTimeRemainingSeconds by com.example.service.ScreenTimeManager.remainingSecondsFlow.collectAsStateWithLifecycle()
    val isScreenTimeLockout by com.example.service.ScreenTimeManager.isLockoutActiveFlow.collectAsStateWithLifecycle()

    DashboardScreen(
        userName = userName,
        screenTimeLimitMinutes = screenTimeLimitMinutes,
        screenTimeRemainingSeconds = screenTimeRemainingSeconds,
        screenTimeResetHours = screenTimeResetHours,
        isScreenTimeLockout = isScreenTimeLockout,
        limitedApps = limitedApps,
        appGroups = appGroups,
        websites = websites,
        resetIntervalHours = resetIntervalHours,
        floatingIslandSize = floatingIslandSize,
        floatingIslandOpacity = floatingIslandOpacity,
        onNavigateToApps = onNavigateToApps,
        onNavigateToLimits = onNavigateToLimits,
        onNavigateToWebsites = onNavigateToWebsites,
        onNavigateToStyling = onNavigateToStyling,
        onNavigateToResetSettings = onNavigateToResetSettings,
        onNavigateToTrustUnlock = onNavigateToTrustUnlock,
        onNavigateToPermissions = onNavigateToPermissions,
        onNavigateToStatistics = onNavigateToStatistics,
        onNavigateToFocusTime = onNavigateToFocusTime,
        onNavigateToAppRoutine = onNavigateToAppRoutine,
        onUpdateAppEntity = onUpdateAppEntity,
        onSaveAppGroup = onSaveAppGroup,
        onDeleteAppGroup = onDeleteAppGroup,
        onUpdateScreenTimeLimit = onUpdateScreenTimeLimit,
        onUpdateResetIntervalHours = onUpdateResetIntervalHours,
        onResetScreenTimeNow = onResetScreenTimeNow
    )
}
