package com.example.service

import com.example.BlockedActivity
import com.example.LockInApplication
import com.example.data.local.RestrictedAppEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Certification suite for the floating-island visibility fixes:
 * 1. Entering a video-limited app whose feed has no video playing must show the island
 *    in a suspended (PAUSADO) state instead of hiding it (Instagram bug).
 * 2. Leaving and re-entering an app (home/recents round trip) must show the island again.
 * 3. When the remaining time reaches zero, the timer must be blocked (BlockedActivity).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FloatingIslandVisibilityTest {

    private fun insertShortVideoApp(pkg: String) = runBlocking {
        val app = LockInApplication.instance
        val entity = RestrictedAppEntity(
            pkg, "Test Reels App",
            limitShortVideosOnly = true,
            isShortVideoPlatform = true,
            hasShortVideoContent = true,
            useAbsoluteTime = false,
            shortVideoLimitMinutes = 10,
            shortVideoRemainingSeconds = 600,
            longVideoLimitMinutes = 0,
            longVideoRemainingSeconds = 0
        )
        app.repository.insertOrUpdateApp(entity)
    }

    private fun insertAbsoluteApp(pkg: String, remainingSeconds: Long) = runBlocking {
        val app = LockInApplication.instance
        val entity = RestrictedAppEntity(
            pkg, "Test Absolute App",
            useAbsoluteTime = true,
            individualLimitMinutes = 15,
            individualRemainingSeconds = remainingSeconds
        )
        app.repository.insertOrUpdateApp(entity)
    }

    private fun awaitMonitored(pkg: String) = runBlocking {
        withTimeout(8000) {
            while (!LimitTimerManager.isPackageMonitored(pkg)) delay(20)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun stateFlow(): MutableStateFlow<ActiveSessionState> =
        LimitTimerManager.javaClass.getDeclaredField("_sessionState")
            .apply { isAccessible = true }
            .get(LimitTimerManager) as MutableStateFlow<ActiveSessionState>

    private suspend fun awaitState(timeoutMs: Long = 10000, predicate: (ActiveSessionState) -> Boolean): ActiveSessionState {
        val flow = stateFlow()
        withTimeout(timeoutMs) {
            while (!predicate(flow.value)) delay(20)
        }
        return flow.value
    }

    // The block state is set before BlockedActivity is launched, so poll the intent too.
    // nextStartedActivity() dequeues, so peek while waiting and pop once at the end.
    private suspend fun awaitStartedActivity(timeoutMs: Long = 8000): android.content.Intent {
        val shadow = shadowOf(LockInApplication.instance)
        withTimeout(timeoutMs) {
            while (shadow.peekNextStartedActivity() == null) delay(20)
        }
        return shadow.nextStartedActivity!!
    }

    private fun cleanup(pkg: String) {
        val app = LockInApplication.instance
        LimitTimerManager.stopTimer(app)
        runBlocking { runCatching { app.repository.deleteApp(pkg) } }
    }

    /** Bug 1: entering Instagram-like app lands in the feed (no video) → island must be visible in PAUSADO state. */
    @Test
    fun enteringFeedShowsSuspendedIslandInsteadOfHiding() = runBlocking {
        val app = LockInApplication.instance
        val pkg = "com.instagram.feed.test"
        insertShortVideoApp(pkg)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = false)
            val state = awaitState {
                it.isRunning && it.packageName == pkg && it.appName?.contains("Pausado") == true
            }
            assertEquals(600L, state.remainingSeconds)
            assertTrue(state.isRunning)
            assertFalse(state.appName!!.contains("null"))
        } finally {
            cleanup(pkg)
        }
    }

    /** Bug 1b: opening a Reel (video detected) must switch from suspended to counting. */
    @Test
    fun openingReelSwitchesFromSuspendedToCounting() = runBlocking {
        val app = LockInApplication.instance
        val pkg = "com.instagram.reel.test"
        insertShortVideoApp(pkg)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = false)
            awaitState { it.isRunning && it.appName?.contains("Pausado") == true && it.packageName == pkg }
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val state = awaitState {
                it.isRunning && it.packageName == pkg && it.appName?.contains("Pausado") != true
            }
            assertEquals(600L, state.remainingSeconds)
            assertTrue(state.appName!!.contains("Reels"))
        } finally {
            cleanup(pkg)
        }
    }

    /** Bugs 2+3: leave (stopTimer via launcher/recents) then re-enter must show the island again. */
    @Test
    fun leavingAndReenteringShowsIslandAgain() = runBlocking {
        val app = LockInApplication.instance
        val pkg = "com.instagram.reenter.test"
        insertShortVideoApp(pkg)
        awaitMonitored(pkg)
        try {
            // 1) Reel playing → island counting
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            awaitState { it.isRunning && it.packageName == pkg && !it.appName!!.contains("Pausado") }
            // 2) User leaves (launcher/recents) → timer stops
            LimitTimerManager.stopTimer(app)
            assertFalse(stateFlow().value.isRunning)
            // 3) User re-enters: first event is the feed (no video detected yet)
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = false)
            val suspended = awaitState {
                it.isRunning && it.packageName == pkg && it.appName?.contains("Pausado") == true
            }
            assertEquals(600L, suspended.remainingSeconds)
            // 4) Reel detected again → counting resumes with the persisted remaining
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val running = awaitState {
                it.isRunning && it.packageName == pkg && it.appName?.contains("Pausado") != true
            }
            assertEquals(600L, running.remainingSeconds)
        } finally {
            cleanup(pkg)
        }
    }

    /** Certification: when the timer reaches zero the usage must be blocked (BlockedActivity launched). */
    @Test
    fun timerReachingZeroLaunchesBlockedActivity() = runBlocking {
        val app = LockInApplication.instance
        val pkg = "com.test.zerolimit"
        insertAbsoluteApp(pkg, remainingSeconds = 1L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            awaitState(15000) { it.isBlocked && it.packageName == pkg }
            assertTrue(AppBlockEnforcer.isAppDepleted(pkg))
            val started = awaitStartedActivity()
            assertEquals(BlockedActivity::class.java.name, started.component?.className)
            assertEquals(true, started.getBooleanExtra(BlockedActivity.EXTRA_IS_DEPLETED_LIMIT, false))
        } finally {
            cleanup(pkg)
        }
    }

    /** Certification: a depleted app must NOT show a live island when re-entered with no time left. */
    @Test
    fun depletedAppEntryDoesNotShowRunningIsland() = runBlocking {
        val app = LockInApplication.instance
        val pkg = "com.test.depleted.reenter"
        insertAbsoluteApp(pkg, remainingSeconds = 0L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            awaitState(10000) { !it.isRunning }
            // Block screen should have been triggered for the depleted app
            val started = awaitStartedActivity()
            assertEquals(BlockedActivity::class.java.name, started.component?.className)
        } finally {
            cleanup(pkg)
        }
    }
}
