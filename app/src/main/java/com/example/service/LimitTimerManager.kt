package com.example.service

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.BlockedActivity
import com.example.LockInApplication
import com.example.data.local.AppGroupEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveSessionState(
    val isRunning: Boolean = false,
    val packageName: String? = null,
    val appName: String? = null,
    val isShortVideo: Boolean = false,
    val isPictureInPicture: Boolean = false,
    val websiteDomain: String? = null,
    val remainingSeconds: Long = 0L,
    val totalLimitSeconds: Long = 0L,
    val isBlocked: Boolean = false,
    val isAppGroup: Boolean = false,
    val appGroupName: String? = null
)

object LimitTimerManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _sessionState = MutableStateFlow(ActiveSessionState())
    val sessionState: StateFlow<ActiveSessionState> = _sessionState.asStateFlow()

    private var currentApp: RestrictedAppEntity? = null
    private var currentGroup: AppGroupEntity? = null
    private var currentWebsite: RestrictedWebsiteEntity? = null

    @Volatile
    private var cachedMonitoredPackages: Set<String> = emptySet()
    @Volatile
    private var cachedAppsMap: Map<String, RestrictedAppEntity> = emptyMap()
    @Volatile
    private var cachedGroupsList: List<AppGroupEntity> = emptyList()
    @Volatile
    private var cachedWebsitesList: List<RestrictedWebsiteEntity> = emptyList()
    @Volatile
    private var isCacheInitialized: Boolean = false
    @Volatile
    private var currentSwitchJob: Job? = null

    /**
     * Initializes real-time reactive in-memory cache of monitored packages.
     * Prevents database hit on every accessibility event for non-restricted apps.
     */
    fun init(context: Context) {
        val app = LockInApplication.instance
        app.applicationScope.launch {
            app.repository.getAllAppsFlow().collect { apps ->
                cachedAppsMap = apps.associateBy { it.packageName }
                recomputeMonitoredSet()
            }
        }
        app.applicationScope.launch {
            app.repository.getAllAppGroupsFlow().collect { groups ->
                cachedGroupsList = groups
                recomputeMonitoredSet()
            }
        }
        app.applicationScope.launch {
            app.repository.getAllWebsitesFlow().collect { websites ->
                cachedWebsitesList = websites
            }
        }
    }

    private fun recomputeMonitoredSet() {
        val monitored = cachedAppsMap.values.filter { it.isLimited }.map { it.packageName }.toMutableSet()
        monitored.addAll(cachedGroupsList.flatMap { it.getPackageList() })
        cachedMonitoredPackages = monitored
        isCacheInitialized = true
    }

    fun isPackageMonitored(packageName: String): Boolean {
        return cachedMonitoredPackages.contains(packageName)
    }

    fun getCachedApps(): Collection<RestrictedAppEntity> = cachedAppsMap.values

    fun getCachedWebsites(): List<RestrictedWebsiteEntity> = cachedWebsitesList

    /**
     * Called by LockInAccessibilityService when foreground window or content changes.
     * Accurately detects transitions between long videos and short videos (e.g. YouTube standard player vs YouTube Shorts)
     * and switches timers and the floating island instantly with ZERO database lag.
     */
    fun onAppForegroundChanged(
        context: Context,
        packageName: String,
        isShortVideo: Boolean,
        isVideoActive: Boolean = true,
        isPaused: Boolean = false,
        urlDomain: String? = null,
        isPictureInPicture: Boolean = false
    ) {
        // Prevent timer if inside Lock in app itself
        if (packageName == context.packageName) {
            stopTimer(context)
            return
        }

        val currentState = _sessionState.value
        val isCurrentlyPaused = currentState.appName?.contains("Pausado", ignoreCase = true) == true

        // If already running for this exact package, video mode, and PiP state, and video is active and NOT paused, KEEP TICKING!
        if (currentState.isRunning && !isCurrentlyPaused && currentState.packageName == packageName &&
            currentState.isShortVideo == isShortVideo &&
            currentState.isPictureInPicture == isPictureInPicture && isVideoActive && !isPaused) {
            return
        }

        // If already in paused state for this exact package and isPaused is still true, do not re-pause repeatedly
        if (currentState.isRunning && isCurrentlyPaused && currentState.packageName == packageName &&
            currentState.isShortVideo == isShortVideo && isPaused) {
            return
        }

        // Fast-path: if package is not monitored and no active session is running, return immediately without DB hit!
        if (isCacheInitialized && !cachedMonitoredPackages.contains(packageName) && urlDomain == null) {
            if (!currentState.isRunning) {
                return
            }
        }

        val app = LockInApplication.instance
        currentSwitchJob?.cancel()
        currentSwitchJob = app.applicationScope.launch {
            if (!isActive) return@launch
            // 1. If transitioning from an active running session, save its elapsed time first
            if (currentState.isRunning && currentState.remainingSeconds > 0) {
                saveSessionProgress(currentState)
            }

            // Stop existing ticker while configuring the new state
            stopTickerOnly()

            // Check if user is switching between Short Video and Long Video in the same app
            val isModeSwitch = currentState.packageName == packageName && currentState.isShortVideo != isShortVideo

            // Check if the app belongs to an App Group (shared group limit) from in-memory cache
            val matchedGroup = cachedGroupsList.firstOrNull { it.containsPackage(packageName) }

            // Find target app from in-memory cache (prioritizing currentApp updated by saveSessionProgress)
            var matchedApp = currentApp?.takeIf { it.packageName == packageName } ?: cachedAppsMap[packageName]
            // Or find website matching domain or app package
            val matchedWebsite = if (urlDomain != null) {
                cachedWebsitesList.firstOrNull {
                    urlDomain.contains(it.domain, ignoreCase = true) || it.domain.contains(urlDomain, ignoreCase = true)
                } ?: run {
                    val appForDomain = com.example.util.AppWebsiteMapper.findAppForDomain(cachedAppsMap.values, urlDomain)
                    if (appForDomain != null && appForDomain.isLimited) {
                        RestrictedWebsiteEntity(
                            domain = urlDomain,
                            linkedPackageName = appForDomain.packageName,
                            limitMinutes = appForDomain.totalLimitMinutes,
                            remainingSeconds = appForDomain.totalAvailableSeconds,
                            isBlocked = appForDomain.isBlocked || appForDomain.isLimitDepleted
                        )
                    } else null
                }
            } else null

            if (matchedApp == null && matchedWebsite?.linkedPackageName != null) {
                matchedApp = cachedAppsMap[matchedWebsite.linkedPackageName]
            }

            val isGroupLimited = matchedGroup != null
            val isAppLimited = matchedApp != null && matchedApp.isLimited
            val isWebLimited = matchedWebsite != null

            if (!isGroupLimited && !isAppLimited && !isWebLimited) {
                stopTimer(context)
                return@launch
            }

            currentGroup = matchedGroup
            currentApp = matchedApp
            currentWebsite = matchedWebsite

            // Compute the remaining seconds and limit for the CURRENT mode (Shorts vs Long vs Absolute)
            val currentRemainingForPackage = when {
                matchedGroup != null && currentState.isAppGroup && currentGroup?.id == matchedGroup.id && currentState.remainingSeconds > 0 -> {
                    currentState.remainingSeconds
                }
                !isModeSwitch && currentState.packageName == packageName && currentState.remainingSeconds > 0 -> {
                    currentState.remainingSeconds
                }
                else -> null
            }

            val remainingSec: Long = if (matchedGroup != null) {
                currentRemainingForPackage ?: matchedGroup.remainingSeconds
            } else if (matchedWebsite != null) {
                currentRemainingForPackage ?: matchedWebsite.remainingSeconds
            } else if (matchedApp != null) {
                if (!matchedApp.useAbsoluteTime && isShortVideo) {
                    if (matchedApp.shortVideoLimitMinutes > 0) {
                        currentRemainingForPackage ?: matchedApp.shortVideoRemainingSeconds
                    } else {
                        -1L
                    }
                } else if (!matchedApp.useAbsoluteTime && !isShortVideo) {
                    if (matchedApp.longVideoLimitMinutes > 0) {
                        currentRemainingForPackage ?: matchedApp.longVideoRemainingSeconds
                    } else if (matchedApp.individualLimitMinutes > 0) {
                        currentRemainingForPackage ?: matchedApp.individualRemainingSeconds
                    } else {
                        -1L
                    }
                } else {
                    if (matchedApp.individualLimitMinutes > 0) {
                        currentRemainingForPackage ?: matchedApp.individualRemainingSeconds
                    } else {
                        -1L
                    }
                }
            } else -1L

            if (remainingSec == -1L) {
                // Not limited for this mode: do not tick and do not block
                stopTimer(context)
                return@launch
            }

            val totalLimit: Long = if (matchedGroup != null) {
                matchedGroup.limitMinutes * 60L
            } else if (matchedWebsite != null) {
                matchedWebsite.limitMinutes * 60L
            } else if (matchedApp != null) {
                if (!matchedApp.useAbsoluteTime && isShortVideo) {
                    if (matchedApp.shortVideoLimitMinutes > 0) matchedApp.shortVideoLimitMinutes * 60L else 0L
                } else if (!matchedApp.useAbsoluteTime && !isShortVideo) {
                    if (matchedApp.longVideoLimitMinutes > 0) {
                        matchedApp.longVideoLimitMinutes * 60L
                    } else if (matchedApp.individualLimitMinutes > 0) {
                        matchedApp.individualLimitMinutes * 60L
                    } else {
                        0L
                    }
                } else {
                    matchedApp.individualLimitMinutes * 60L
                }
            } else 0L

            val isVideoApp = AppVideoCategoryHelper.hasVideos(packageName) || urlDomain != null
            val baseName = when {
                matchedGroup != null -> matchedGroup.name
                matchedApp != null -> matchedApp.appName
                else -> urlDomain ?: "App"
            }
            val typeSuffix = if (isShortVideo) {
                if (packageName.contains("instagram")) "Reels" else "Shorts"
            } else if (AppVideoCategoryHelper.hasShortVideos(packageName) || AppVideoCategoryHelper.hasShortVideosForDomain(urlDomain)) {
                "Vídeo Longo"
            } else "Vídeo"

            // Check if user is inside ANY video app with video paused
            if (isVideoApp && isPaused) {
                // Video is paused across ANY video app (Shorts, Reels, YouTube, Netflix, etc.)!
                // Suspend countdown, keep floating island visible with PAUSADO
                stopTickerOnly()
                _sessionState.value = ActiveSessionState(
                    isRunning = true,
                    packageName = packageName,
                    appName = "$baseName ($typeSuffix - Pausado)",
                    isShortVideo = isShortVideo,
                    isPictureInPicture = isPictureInPicture,
                    websiteDomain = urlDomain,
                    remainingSeconds = remainingSec,
                    totalLimitSeconds = totalLimit,
                    isBlocked = false,
                    isAppGroup = matchedGroup != null,
                    appGroupName = matchedGroup?.name
                )
                FloatingIslandService.showIsland(context)
                return@launch
            }

            // Check if user is inside a video-limited app with no video playing
            if (matchedApp != null && !matchedApp.useAbsoluteTime && !isVideoActive) {
                // User is inside the app, but no video is open or played.
                stopTimer(context)
                return@launch
            }

            val displayName = if (isShortVideo) {
                if (packageName.contains("instagram")) "$baseName (Reels)" else "$baseName (Shorts)"
            } else if (AppVideoCategoryHelper.hasShortVideos(packageName) || AppVideoCategoryHelper.hasShortVideosForDomain(urlDomain)) {
                "$baseName (Vídeo Longo)"
            } else {
                baseName
            }

            if (remainingSec <= 0L) {
                // Limit reached for this specific video mode: TRIGGER BLOCKING SCREEN!
                triggerBlockScreen(context, displayName, packageName)
                stopTimer(context)
                return@launch
            }

            _sessionState.value = ActiveSessionState(
                isRunning = true,
                packageName = packageName,
                appName = displayName,
                isShortVideo = isShortVideo,
                isPictureInPicture = isPictureInPicture,
                websiteDomain = urlDomain,
                remainingSeconds = remainingSec,
                totalLimitSeconds = totalLimit,
                isBlocked = false,
                isAppGroup = matchedGroup != null,
                appGroupName = matchedGroup?.name
            )

            startTicker(context)
            FloatingIslandService.showIsland(context)
        }
    }

    private fun startTicker(context: Context) {
        timerJob?.cancel()
        timerJob = scope.launch {
            var tickCount = 0
            var lastTickElapsed = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1000L)
                val current = _sessionState.value
                if (!current.isRunning || current.remainingSeconds <= 0) break

                val nowElapsed = SystemClock.elapsedRealtime()
                val deltaSec = ((nowElapsed - lastTickElapsed) / 1000L).coerceAtLeast(1L)
                lastTickElapsed = nowElapsed

                val newRemaining = (current.remainingSeconds - deltaSec).coerceAtLeast(0L)
                _sessionState.value = current.copy(remainingSeconds = newRemaining)
                tickCount += deltaSec.toInt()

                // Save to SQLite every 4 seconds or on limit end
                if (tickCount % 4 == 0 || newRemaining <= 0) {
                    saveRemainingTime(newRemaining)
                }

                if (newRemaining <= 0) {
                    _sessionState.value = _sessionState.value.copy(
                        isRunning = false,
                        remainingSeconds = 0,
                        isBlocked = true
                    )
                    val currPkg = current.packageName
                    if (currPkg != null) {
                        AppBlockEnforcer.markAppDepleted(currPkg)
                    }
                    val linkedPkg = currentWebsite?.linkedPackageName
                    if (linkedPkg != null) {
                        AppBlockEnforcer.markAppDepleted(linkedPkg)
                    }
                    triggerBlockScreen(context, current.appName ?: "App", current.packageName)
                    FloatingIslandService.hideIsland(context)
                    break
                }
            }
        }
    }

    private suspend fun saveSessionProgress(state: ActiveSessionState) {
        val app = LockInApplication.instance
        val db = app.repository
        val prefs = app.userPreferences
        val pkg = state.packageName ?: return

        val group = currentGroup
        if (group != null && state.isAppGroup) {
            val updated = group.copy(
                remainingSeconds = state.remainingSeconds,
                isBlocked = state.remainingSeconds <= 0
            )
            db.updateAppGroup(updated)
            currentGroup = updated
            return
        }

        val appEntity = db.getApp(pkg) ?: currentApp
        if (appEntity != null && appEntity.isLimited) {
            val updated = if (!appEntity.useAbsoluteTime && state.isShortVideo) {
                val totalRemaining = state.remainingSeconds + appEntity.longVideoRemainingSeconds
                val shortDepleted = appEntity.shortVideoLimitMinutes > 0 && state.remainingSeconds <= 0
                val longDepleted = appEntity.longVideoLimitMinutes > 0 && appEntity.longVideoRemainingSeconds <= 0
                val bothConfigured = appEntity.shortVideoLimitMinutes > 0 && appEntity.longVideoLimitMinutes > 0
                val blocked = if (bothConfigured) (shortDepleted && longDepleted) else shortDepleted
                appEntity.copy(
                    shortVideoRemainingSeconds = state.remainingSeconds,
                    individualRemainingSeconds = totalRemaining,
                    isBlocked = blocked
                )
            } else if (!appEntity.useAbsoluteTime && !state.isShortVideo) {
                val totalRemaining = appEntity.shortVideoRemainingSeconds + state.remainingSeconds
                val longDepleted = appEntity.longVideoLimitMinutes > 0 && state.remainingSeconds <= 0
                val shortDepleted = appEntity.shortVideoLimitMinutes > 0 && appEntity.shortVideoRemainingSeconds <= 0
                val bothConfigured = appEntity.shortVideoLimitMinutes > 0 && appEntity.longVideoLimitMinutes > 0
                val blocked = if (bothConfigured) (longDepleted && shortDepleted) else longDepleted
                appEntity.copy(
                    longVideoRemainingSeconds = state.remainingSeconds,
                    individualRemainingSeconds = totalRemaining,
                    isBlocked = blocked
                )
            } else {
                appEntity.copy(
                    individualRemainingSeconds = state.remainingSeconds,
                    isBlocked = appEntity.individualLimitMinutes > 0 && state.remainingSeconds <= 0
                )
            }
            db.updateApp(updated)
            currentApp = updated
            cachedAppsMap = cachedAppsMap + (updated.packageName to updated)
        }
    }

    private suspend fun saveRemainingTime(
        seconds: Long,
        state: ActiveSessionState = _sessionState.value,
        group: AppGroupEntity? = currentGroup,
        appEntity: RestrictedAppEntity? = currentApp,
        webEntity: RestrictedWebsiteEntity? = currentWebsite
    ) {
        val app = LockInApplication.instance
        val prefs = app.userPreferences
        val db = app.repository

        if (group != null) {
            val updatedGroup = group.copy(
                remainingSeconds = seconds,
                isBlocked = seconds <= 0
            )
            db.updateAppGroup(updatedGroup)
            if (currentGroup === group) currentGroup = updatedGroup
            return
        }

        if (appEntity != null && appEntity.isLimited) {
            val updated = if (!appEntity.useAbsoluteTime && state.isShortVideo) {
                val totalRemaining = seconds + appEntity.longVideoRemainingSeconds
                val shortDepleted = appEntity.shortVideoLimitMinutes > 0 && seconds <= 0
                val longDepleted = appEntity.longVideoLimitMinutes > 0 && appEntity.longVideoRemainingSeconds <= 0
                val bothConfigured = appEntity.shortVideoLimitMinutes > 0 && appEntity.longVideoLimitMinutes > 0
                val blocked = if (bothConfigured) (shortDepleted && longDepleted) else shortDepleted
                appEntity.copy(
                    shortVideoRemainingSeconds = seconds,
                    individualRemainingSeconds = totalRemaining,
                    isBlocked = blocked
                )
            } else if (!appEntity.useAbsoluteTime && !state.isShortVideo) {
                val totalRemaining = appEntity.shortVideoRemainingSeconds + seconds
                val longDepleted = appEntity.longVideoLimitMinutes > 0 && seconds <= 0
                val shortDepleted = appEntity.shortVideoLimitMinutes > 0 && appEntity.shortVideoRemainingSeconds <= 0
                val bothConfigured = appEntity.shortVideoLimitMinutes > 0 && appEntity.longVideoLimitMinutes > 0
                val blocked = if (bothConfigured) (longDepleted && shortDepleted) else longDepleted
                appEntity.copy(
                    longVideoRemainingSeconds = seconds,
                    individualRemainingSeconds = totalRemaining,
                    isBlocked = blocked
                )
            } else {
                appEntity.copy(
                    individualRemainingSeconds = seconds,
                    isBlocked = appEntity.individualLimitMinutes > 0 && seconds <= 0
                )
            }
            db.updateApp(updated)
            if (currentApp === appEntity) currentApp = updated

            if (seconds <= 0) {
                AppBlockEnforcer.markAppDepleted(updated.packageName)
            }

            // Bidirectional sync: update any corresponding website
            val linkedDomains = com.example.util.AppWebsiteMapper.getWebsitesForApp(updated.packageName, updated.appName)
            for (domain in linkedDomains) {
                val matchedWeb = cachedWebsitesList.firstOrNull { it.domain.equals(domain, ignoreCase = true) }
                if (matchedWeb != null) {
                    val updatedWeb = matchedWeb.copy(
                        remainingSeconds = seconds,
                        isBlocked = seconds <= 0 || updated.isBlocked
                    )
                    db.updateWebsite(updatedWeb)
                }
            }
        }

        if (webEntity != null) {
            val updatedWeb = webEntity.copy(remainingSeconds = seconds, isBlocked = seconds <= 0)
            db.updateWebsite(updatedWeb)
            if (currentWebsite === webEntity) currentWebsite = updatedWeb

            // Bidirectional sync: update corresponding app if linked
            val linkedPkg = webEntity.linkedPackageName
            if (linkedPkg != null) {
                val targetApp = cachedAppsMap[linkedPkg]
                if (targetApp != null) {
                    val updatedApp = targetApp.copy(
                        individualRemainingSeconds = seconds,
                        isBlocked = seconds <= 0
                    )
                    db.updateApp(updatedApp)
                    if (seconds <= 0) {
                        AppBlockEnforcer.markAppDepleted(targetApp.packageName)
                    }
                }
            }
        }
    }

    private fun stopTickerOnly() {
        timerJob?.cancel()
        timerJob = null
    }

    internal fun stopTimer(context: Context, persistenceScope: CoroutineScope = scope) {
        val current = _sessionState.value
        val group = currentGroup
        val app = currentApp
        val website = currentWebsite
        if (current.isRunning) {
            persistenceScope.launch {
                saveRemainingTime(current.remainingSeconds, current, group, app, website)
            }
        }
        stopTickerOnly()
        _sessionState.value = ActiveSessionState(isRunning = false)
        currentGroup = null
        currentApp = null
        currentWebsite = null
        FloatingIslandService.hideIsland(context)
    }

    fun onDailyReset() {
        currentApp = null
        currentGroup = null
        currentWebsite = null
        val current = _sessionState.value
        if (current.isBlocked) {
            _sessionState.value = ActiveSessionState(isRunning = false)
        }
    }

    private fun triggerBlockScreen(context: Context, appOrSiteName: String, packageName: String? = null) {
        // Dismiss target app process and window without sending device to Home Launcher
        try {
            LockInAccessibilityService.closeAndDismissApp(context, packageName)
        } catch (_: Exception) {}

        val intent = Intent(context, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(BlockedActivity.EXTRA_BLOCKED_TARGET, appOrSiteName)
            putExtra(BlockedActivity.EXTRA_APP_NAME, appOrSiteName)
            if (packageName != null) {
                putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(BlockedActivity.EXTRA_PROHIBITED_PACKAGE, packageName)
            }
            putExtra(
                BlockedActivity.EXTRA_BLOCKED_REASON,
                "Você já excedeu o limite de uso deste app"
            )
            putExtra(BlockedActivity.EXTRA_IS_DEPLETED_LIMIT, true)
        }
        try {
            com.example.util.XiaomiCompatibilityHelper.safelyStartActivity(context, intent)
        } catch (e: Exception) {
            android.util.Log.e("LimitTimerManager", "Failed to launch BlockedActivity", e)
        }
    }
}
