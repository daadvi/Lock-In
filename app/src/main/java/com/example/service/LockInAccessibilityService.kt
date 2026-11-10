package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.R
import com.example.util.XiaomiCompatibilityHelper

class LockInAccessibilityService : AccessibilityService() {

    private var screenReceiver: ScreenLockReceiver? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scanBudget = NodeScanBudget()
    private val uninstallOverlay by lazy { UninstallChallengeOverlay(this) }
    private var uninstallActionClickedEpoch = 0L
    private var pendingUninstallPackage: String? = null
    private val pendingUninstallCheck = Runnable {
        val pkg = pendingUninstallPackage ?: return@Runnable
        pendingUninstallPackage = null
        scanBudget.reset()
        runCatching { checkUninstallAttempt(pkg, System.currentTimeMillis()) }
    }
    private fun isUninstallSurface(pkg: String): Boolean = pkg.contains("settings") || pkg.contains("installer") ||
        pkg.contains("securitycenter") || pkg.contains("launcher") || pkg.contains("home") || pkg == "com.android.vending" || pkg == "android"

    internal fun showUninstallChallenge(mode: String): Boolean {
        if (DeviceAdminManager.isUninstallAuthorized(this)) return false
        return uninstallOverlay.show(mode)
    }


    // Debounce and caching state to guarantee zero main-thread ANR / freeze
    private var lastProcessedPackage: String? = null
    private var lastContentChangeProcessedEpoch = 0L

    private var lastPlayStoreBlockEpoch = 0L
    private var lastPlayStoreScanEpoch = 0L
    private var lastBlacklistLaunchEpoch = 0L
    private var lastAppRoutineBlockEpoch = 0L

    private var lastUninstallInterceptEpoch = 0L
    private var lastUninstallInterceptPackage: String? = null
    private var lastUninstallScanEpoch = 0L

    private var lastBrowserCheckEpoch = 0L
    private var cachedBrowserUrl: String? = null
    private var lastBrowserPackage: String? = null

    private var lastShortVideoCheckEpoch = 0L
    private var cachedShortVideoResult = false
    private var lastShortVideoPackage: String? = null

    private var lastPipScanEpoch = 0L
    private var cachedPipPackage: String? = null
    private var lastVideoStatePkg: String? = null
    private var lastVideoStateResult: VideoPlaybackState? = null
    private var lastVideoStateEpoch: Long = 0L
    private var lastWebsiteBlockEpoch: Long = 0L

    private val browserPackages = listOf(
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser"
    )

    private val pendingVideoStateRunnable = Runnable {
        scanBudget.reset()
        var root: AccessibilityNodeInfo? = null
        try {
            root = rootInActiveWindow ?: return@Runnable
            val activePkg = root.packageName?.toString() ?: return@Runnable
            if (AppVideoCategoryHelper.hasVideos(activePkg)) {
                val now = System.currentTimeMillis()
                val rawBrowserUrl = detectBrowserRawUrl(activePkg, now)
                val browserUrlDomain = rawBrowserUrl?.replace("https://", "")?.replace("http://", "")?.split("/")?.firstOrNull()
                val dummyEvent = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
                val videoState = detectVideoPlaybackState(dummyEvent, activePkg, rawBrowserUrl, now)
                dummyEvent.recycle()
                LimitTimerManager.onAppForegroundChanged(
                    context = applicationContext,
                    packageName = activePkg,
                    isShortVideo = videoState.isShortVideo,
                    isVideoActive = videoState.isVideoActive,
                    isPaused = videoState.isPaused,
                    urlDomain = browserUrlDomain,
                    isPictureInPicture = false
                )
            }
        } catch (_: Exception) {
        } finally {
            try { root?.recycle() } catch (_: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        scanBudget.reset()
        try {
            val eventType = event.eventType

        // 1. Ignore scroll, hover, notification and gesture noise
        val isClick = eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            eventType == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER ||
            eventType == AccessibilityEvent.TYPE_VIEW_HOVER_EXIT ||
            eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT ||
            eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_START ||
            eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_END) {
            return
        }

        val pkg = event.packageName?.toString() ?: if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val root = rootInActiveWindow
            try { root?.packageName?.toString() } finally { root?.recycle() }
        } else null
        if (pkg.isNullOrBlank() || pkg == packageName) return

        // CRITICAL BANKING & FINANCIAL SECURITY ISOLATION:
        // Banking apps (Caixa, Nubank, Itaú, Bradesco, Santander, Inter, etc.) scan for active
        // accessibility services. When a banking app is active or protection is paused,
        // we exit IMMEDIATELY with zero inspection, zero window interrogation, and zero delay,
        // and instantly suppress any overlay.
        if (BankingSecurityHelper.isBankingOrFinancialApp(pkg) || BankingSecurityHelper.isBankingProtectionPaused()) {
            FloatingIslandService.hideIsland(applicationContext)
            return
        }

        if (com.example.service.DeviceAdminManager.isUninstallAuthorized(applicationContext)) {
            return
        }

        DailyResetManager.checkResetIfDue(applicationContext)

        val now = System.currentTimeMillis()

        if (isUninstallSurface(pkg)) {
            if (isClick) {
                val clickLabels = event.text.map { it.toString() } + listOfNotNull(event.contentDescription?.toString())
                if (clickLabels.any { it.trim().lowercase(java.util.Locale.ROOT) in setOf("desinstalar", "uninstall", "desativar e desinstalar", "deactivate and uninstall") }) {
                    uninstallActionClickedEpoch = now
                }
            }
            if (checkUninstallAttempt(pkg, now)) return
            pendingUninstallPackage = pkg
            mainHandler.removeCallbacks(pendingUninstallCheck)
            mainHandler.postDelayed(pendingUninstallCheck, 250L)
        }

        // Debounce clicks slightly (200ms) to avoid double work, but NEVER debounce video platform clicks (Instagram, YouTube)
        val isVideoPlatform = pkg.contains("instagram") || pkg.contains("youtube") || pkg.contains("tiktok")
        if (isClick && !isVideoPlatform && (now - lastContentChangeProcessedEpoch < 200L)) {
            return
        }

        // 2. Coalesce rapid content changed events for the SAME package to eliminate UI thread saturation and ANRs
        val isVideoApp = isVideoPlatform || AppVideoCategoryHelper.hasVideos(pkg)
        val minInterval = if (isVideoApp) 500L else 600L
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (pkg == lastProcessedPackage && (now - lastContentChangeProcessedEpoch < minInterval)) {
                return
            }
        }
        lastProcessedPackage = pkg
        lastContentChangeProcessedEpoch = now

        if (isVideoApp) {
            mainHandler.removeCallbacks(pendingVideoStateRunnable)
            mainHandler.postDelayed(pendingVideoStateRunnable, 450L)
        }

        // 2.35 CRITICAL: If an uninstallation of ANY other application is occurring, ALLOW IT FREELY!
        // Never block or trigger installation blocked screen when uninstalling an app!
        val isPackageInstaller = pkg.contains("packageinstaller") || pkg.contains("installer")
        if (isPackageInstaller) {
            val root = rootInActiveWindow ?: event.source
            val texts = mutableListOf<String>()
            try {
                if (root != null) {
                    collectWindowTexts(root, texts, depth = 0)
                }
            } finally {
                try { root?.recycle() } catch (_: Exception) {}
            }

            val className = event.className?.toString() ?: ""
            val isUninstallerClass = className.contains("Uninstall", ignoreCase = true) || className.contains("Delete", ignoreCase = true)
            val hasUninstall = isUninstallerClass || texts.any {
                it.contains("desinstalar") || it.contains("uninstall") ||
                        it.contains("deseja desinstalar") || it.contains("desinstalação") ||
                        it.contains("remover") || it.contains("excluir")
            }

            if (hasUninstall) {
                isUninstallingOtherApp = true
                allowedUninstallTimestamp = now
                Log.d("LockInAccessibility", "Uninstallation in progress for other app - allowing freely without blocking.")
                return
            }

            if (isUninstallingOtherApp) {
                if (now - allowedUninstallTimestamp < 15_000L) {
                    return
                } else {
                    isUninstallingOtherApp = false
                }
            }
        }

        // Check if launcher / home screen
        val isLauncher = (pkg.contains("launcher") || pkg.contains("home") || pkg.contains("trebuchet") || pkg.contains("nexuslauncher") || pkg.contains("pixel")) &&
                !pkg.contains("packageinstaller")
        if (isLauncher) {
            LimitTimerManager.stopTimer(applicationContext)
            isUninstallingOtherApp = false
            return
        }

        val isSystemHelperPackage = pkg == "com.android.systemui" ||
                pkg.contains("inputmethod") ||
                pkg.contains("keyboard") ||
                pkg == "android" ||
                pkg == "com.google.android.permissioncontroller" ||
                pkg.contains("incallui") ||
                pkg.contains("callui") ||
                pkg.contains("telecom") ||
                PhoneAppHelper.isPhoneDialerApp(applicationContext, pkg)

        // 1. Check if FOCUS TIME is active (Device Total Lockdown)
        if (FocusTimeManager.isFocusTimeActive(applicationContext) && !isSystemHelperPackage) {
            val isAllowed = FocusTimeManager.isPackageAllowed(applicationContext, pkg)
            if (!isAllowed) {
                FocusTimeManager.bringFocusLockToFront(applicationContext)
                return
            }
        }

        // 2. Check if ABSOLUTE SCREEN TIME LOCKOUT is active
        if (ScreenTimeManager.isLockoutActive(applicationContext) && !isSystemHelperPackage) {
            val isAllowed = FocusTimeManager.isPackageAllowed(applicationContext, pkg)
            if (!isAllowed) {
                ScreenTimeManager.bringLockoutToFront(applicationContext)
                return
            }
        }

        if (isSystemHelperPackage) return

        // 2.0 ZERO-TOLERANCE PLAY STORE & INSTALLATION BLACKLIST CHECK
        val earlyBlacklistMatch = PlayStoreBlacklistManager.isAppOrPackageBlacklisted(applicationContext, pkg)
        if (earlyBlacklistMatch != null) {
            // Check if friend in party granted temporary approval
            val isFriendApproved = FriendsPartyManager.isPackageTemporarilyAllowed(pkg) ||
                    FriendsPartyManager.isPackageTemporarilyAllowed(earlyBlacklistMatch)
            if (!isFriendApproved) {
                if (now - lastBlacklistLaunchEpoch > 500L) {
                    lastBlacklistLaunchEpoch = now
                    closeAndDismissApp(applicationContext, pkg)
                    val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                        putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, earlyBlacklistMatch)
                        putExtra(com.example.BlockedActivity.EXTRA_PROHIBITED_PACKAGE, pkg)
                        putExtra(com.example.BlockedActivity.EXTRA_PACKAGE_NAME, pkg)
                        putExtra(com.example.BlockedActivity.EXTRA_APP_NAME, earlyBlacklistMatch)
                        putExtra(
                            com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                            "Este aplicativo ('$earlyBlacklistMatch') está na sua lista de bloqueio de instalação e não pode ser acessado."
                        )
                        putExtra(com.example.BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
                    try {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "LOCK IN: '$earlyBlacklistMatch' está na lista de bloqueio de instalação!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {}
                }
                return
            }
        }

        // 2.1 PICTURE-IN-PICTURE (PIP) DETECTION & ENFORCEMENT
        var pipPackage: String? = null
        var pipNode: AccessibilityNodeInfo? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isWindowChangeEvent = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
            val shouldScanPip = isWindowChangeEvent && (now - lastPipScanEpoch > 3000L)
            if (shouldScanPip) {
                lastPipScanEpoch = now
                try {
                    val pipWindow = windows?.firstOrNull { isWindowInPictureInPicture(it) }
                    if (pipWindow != null) {
                        val root = pipWindow.root
                        val p = root?.packageName?.toString()
                        if (!p.isNullOrBlank() && p != applicationContext.packageName) {
                            pipPackage = p
                            pipNode = root
                        }
                    }
                } catch (_: Exception) {}
                cachedPipPackage = pipPackage
            } else {
                pipPackage = cachedPipPackage
            }
        }

        if (pipPackage != null) {
            try {
                val pipBlacklist = PlayStoreBlacklistManager.isAppOrPackageBlacklisted(applicationContext, pipPackage)
                if (pipBlacklist != null) {
                    dismissPipNode(pipNode)
                    closeAndDismissApp(applicationContext, pipPackage)
                    val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                        putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, pipBlacklist)
                        putExtra(com.example.BlockedActivity.EXTRA_PROHIBITED_PACKAGE, pipPackage)
                        putExtra(com.example.BlockedActivity.EXTRA_PACKAGE_NAME, pipPackage)
                        putExtra(com.example.BlockedActivity.EXTRA_APP_NAME, pipBlacklist)
                        putExtra(
                            com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                            "Este aplicativo está na sua lista de bloqueio da Play Store e está totalmente bloqueado para ser usado."
                        )
                        putExtra(com.example.BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
                    return
                }

                // If the app in PiP is depleted (daily or absolute limit reached): ZERO TOLERANCE!
                if (AppBlockEnforcer.isAppDepleted(pipPackage)) {
                    Log.w("LockInAccessibility", "PiP app depleted: $pipPackage. Dismissing PiP and showing block!")
                    dismissPipNode(pipNode)
                    AppBlockEnforcer.triggerBlockScreen(applicationContext, pipPackage, force = true)
                    return
                }

                // If the user minimized to PiP and is on launcher/unmonitored app, track PiP usage!
                val isForegroundMonitored = AppBlockEnforcer.isAppMonitored(pkg)
                if (!isForegroundMonitored || pkg.contains("launcher") || pkg.contains("home")) {
                    val pipVideoState = if (pipPackage.contains("android.youtube")) {
                        VideoPlaybackState(isShortVideo = false, isVideoActive = true)
                    } else if (pipNode != null) {
                        detectVideoPlaybackStateForRoot(event, pipPackage, pipNode, now)
                    } else {
                        VideoPlaybackState(isShortVideo = false, isVideoActive = true)
                    }

                    LimitTimerManager.onAppForegroundChanged(
                        context = applicationContext,
                        packageName = pipPackage,
                        isShortVideo = pipVideoState.isShortVideo,
                        isVideoActive = true,
                        urlDomain = null,
                        isPictureInPicture = true
                    )
                    return
                }
            } finally {
                try { pipNode?.recycle() } catch (_: Exception) {}
            }
        }

        // 2.5 IMMEDIATE CHECK: Is this app's daily limit DEPLETED? (Zero tolerance blocking)
        val activeWindowRoot = rootInActiveWindow
        val activePkg = try {
            activeWindowRoot?.packageName?.toString() ?: pkg
        } finally {
            try { activeWindowRoot?.recycle() } catch (_: Exception) {}
        }
        if (activePkg.isNotBlank() && activePkg != pkg && !pkg.contains("packageinstaller")) {
            val isLauncherTransition = activePkg.contains("launcher") || activePkg.contains("home") || activePkg.contains("systemui")
            if (!isLauncherTransition) {
                // Event is from a background component or overlay, NOT the user actively foregrounding the app
                return
            }
        }
        if (activePkg.isBlank() && eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !isClick) {
            return
        }

        if (AppBlockEnforcer.isAppDepleted(pkg)) {
            AppBlockEnforcer.triggerBlockScreen(applicationContext, pkg, force = true)
            return
        }

        // 4. Check if current app is prohibited by the Installation / Play Store Blacklist
        val blacklistedMatch = PlayStoreBlacklistManager.isAppOrPackageBlacklisted(applicationContext, pkg)
        if (blacklistedMatch != null) {
            val isFriendApproved = FriendsPartyManager.isPackageTemporarilyAllowed(pkg) ||
                    FriendsPartyManager.isPackageTemporarilyAllowed(blacklistedMatch)
            if (!isFriendApproved) {
                if (now - lastBlacklistLaunchEpoch > 500L) {
                    lastBlacklistLaunchEpoch = now
                    closeAndDismissApp(applicationContext, pkg)
                    val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                        putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, blacklistedMatch)
                        putExtra(com.example.BlockedActivity.EXTRA_PROHIBITED_PACKAGE, pkg)
                        putExtra(com.example.BlockedActivity.EXTRA_PACKAGE_NAME, pkg)
                        putExtra(com.example.BlockedActivity.EXTRA_APP_NAME, blacklistedMatch)
                        putExtra(
                            com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                            "Este aplicativo ('$blacklistedMatch') está na sua lista de bloqueio de instalação e não pode ser acessado."
                        )
                        putExtra(com.example.BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
                }
            }
            return
        }

        // 4.1 Check if current browser is opening a website on the Website Blacklist
        if (browserPackages.contains(pkg)) {
            val rawBrowserUrl = detectBrowserRawUrl(pkg, now)
            val matchedBlockedSite = WebsiteBlacklistManager.isWebsiteBlacklisted(rawBrowserUrl)
            if (matchedBlockedSite != null) {
                if (now - lastWebsiteBlockEpoch > 800L) {
                    lastWebsiteBlockEpoch = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, matchedBlockedSite)
                        putExtra(com.example.BlockedActivity.EXTRA_APP_NAME, "Navegador Web")
                        putExtra(com.example.BlockedActivity.EXTRA_PACKAGE_NAME, pkg)
                        putExtra(
                            com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                            "O site '$matchedBlockedSite' está na sua lista de sites bloqueados e o acesso foi totalmente impedido."
                        )
                        putExtra(com.example.BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
                    }
                    XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
                }
                return
            }

            // Check if current browser URL corresponds to an app in the Installation / Play Store Blacklist!
            val installationBlacklistSiteApp = PlayStoreBlacklistManager.isWebsiteBlacklistedByInstallationBlacklist(rawBrowserUrl)
            if (installationBlacklistSiteApp != null) {
                if (now - lastWebsiteBlockEpoch > 800L) {
                    lastWebsiteBlockEpoch = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, installationBlacklistSiteApp)
                        putExtra(com.example.BlockedActivity.EXTRA_APP_NAME, installationBlacklistSiteApp)
                        putExtra(com.example.BlockedActivity.EXTRA_PACKAGE_NAME, pkg)
                        putExtra(
                            com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                            "O site '$rawBrowserUrl' pertence ao aplicativo '$installationBlacklistSiteApp', que está na sua lista de bloqueio de instalação. O acesso ao site está 100% bloqueado."
                        )
                        putExtra(com.example.BlockedActivity.EXTRA_IS_BLACKLIST_RESTRICTED, true)
                    }
                    XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
                }
                return
            }

            // Check if current browser URL corresponds to an app whose time limit is depleted
            val appForSite = com.example.util.AppWebsiteMapper.findAppForDomain(LimitTimerManager.getCachedApps(), rawBrowserUrl)
            if (appForSite != null && (AppBlockEnforcer.isAppDepleted(appForSite.packageName) || appForSite.isLimitDepleted || appForSite.isBlocked)) {
                if (now - lastWebsiteBlockEpoch > 800L) {
                    lastWebsiteBlockEpoch = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, appForSite.appName)
                        putExtra(com.example.BlockedActivity.EXTRA_APP_NAME, appForSite.appName)
                        putExtra(com.example.BlockedActivity.EXTRA_PACKAGE_NAME, appForSite.packageName)
                        putExtra(
                            com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                            "O tempo limite do app '${appForSite.appName}' foi esgotado. O site correspondente também está bloqueado."
                        )
                    }
                    XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
                }
                return
            }
        }

        // 4.5 Package Installer & Play Store: User installs freely without interruption or warnings.
        // Once installation completes, PackageAddedReceiver locks all access and prompts uninstallation.
        if (pkg == "com.android.vending" || pkg.contains("packageinstaller") || pkg.contains("installer")) {
            return
        }

        // 6. Check if current app is scheduled to be blocked by an App Routine
        val activeAppRoutine = FocusTimeManager.getActiveAppRoutine(applicationContext, pkg)
        if (activeAppRoutine != null) {
            if (now - lastAppRoutineBlockEpoch > 2000L) {
                lastAppRoutineBlockEpoch = now
                performGlobalAction(GLOBAL_ACTION_HOME)
                val blockIntent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                    putExtra(com.example.BlockedActivity.EXTRA_BLOCKED_TARGET, activeAppRoutine.targetAppName ?: activeAppRoutine.name)
                    putExtra(com.example.BlockedActivity.EXTRA_PROHIBITED_PACKAGE, pkg)
                    putExtra(
                        com.example.BlockedActivity.EXTRA_BLOCKED_REASON,
                        "Bloqueado pela rotina programada: '${activeAppRoutine.name}' (${activeAppRoutine.formatTimeRange()})"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, blockIntent)
            }
            return
        }

        val rawBrowserUrl = detectBrowserRawUrl(pkg, now)
        val browserUrlDomain = rawBrowserUrl?.replace("https://", "")?.replace("http://", "")?.split("/")?.firstOrNull()
        val videoState = detectVideoPlaybackState(event, pkg, rawBrowserUrl, now)

        LimitTimerManager.onAppForegroundChanged(
            context = applicationContext,
            packageName = pkg,
            isShortVideo = videoState.isShortVideo,
            isVideoActive = videoState.isVideoActive,
            isPaused = videoState.isPaused,
            urlDomain = browserUrlDomain,
            isPictureInPicture = false
        )
        } catch (t: Throwable) {
            Log.e("LockInAccessibility", "Safe catch in onAccessibilityEvent: ${t.message}", t)
        }
    }

    data class VideoPlaybackState(
        val isShortVideo: Boolean = false,
        val isVideoActive: Boolean = false,
        val isPaused: Boolean = false
    )

    data class StoreBlacklistResult(
        val matchedApp: String,
        val isInstalled: Boolean,
        val targetPackage: String?
    )

    private fun detectVideoPlaybackState(
        event: AccessibilityEvent,
        pkg: String,
        rawBrowserUrl: String? = null,
        now: Long
    ): VideoPlaybackState {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            pkg == lastVideoStatePkg && (now - lastVideoStateEpoch < 600L) &&
            lastVideoStateResult != null) {
            return lastVideoStateResult ?: VideoPlaybackState()
        }

        if (!rawBrowserUrl.isNullOrBlank()) {
            val lowerUrl = rawBrowserUrl.lowercase()
            if (lowerUrl.contains("/shorts") || lowerUrl.contains("/reel") ||
                lowerUrl.contains("tiktok.com") || lowerUrl.contains("kwai.com")) {
                val res = VideoPlaybackState(isShortVideo = true, isVideoActive = true)
                lastVideoStatePkg = pkg
                lastVideoStateResult = res
                lastVideoStateEpoch = now
                return res
            }
            if (lowerUrl.contains("youtube.com/watch") || lowerUrl.contains("youtu.be")) {
                val res = VideoPlaybackState(isShortVideo = false, isVideoActive = true)
                lastVideoStatePkg = pkg
                lastVideoStateResult = res
                lastVideoStateEpoch = now
                return res
            }
            if (lowerUrl.contains("youtube.com")) {
                // Browsing YouTube website
                val res = VideoPlaybackState(isShortVideo = false, isVideoActive = false)
                lastVideoStatePkg = pkg
                lastVideoStateResult = res
                lastVideoStateEpoch = now
                return res
            }
        }

        // TikTok / Kwai are inherently short video platforms
        if (pkg.contains("zhiliaoapp") || pkg.contains("tiktok") || pkg.contains("trill") || pkg.contains("kwai") || pkg.contains("kuaishou")) {
            val root = rootInActiveWindow
            val universalPaused = try {
                if (root != null) isUniversalVideoPaused(root) else false
            } finally {
                try { root?.recycle() } catch (_: Exception) {}
            }
            val res = VideoPlaybackState(isShortVideo = true, isVideoActive = true, isPaused = universalPaused)
            lastVideoStatePkg = pkg
            lastVideoStateResult = res
            lastVideoStateEpoch = now
            return res
        }

        val isYouTube = pkg.contains("android.youtube")
        val isInstagram = pkg.contains("instagram")
        val isFacebook = pkg.contains("katana") || pkg.contains("facebook")
        val isSnapchat = pkg.contains("snapchat")

        if (!isYouTube && !isInstagram && !isFacebook && !isSnapchat) {
            // Check if app has long videos only (Netflix, Twitch, Disney+, VLC)
            val root = rootInActiveWindow
            val universalPaused = try {
                if (root != null) isUniversalVideoPaused(root) else false
            } finally {
                try { root?.recycle() } catch (_: Exception) {}
            }
            val hasLongOnly = AppVideoCategoryHelper.hasLongVideosOnly(pkg)
            val res = VideoPlaybackState(isShortVideo = false, isVideoActive = hasLongOnly || !AppVideoCategoryHelper.hasVideos(pkg), isPaused = universalPaused)
            lastVideoStatePkg = pkg
            lastVideoStateResult = res
            lastVideoStateEpoch = now
            return res
        }

        val root = rootInActiveWindow ?: return VideoPlaybackState(isShortVideo = false, isVideoActive = false)
        val computed = try {
            val universalPaused = isUniversalVideoPaused(root)
            when {
                isYouTube -> {
                    val state = detectYouTubeVideoState(event, root)
                    state.copy(isPaused = universalPaused || state.isPaused)
                }
                isInstagram -> {
                    detectInstagramVideoState(event, root, now)
                }
                isFacebook -> {
                    val isReels = containsViewIdKeyword(root, listOf("reel", "short_form_video", "fb_reels"), maxDepth = 4)
                    VideoPlaybackState(isShortVideo = isReels, isVideoActive = isReels, isPaused = universalPaused)
                }
                isSnapchat -> {
                    val isSpotlight = containsViewIdKeyword(root, listOf("spotlight", "discover_feed"), maxDepth = 4)
                    VideoPlaybackState(isShortVideo = isSpotlight, isVideoActive = isSpotlight, isPaused = universalPaused)
                }
                else -> VideoPlaybackState(isShortVideo = false, isVideoActive = true, isPaused = universalPaused)
            }
        } finally {
            try { root.recycle() } catch (_: Exception) {}
        }
        lastVideoStatePkg = pkg
        lastVideoStateResult = computed
        lastVideoStateEpoch = now
        return computed
    }

    private fun isWindowInPictureInPicture(window: android.view.accessibility.AccessibilityWindowInfo): Boolean {
        return try {
            val method = window.javaClass.getMethod("isInPictureInPictureMode")
            method.invoke(window) as? Boolean ?: false
        } catch (_: Exception) {
            try {
                val method = window.javaClass.getMethod("isInPictureInPicture")
                method.invoke(window) as? Boolean ?: false
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun detectVideoPlaybackStateForRoot(
        event: AccessibilityEvent,
        pkg: String,
        root: AccessibilityNodeInfo,
        now: Long
    ): VideoPlaybackState {
        if (pkg.contains("android.youtube")) {
            return detectYouTubeVideoState(event, root)
        }
        if (pkg.contains("instagram")) {
            val isReels = isInstagramReelsFast(event, root)
            return VideoPlaybackState(isShortVideo = isReels, isVideoActive = true)
        }
        if (pkg.contains("tiktok") || pkg.contains("kwai")) {
            return VideoPlaybackState(isShortVideo = true, isVideoActive = true)
        }
        return VideoPlaybackState(isShortVideo = false, isVideoActive = true)
    }

    private fun dismissPipNode(node: AccessibilityNodeInfo?) {
        if (node == null) return
        val closeKeywords = listOf("close", "fechar", "descartar", "dismiss", "remover")
        fun searchAndClick(n: AccessibilityNodeInfo, depth: Int): Boolean {
            if (depth > 6) return false
            val desc = n.contentDescription?.toString()?.lowercase() ?: ""
            val text = n.text?.toString()?.lowercase() ?: ""
            val id = n.viewIdResourceName?.lowercase() ?: ""
            if (closeKeywords.any { desc.contains(it) || text.contains(it) || id.contains(it) }) {
                if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                val p = n.parent
                if (p != null) {
                    val parentClicked = try {
                        p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } finally {
                        try { p.recycle() } catch (_: Exception) {}
                    }
                    if (parentClicked) return true
                }
            }
            for (i in 0 until n.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = n.getChild(i) ?: continue
                val clicked = try {
                    searchAndClick(child, depth + 1)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
                if (clicked) return true
            }
            return false
        }
        try {
            searchAndClick(node, 0)
        } catch (_: Exception) {}
    }

    private fun findBlacklistMatchInStore(root: AccessibilityNodeInfo): StoreBlacklistResult? {
        val blacklist = PlayStoreBlacklistManager.blacklistedApps.value.ifEmpty {
            AppBlockerAccessibilityService.DEFAULT_BLACKLIST
        }
        val texts = mutableListOf<String>()
        fun collect(n: AccessibilityNodeInfo, depth: Int) {
            if (depth > 7 || texts.size >= 60) return
            n.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            n.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            for (i in 0 until n.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = n.getChild(i) ?: continue
                try {
                    collect(child, depth + 1)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
            }
        }
        try { collect(root, 0) } catch (_: Exception) {}

        // If this is an uninstallation dialog on Play Store or PackageInstaller, allow uninstallation freely
        val isUninstallPrompt = texts.any {
            val lower = it.lowercase()
            lower.contains("desinstalar") || lower.contains("uninstall") ||
                    lower.contains("deseja desinstalar") || lower.contains("desinstalação") ||
                    lower.contains("remover") || lower.contains("excluir")
        }
        if (isUninstallPrompt) {
            return null
        }

        val hasInstalledIndicator = texts.any {
            val lower = it.lowercase()
            lower == "abrir" || lower == "open" || lower.startsWith("abrir ") ||
                    lower == "jogar" || lower == "play" || lower == "instalado" || lower == "installed" ||
                    lower.contains("desinstalar") || lower.contains("uninstall")
        }

        for (item in blacklist) {
            val normItem = item.lowercase().trim()
            if (normItem.isBlank()) continue
            val hasMatch = texts.any { t ->
                val normT = t.lowercase().trim()
                normT == normItem ||
                        normT.startsWith("$normItem ") ||
                        normT.startsWith("$normItem:") ||
                        normT.startsWith("$normItem -") ||
                        normT.startsWith("$normItem –") ||
                        normT.contains(" $normItem ") ||
                        PlayStoreBlacklistManager.isAppBlacklisted(t) != null
            }
            if (hasMatch) {
                val installedPkg = PlayStoreBlacklistManager.findInstalledPackageForBlacklist(applicationContext, item)
                val isInstalled = hasInstalledIndicator || installedPkg != null
                return StoreBlacklistResult(item, isInstalled, installedPkg)
            }
        }
        return null
    }

    /**
     * Helper to detect if YouTube's in-app miniplayer (miniatura) is currently visible.
     */
    /**
     * Precision YouTube detection distinguishing Shorts vs Long Video Player (Full & Miniplayer) vs Home Feed Browsing.
     * Uses a single-pass DFS (max depth 5, max 35 nodes) instead of dozens of synchronous Binder IPC queries.
     */
    private fun detectYouTubeVideoState(event: AccessibilityEvent, root: AccessibilityNodeInfo): VideoPlaybackState {
        val className = event.className?.toString() ?: ""
        if (className.contains("ReelPlayer", ignoreCase = true) ||
            className.contains("ShortsPlayer", ignoreCase = true) ||
            className.contains("ReelWatch", ignoreCase = true)) {
            return VideoPlaybackState(isShortVideo = true, isVideoActive = true)
        }

        var hasMiniplayer = false
        var hasShortsContainer = false
        var hasLongControls = false
        var isShortsTabSelected = false
        var isOtherTabSelected = false
        var visitedNodes = 0

        fun scanYouTube(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 5 || visitedNodes >= 35) return
            visitedNodes++

            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""

            if (node.isVisibleToUser) {
                if (viewId.contains("miniplayer") || text.contains("miniplayer") || desc.contains("miniplayer") ||
                    text.contains("expandir miniplayer") || text.contains("maximizar reprodutor")) {
                    hasMiniplayer = true
                }
                if (viewId.contains("reel") || viewId.contains("shorts_pager") || viewId.contains("shorts_container")) {
                    hasShortsContainer = true
                }
                if (viewId.contains("time_bar") || viewId.contains("youtube_controls") ||
                    viewId.contains("player_control") || viewId.contains("fullscreen_button") ||
                    viewId.contains("watch_while_layout")) {
                    hasLongControls = true
                }
            }

            if ((node.isSelected || node.isFocused) && desc.isNotBlank()) {
                if (desc.contains("short")) {
                    isShortsTabSelected = true
                } else if (desc.contains("início") || desc.contains("home") ||
                    desc.contains("inscriç") || desc.contains("subscription") ||
                    desc.contains("biblioteca") || desc.contains("library") ||
                    desc.contains("você") || desc.contains("you")) {
                    isOtherTabSelected = true
                }
            }

            for (i in 0 until node.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = node.getChild(i) ?: continue
                val shouldStop = try {
                    scanYouTube(child, depth + 1)
                    hasMiniplayer || (hasShortsContainer && !isOtherTabSelected)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
                if (shouldStop) {
                    break
                }
            }
        }

        try {
            scanYouTube(root, 0)
        } catch (_: Exception) {}

        if (hasMiniplayer) {
            return VideoPlaybackState(isShortVideo = false, isVideoActive = true)
        }
        if (!isOtherTabSelected && (hasShortsContainer || isShortsTabSelected)) {
            return VideoPlaybackState(isShortVideo = true, isVideoActive = true)
        }
        if (hasLongControls) {
            return VideoPlaybackState(isShortVideo = false, isVideoActive = true)
        }

        return VideoPlaybackState(isShortVideo = false, isVideoActive = false)
    }

    /**
     * Precision Instagram Reels detection in a single pass (max depth 6)
     */
    private fun isInstagramReelsFast(event: AccessibilityEvent, root: AccessibilityNodeInfo): Boolean {
        val className = event.className?.toString() ?: ""
        if (className.contains("Clips", ignoreCase = true) || className.contains("Reel", ignoreCase = true)) {
            return true
        }

        var isReels = false
        fun inspectNode(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 6 || isReels) return

            val desc = node.contentDescription?.toString() ?: ""
            if (desc.contains("Reels", ignoreCase = true) && (node.isSelected || node.isChecked)) {
                isReels = true
                return
            }

            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            if (viewId.contains("clips_viewer") || viewId.contains("reel_viewer") ||
                viewId.contains("clips_video_container") || viewId.contains("clips_surface")) {
                isReels = true
                return
            }

            for (i in 0 until node.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = node.getChild(i) ?: continue
                try {
                    inspectNode(child, depth + 1)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
                if (isReels) break
            }
        }

        inspectNode(root, 0)
        return isReels
    }

    /**
     * Precision Instagram video & Reels playback detection.
     * Accurately determines when a video is playing vs paused:
     * - When paused: Instagram shows a play button/overlay with contentDescription "Play" or "Reproduzir" or id containing "play".
     * - When unpaused: The play button disappears, video is active and playing.
     */
    private fun detectInstagramVideoState(event: AccessibilityEvent, root: AccessibilityNodeInfo, now: Long): VideoPlaybackState {
        val isReels = isInstagramReelsFast(event, root)
        val isPaused = isInstagramVideoPaused(root) || isUniversalVideoPaused(root)
        if (!isReels) {
            val hasVideoContainer = containsViewIdKeyword(root, listOf("feed_video", "video_container", "story_viewer", "clips_surface"), maxDepth = 4)
            if (hasVideoContainer) {
                return VideoPlaybackState(isShortVideo = false, isVideoActive = true, isPaused = isPaused)
            }
            return VideoPlaybackState(isShortVideo = false, isVideoActive = false, isPaused = false)
        }

        // Reels is active!
        return VideoPlaybackState(
            isShortVideo = true,
            isVideoActive = true,
            isPaused = isPaused
        )
    }

    private fun isInstagramVideoPaused(root: AccessibilityNodeInfo): Boolean {
        val playKeywords = listOf("play", "reproduzir", "tocar", "iniciar vídeo", "resume")
        val playIdKeywords = listOf("play_button", "video_play_button", "indicator_play", "clips_play_button", "button_play")

        var isPaused = false
        fun checkNode(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 6 || isPaused) return
            val cd = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val text = node.text?.toString()?.trim()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            if (node.isVisibleToUser) {
                val isExcludedTab = viewId.contains("tab") || cd.contains("áudio") || cd.contains("audio") || cd.contains("perfil")
                if (!isExcludedTab) {
                    if (playKeywords.any { cd == it || cd.startsWith("$it ") } ||
                        playKeywords.any { text == it || text.startsWith("$it ") } ||
                        playIdKeywords.any { viewId.contains(it) }) {
                        isPaused = true
                        return
                    }
                }
            }

            for (i in 0 until node.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = node.getChild(i) ?: continue
                try {
                    checkNode(child, depth + 1)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
                if (isPaused) break
            }
        }

        checkNode(root, 0)
        return isPaused
    }

    private fun isUniversalVideoPaused(root: AccessibilityNodeInfo): Boolean {
        var isPaused = false
        var hasActivePauseButton = false
        val playKeywords = listOf("play", "reproduzir", "reproduzir vídeo", "play video", "tocar", "continuar", "iniciar", "resume")
        val pauseKeywords = listOf("pause", "pausar", "pausar vídeo", "pause video")
        val playIdKeywords = listOf("play_button", "video_play_button", "indicator_play", "clips_play_button", "button_play", "exo_play", "btn_play", "player_play", "playback_play")
        val pauseIdKeywords = listOf("pause_button", "video_pause_button", "clips_pause_button", "button_pause", "exo_pause", "btn_pause", "player_pause", "playback_pause")

        fun check(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 8 || hasActivePauseButton) return
            val cd = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val text = node.text?.toString()?.trim()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            if (node.isVisibleToUser) {
                if (pauseKeywords.any { cd == it || cd.startsWith("$it ") } ||
                    pauseKeywords.any { text == it || text.startsWith("$it ") } ||
                    pauseIdKeywords.any { viewId.contains(it) }) {
                    hasActivePauseButton = true
                    isPaused = false
                    return
                }

                val isExcludedTab = viewId.contains("tab") || cd.contains("áudio") || cd.contains("audio") || cd.contains("perfil") || cd.contains("profile")
                if (!isExcludedTab) {
                    if (playKeywords.any { cd == it || cd.startsWith("$it ") } ||
                        playKeywords.any { text == it || text.startsWith("$it ") } ||
                        playIdKeywords.any { viewId.contains(it) }) {
                        isPaused = true
                    }
                }
            }

            for (i in 0 until node.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = node.getChild(i) ?: continue
                try {
                    check(child, depth + 1)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
                if (hasActivePauseButton) break
            }
        }

        check(root, 0)
        return if (hasActivePauseButton) false else isPaused
    }

    private fun containsViewIdKeyword(
        node: AccessibilityNodeInfo,
        keywords: List<String>,
        maxDepth: Int,
        currentDepth: Int = 0,
        excludeIds: List<String> = emptyList()
    ): Boolean {
        if (currentDepth > maxDepth) return false
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (excludeIds.any { viewId.contains(it) }) {
            return false
        }

        if (keywords.any { viewId.contains(it) }) {
            return true
        }

        for (i in 0 until node.childCount) {
            if (!scanBudget.tryAcquire()) break
            val child = node.getChild(i) ?: continue
            val match = try {
                containsViewIdKeyword(child, keywords, maxDepth, currentDepth + 1, excludeIds)
            } finally {
                try { child.recycle() } catch (_: Exception) {}
            }
            if (match) {
                return true
            }
        }
        return false
    }

    private fun detectBrowserRawUrl(pkg: String, now: Long): String? {
        if (!browserPackages.contains(pkg)) return null

        if (now - lastBrowserCheckEpoch < 1000L && lastBrowserPackage == pkg) {
            return cachedBrowserUrl
        }

        val root = rootInActiveWindow ?: return cachedBrowserUrl
        val url = try {
            findUrlTextInNode(root, depth = 0)
        } finally {
            try { root.recycle() } catch (_: Exception) {}
        }
        lastBrowserCheckEpoch = now
        lastBrowserPackage = pkg
        cachedBrowserUrl = url
        return url
    }

    private fun findUrlTextInNode(node: AccessibilityNodeInfo, depth: Int = 0): String? {
        if (depth > 5) return null
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("url_bar") || viewId.contains("location_bar") || viewId.contains("search_box")) {
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && (text.contains(".") || text.contains("http"))) {
                return text
            }
        }

        for (i in 0 until node.childCount) {
            if (!scanBudget.tryAcquire()) break
            val child = node.getChild(i) ?: continue
            val found = try {
                findUrlTextInNode(child, depth + 1)
            } finally {
                try { child.recycle() } catch (_: Exception) {}
            }
            if (found != null) return found
        }
        return null
    }

    private fun checkPlayStoreBlacklist(pkg: String, now: Long): Boolean {
        if (now - lastPlayStoreBlockEpoch < 2000L) return true
        if (now - lastPlayStoreScanEpoch < 400L) return false
        lastPlayStoreScanEpoch = now

        val root = rootInActiveWindow ?: return false

        // Fast non-recursive BFS pass (max 60 nodes) to collect texts and buttons
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visitedCount = 0

        var hasInstallAction = false
        var isExplicitUninstall = false
        var matchedBlacklistApp: String? = null

        try {
            while (!queue.isEmpty() && visitedCount < 60) {
                val node = queue.poll() ?: continue
                try {
                    visitedCount++

                    val t = node.text?.toString()?.trim()
                    val cd = node.contentDescription?.toString()?.trim()

                    if (!t.isNullOrEmpty()) {
                        val lowerT = t.lowercase()
                        if (lowerT == "instalar" || lowerT == "install" || lowerT == "obter" ||
                            lowerT == "get" || lowerT.startsWith("instalar ") || lowerT.startsWith("install ") ||
                            lowerT == "atualizar" || lowerT == "update" || lowerT.contains("deseja instalar") ||
                            lowerT.contains("want to install")) {
                            hasInstallAction = true
                        }
                        if (lowerT.contains("deseja desinstalar") || lowerT.contains("uninstall this app") ||
                            lowerT == "desinstalar" || lowerT == "uninstall") {
                            isExplicitUninstall = true
                        }
                        if (matchedBlacklistApp == null) {
                            matchedBlacklistApp = PlayStoreBlacklistManager.isAppBlacklisted(t)
                        }
                    }

                    if (!cd.isNullOrEmpty()) {
                        val lowerCd = cd.lowercase()
                        if (lowerCd == "instalar" || lowerCd == "install" || lowerCd == "obter" ||
                            lowerCd == "get" || lowerCd.startsWith("instalar ") || lowerCd.startsWith("install ") ||
                            lowerCd == "atualizar" || lowerCd == "update") {
                            hasInstallAction = true
                        }
                        if (matchedBlacklistApp == null) {
                            matchedBlacklistApp = PlayStoreBlacklistManager.isAppBlacklisted(cd)
                        }
                    }

                    for (i in 0 until node.childCount) {
                        if (!scanBudget.tryAcquire()) break
                        val child = node.getChild(i)
                        if (child != null) queue.add(child)
                    }
                } finally {
                    try { node.recycle() } catch (_: Exception) {}
                }
            }
        } finally {
            while (!queue.isEmpty()) {
                try { queue.poll()?.recycle() } catch (_: Exception) {}
            }
        }

        // If this is an uninstallation dialog, NEVER treat it as an installation attempt
        if (isExplicitUninstall && !hasInstallAction) {
            return false
        }

        // If a blacklisted app is found on Play Store or PackageInstaller, block it!
        if (matchedBlacklistApp != null) {
            if (pkg.contains("packageinstaller") || pkg.contains("installer") || hasInstallAction || pkg == "com.android.vending") {
                lastPlayStoreBlockEpoch = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                val intent = Intent(applicationContext, com.example.BlockedActivity::class.java).apply {
                    putExtra(
                        com.example.BlockedActivity.EXTRA_BLOCKED_TARGET,
                        "Instalação Bloqueada: '$matchedBlacklistApp' (Blacklist do Lock In)"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                XiaomiCompatibilityHelper.safelyStartActivity(applicationContext, intent)
                return true
            }
        }

        return false
    }

    private fun checkUninstallAttempt(pkg: String, now: Long): Boolean {
        // If uninstallation has been verified and authorized by user, NEVER intercept!
        if (DeviceAdminManager.isUninstallAuthorized(applicationContext)) {
            return false
        }

        // Never intercept inside our own application (e.g. UninstallProtectionActivity, BlockedActivity, or Settings)
        if (pkg == packageName) {
            return false
        }

        val isSettings = pkg.contains("settings")
        val isInstaller = pkg.contains("installer")
        val isXiaomiMiui = pkg.contains("securitycenter")
        val isLauncherDialog = !isSettings && !isInstaller && !isXiaomiMiui && isUninstallSurface(pkg)
        if (!isSettings && !isInstaller && !isXiaomiMiui && !isLauncherDialog) return false

        // An earlier removal of another app must never suppress a new attempt against Lock In.
        if (now - lastUninstallInterceptEpoch < 1500L && pkg == lastUninstallInterceptPackage) return true
        if (now - lastUninstallScanEpoch < 200L && now - uninstallActionClickedEpoch !in 0L..200L) return false
        lastUninstallScanEpoch = now

        val targetRoot: AccessibilityNodeInfo = if (isInstaller) {
            val active = rootInActiveWindow
            val activePkg = active?.packageName?.toString() ?: ""
            if (activePkg.contains("packageinstaller") || activePkg.contains("installer") || activePkg == pkg) {
                active
            } else {
                try { active?.recycle() } catch (_: Exception) {}
                null
            }
        } else {
            val active = rootInActiveWindow
            val activePkg = active?.packageName?.toString() ?: ""
            if (activePkg == pkg) {
                active
            } else {
                try { active?.recycle() } catch (_: Exception) {}
                null
            }
        } ?: return false

        try {
            val targetRootPkg = targetRoot.packageName?.toString() ?: ""
            if (targetRootPkg.isNotBlank() && targetRootPkg != pkg &&
                !targetRootPkg.contains("packageinstaller") &&
                !targetRootPkg.contains("installer") &&
                !targetRootPkg.contains("settings") &&
                !targetRootPkg.contains("securitycenter") && !isLauncherDialog) {
                return false
            }

            val texts = mutableListOf<String>()
            collectWindowTexts(targetRoot, texts, depth = 0)
            if (texts.isEmpty()) return false

            val appLabel = try {
                packageManager.getApplicationLabel(applicationInfo).toString().trim().lowercase()
            } catch (_: Exception) {
                "lock in"
            }
            val myPkgLower = packageName.lowercase()
            val adminLabel = try {
                getString(R.string.device_admin_label).trim().lowercase()
            } catch (_: Exception) {
                "proteção contra desinstalação lock in"
            }

            // Check common external app names to prevent intercepting other apps
            val commonOtherApps = listOf(
                "whatsapp", "instagram", "youtube", "chrome", "spotify",
                "telegram", "facebook", "tiktok", "netflix", "duolingo",
                "twitter", "uber", "gmail", "maps", "discord", "camera", "clock", "calculator"
            )

            // CASE 1: PackageInstaller confirmation dialog (e.g. from Launcher, Play Store, or App prompt)
            if (isInstaller || isLauncherDialog) {
                val hasUninstallQuestion = texts.any {
                    it.contains("deseja desinstalar") || it.contains("uninstall this app") ||
                            it.contains("want to uninstall") || it.contains("desinstalar este app") ||
                            it.contains("desinstalar o app") || it == "desinstalar" || it == "uninstall" ||
                            ((it.contains("desinstalar") || it.contains("uninstall")) && it.contains("?"))
                }
                if (!hasUninstallQuestion) return false
                // A home screen/long-press menu can contain many app labels. Require an actual
                // removal question rather than matching its generic "Uninstall" shortcut.
                if (isLauncherDialog && texts.none { (it.contains("desinstalar") || it.contains("uninstall")) &&
                    (it.contains("?") || it.contains("deseja") || it.contains("want to") || it.contains("removido") || it.contains("removed")) }) return false

                // Check if there is an explicit app title in the dialog
                val alertTitleNodes = targetRoot.findAccessibilityNodeInfosByViewId("android:id/alertTitle")
                val alertTitle = try {
                    if (alertTitleNodes.isNotEmpty()) alertTitleNodes[0].text?.toString()?.trim()?.lowercase() else null
                } finally {
                    alertTitleNodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                }

                val appNameNodes = targetRoot.findAccessibilityNodeInfosByViewId("com.android.packageinstaller:id/app_name")
                val gAppNameNodes = targetRoot.findAccessibilityNodeInfosByViewId("com.google.android.packageinstaller:id/app_name")
                val specificAppName = try {
                    when {
                        appNameNodes.isNotEmpty() -> appNameNodes[0].text?.toString()?.trim()?.lowercase()
                        gAppNameNodes.isNotEmpty() -> gAppNameNodes[0].text?.toString()?.trim()?.lowercase()
                        else -> null
                    }
                } finally {
                    appNameNodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                    gAppNameNodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                }

                // If the dialog explicitly names another app, NEVER intercept!
                if (specificAppName != null) {
                    if (specificAppName != appLabel && specificAppName != "lock in" && specificAppName != "lockin") {
                        return false
                    }
                }
                if (alertTitle != null) {
                    if (alertTitle.contains("deseja desinstalar") && !alertTitle.contains("lock") && !alertTitle.contains(appLabel)) {
                        // Title mentions another app specifically, e.g. "Deseja desinstalar o WhatsApp?"
                        val isOtherInTitle = commonOtherApps.any { alertTitle.contains(it) }
                        if (isOtherInTitle) return false
                    }
                }

                // Check if ANY other app is mentioned in the dialog texts
                val mentionsOtherApp = commonOtherApps.any { other -> texts.any { it.equals(other, ignoreCase = true) } }
                if (mentionsOtherApp) return false

                // Strict check: Dialog MUST explicitly reference Lock In as the target app
                val hasLockInTarget = texts.any { t ->
                    t == appLabel || t == "lock in" || t == "lockin" || t == myPkgLower ||
                            t == "deseja desinstalar o app lock in?" || t == "deseja desinstalar lock in?" ||
                            t == "uninstall lock in?" || t == "desinstalar lock in" ||
                            (Regex("""\b(lock[\s\-_]*in|lockin)\b""", RegexOption.IGNORE_CASE).containsMatchIn(t) &&
                                    !t.contains("serviço") && !t.contains("acessibilidade") && !t.contains("service"))
                }

                if (hasLockInTarget) {
                    return interceptUninstall(pkg, now)
                }
                return false
            }

            // CASE 2: Device Settings (App Info or Device Admin deactivation)
            if (isSettings || isXiaomiMiui) {
                // Subcase 2.1: Device Admin Deactivation attempt
                val isDeviceAdminDeactivation = texts.any {
                    it.contains("desativar e desinstalar") || it.contains("deactivate and uninstall") ||
                    it.contains("desativar este app de administração") ||
                            it.contains("desativar administrador") ||
                            it.contains("deactivate this device admin") ||
                            it.contains("desativar este app de admin") ||
                            it.contains("desativar app de administração") ||
                            it.contains("desativar este administrador") ||
                            it.contains("desativar o administrador") ||
                            it.contains("deactivate device admin") ||
                            it.contains("deactivate admin") ||
                            it.contains("remover administrador")
                }
                if (isDeviceAdminDeactivation) {
                    val isAdminLockIn = texts.any {
                        it.contains("lock in") || it.contains("lockin") || it.contains(adminLabel) || it.contains(appLabel) || it.contains(myPkgLower)
                    }
                    if (isAdminLockIn) {
                        val removingApp = texts.any { it.contains("desinstalar") || it.contains("uninstall") }
                        return interceptUninstall(pkg, now, challengeMode = if (removingApp)
                            com.example.UninstallProtectionActivity.MODE_UNINSTALL_APP else
                            com.example.UninstallProtectionActivity.MODE_DEACTIVATE_ADMIN)
                    }
                    return false
                }

                // Subcase 2.2: Settings App Info page for Lock In
                val entityHeaderNodes = targetRoot.findAccessibilityNodeInfosByViewId("com.android.settings:id/entity_header_title")
                val entityHeaderSamsung = targetRoot.findAccessibilityNodeInfosByViewId("com.samsung.android.settings:id/entity_header_title")
                val entityHeaderMiui = targetRoot.findAccessibilityNodeInfosByViewId("com.miui.securitycenter:id/app_name")
                val headerTitle = try {
                    when {
                        entityHeaderNodes.isNotEmpty() -> entityHeaderNodes[0].text?.toString()?.trim()?.lowercase()
                        entityHeaderSamsung.isNotEmpty() -> entityHeaderSamsung[0].text?.toString()?.trim()?.lowercase()
                        entityHeaderMiui.isNotEmpty() -> entityHeaderMiui[0].text?.toString()?.trim()?.lowercase()
                        else -> null
                    }
                } finally {
                    entityHeaderNodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                    entityHeaderSamsung.forEach { try { it.recycle() } catch (_: Exception) {} }
                    entityHeaderMiui.forEach { try { it.recycle() } catch (_: Exception) {} }
                }

                // If the App Info header title is another app, NEVER intercept!
                if (headerTitle != null && headerTitle != appLabel && headerTitle != "lock in" && headerTitle != "lockin") {
                    return false
                }

                // Check if user is browsing an app list rather than Lock In's individual page
                val otherAppsFound = commonOtherApps.count { otherApp ->
                    texts.any { t -> t.contains(otherApp, ignoreCase = true) }
                }
                if (otherAppsFound >= 1 && headerTitle == null) {
                    return false
                }

                // Check if there is an uninstall / clear data / force stop action specifically on Lock In's screen
                val hasUninstallAction = texts.any {
                    it == "desinstalar" || it == "uninstall" ||
                            it == "forçar parada" || it == "force stop" ||
                            it == "limpar dados" || it == "limpar armazenamento" || it == "clear storage" ||
                            it == "desinstalação" || it.contains("deseja desinstalar")
                }
                if (!hasUninstallAction) return false

                val hasSpecificLockIn = texts.any { t ->
                    t == appLabel || t == "lock in" || t == "lockin" || t == myPkgLower
                }
                val isRemovalConfirmation = texts.any { it.contains("deseja desinstalar") || it.contains("want to uninstall") ||
                    it.contains("desinstalar") && it.contains("?") || it.contains("uninstall") && it.contains("?") }
                if (hasSpecificLockIn && (isRemovalConfirmation || now - uninstallActionClickedEpoch in 0L..1500L)) {
                    return interceptUninstall(pkg, now)
                }
            }

            return false
        } finally {
            try { targetRoot.recycle() } catch (_: Exception) {}
        }
    }

    private fun interceptUninstall(
        pkg: String,
        now: Long,
        challengeMode: String = com.example.UninstallProtectionActivity.MODE_UNINSTALL_APP
    ): Boolean {
        Log.w("LockInAccessibility", "INTERCEPTED UNINSTALL / DEACTIVATION ATTEMPT SPECIFICALLY FOR LOCK IN (pkg: $pkg, mode: $challengeMode)")
        lastUninstallInterceptEpoch = now
        lastUninstallInterceptPackage = pkg
        mainHandler.removeCallbacks(pendingUninstallCheck)
        pendingUninstallPackage = null
        if (showUninstallChallenge(challengeMode)) return true
        // Keep the originating window in place until the challenge is foreground.
        // HOME races the launch and can hide the challenge behind the launcher.
        val intent = Intent(this, com.example.UninstallProtectionActivity::class.java).apply {
            putExtra(com.example.UninstallProtectionActivity.EXTRA_CHALLENGE_MODE, challengeMode)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        XiaomiCompatibilityHelper.safelyStartActivity(this, intent)
        return true
    }

    fun isUninstallDialogActive(): Boolean {
        if (Companion.isAnyUninstallActive()) return true
        var root: AccessibilityNodeInfo? = null
        try {
            root = rootInActiveWindow ?: return false
            val p = root.packageName?.toString() ?: ""
            if (p.contains("packageinstaller") || p.contains("installer") || p.contains("settings")) {
                val texts = mutableListOf<String>()
                collectWindowTexts(root, texts, depth = 0)
                return texts.any {
                    it.contains("desinstalar") || it.contains("uninstall") ||
                            it.contains("deseja desinstalar") || it.contains("desinstalação") ||
                            it.contains("remover")
                }
            }
        } catch (_: Exception) {
        } finally {
            try { root?.recycle() } catch (_: Exception) {}
        }
        return false
    }

    private fun collectWindowTexts(node: AccessibilityNodeInfo, texts: MutableList<String>, depth: Int) {
        if (depth > 5 || texts.size >= 30) return
        val t = node.text?.toString()?.trim()
        if (!t.isNullOrEmpty()) texts.add(t.lowercase())
        val cd = node.contentDescription?.toString()?.trim()
        if (!cd.isNullOrEmpty()) texts.add(cd.lowercase())

        for (i in 0 until node.childCount) {
            if (!scanBudget.tryAcquire()) break
            val child = node.getChild(i) ?: continue
            try {
                collectWindowTexts(child, texts, depth + 1)
            } finally {
                try { child.recycle() } catch (_: Exception) {}
            }
        }
    }

    override fun onInterrupt() {
        uninstallOverlay.dismiss()
        Log.d("LockInAccessibility", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("LockInAccessibility", "Lock in Accessibility Service connected")
        try {
            screenReceiver = ScreenLockReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            screenReceiver?.let {
                ContextCompat.registerReceiver(
                    this,
                    it,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        } catch (_: Exception) {
        }
        try {
            if (android.provider.Settings.canDrawOverlays(applicationContext)) {
                FloatingIslandService.ensureStarted(applicationContext)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Dynamically restricts or restores AccessibilityService info package scoping.
     * When isolated for banking, scopes to an empty/dummy package so that
     * Android OS and banking anti-fraud scanners (Caixa / Topaz OFD / Warsaw)
     * verify that Lock In cannot inspect the bank's window content or intercept input.
     */
    fun setBankingIsolationMode(isolated: Boolean) {
        try {
            val info = serviceInfo ?: return
            if (isolated) {
                info.packageNames = arrayOf("com.example.lockin.isolated.banking")
                Log.i("LockInAccessibility", "Accessibility isolated for banking safety")
            } else {
                info.packageNames = null
                Log.i("LockInAccessibility", "Accessibility restored to standard monitoring")
            }
            serviceInfo = info
        } catch (e: Exception) {
            Log.w("LockInAccessibility", "Failed to update serviceInfo isolation mode", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uninstallOverlay.dismiss()
        mainHandler.removeCallbacks(pendingUninstallCheck)
        if (instance == this) {
            instance = null
        }
        try {
            mainHandler.removeCallbacks(pendingVideoStateRunnable)
            screenReceiver?.let { unregisterReceiver(it) }
            screenReceiver = null
        } catch (_: Exception) {
        }
    }

    companion object {
        var instance: LockInAccessibilityService? = null
            private set

        @Volatile
        var isUninstallingOtherApp: Boolean = false

        @Volatile
        var allowedUninstallPackage: String? = null

        @Volatile
        var allowedUninstallTimestamp: Long = 0L

        fun isAnyUninstallActive(): Boolean {
            val elapsed = System.currentTimeMillis() - allowedUninstallTimestamp
            if (elapsed > 15_000L) {
                isUninstallingOtherApp = false
                return false
            }
            return isUninstallingOtherApp
        }

        fun allowTemporaryUninstall(targetPackage: String?) {
            allowedUninstallPackage = targetPackage
            allowedUninstallTimestamp = System.currentTimeMillis()
            isUninstallingOtherApp = true
        }

        /**
         * Dismisses the target app's active window without sending
         * the device to the launcher Home screen (which would minimize BlockedActivity).
         */
        fun closeAndDismissApp(context: Context, targetPackage: String? = null) {
            // Dismiss the active window through the user-enabled accessibility service.
            instance?.let { service ->
                var activeRoot: AccessibilityNodeInfo? = null
                try {
                    activeRoot = service.rootInActiveWindow
                    val activePkg = activeRoot?.packageName?.toString()
                    if (!targetPackage.isNullOrBlank() && activePkg == targetPackage) {
                        service.performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                } catch (e: Exception) {
                    Log.w("LockInAccessibility", "Error performing back on active app window", e)
                } finally {
                    try { activeRoot?.recycle() } catch (_: Exception) {}
                }
            }
        }

        fun closeActiveAppWindow(context: Context? = null, targetPackage: String? = null) {
            val ctx = context ?: instance?.applicationContext
            if (ctx != null) {
                closeAndDismissApp(ctx, targetPackage)
            } else if (!targetPackage.isNullOrBlank()) {
                instance?.let { service ->
                    var activeRoot: AccessibilityNodeInfo? = null
                    try {
                        activeRoot = service.rootInActiveWindow
                        val activePkg = activeRoot?.packageName?.toString()
                        if (activePkg == targetPackage) {
                            service.performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    } catch (_: Exception) {
                    } finally {
                        try { activeRoot?.recycle() } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
