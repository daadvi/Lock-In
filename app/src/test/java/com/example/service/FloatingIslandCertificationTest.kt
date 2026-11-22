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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * SUÍTE DE CERTIFICAÇÃO DA ILHA FLUTUANTE — garante que:
 *
 * A) A ilha aparece SEMPRE que o usuário entra em um app com limite de tempo
 *    configurado (tempo absoluto, grupo ou site).
 * B) A ilha mostra o TIMER INDIVIDUAL DE VÍDEOS CURTOS (Shorts/Reels) quando
 *    o usuário entra em um vídeo curto (isShortVideo=true) — consumindo o
 *    shortVideoRemainingSeconds, NÃO o tempo absoluto/longo.
 * C) A ilha mostra o TIMER INDIVIDUAL DE VÍDEOS LONGOS quando o usuário entra
 *    em vídeo longo (isShortVideo=false) — consumindo o longVideoRemainingSeconds
 *    (ou individual, quando não há limite longo configurado).
 * D) Transições entre feed/vídeo curto/vídeo longo NÃO pausam nem zeram o tempo.
 * E) Tempo zerado → bloqueio (sem ilha ativa).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FloatingIslandCertificationTest {

    private val app: LockInApplication get() = LockInApplication.instance

    // ---------- helpers ----------

    /** App com limites SEPARADOS de vídeo curto e vídeo longo (sem tempo absoluto). */
    private fun insertDualVideoApp(
        pkg: String,
        shortRemaining: Long = 600L,   // 10 min de shorts
        longRemaining: Long = 1800L,   // 30 min de vídeo longo
        shortLimitMinutes: Int = 10,
        longLimitMinutes: Int = 30
    ) = runBlocking {
        app.repository.insertOrUpdateApp(
            RestrictedAppEntity(
                pkg, "Test Dual Video App",
                limitShortVideosOnly = false,
                isShortVideoPlatform = false,
                hasShortVideoContent = true,
                useAbsoluteTime = false,
                shortVideoLimitMinutes = shortLimitMinutes,
                shortVideoRemainingSeconds = shortRemaining,
                longVideoLimitMinutes = longLimitMinutes,
                longVideoRemainingSeconds = longRemaining,
                individualLimitMinutes = 0,
                individualRemainingSeconds = 0
            )
        )
    }

    /** App SOMENTE com limite de vídeo curto (sem limite longo e sem absoluto). */
    private fun insertShortOnlyApp(
        pkg: String,
        shortRemaining: Long = 600L,
        shortLimitMinutes: Int = 10
    ) = runBlocking {
        app.repository.insertOrUpdateApp(
            RestrictedAppEntity(
                pkg, "Test Short Only App",
                limitShortVideosOnly = true,
                isShortVideoPlatform = true,
                hasShortVideoContent = true,
                useAbsoluteTime = false,
                shortVideoLimitMinutes = shortLimitMinutes,
                shortVideoRemainingSeconds = shortRemaining,
                longVideoLimitMinutes = 0,
                longVideoRemainingSeconds = 0,
                individualLimitMinutes = 0,
                individualRemainingSeconds = 0
            )
        )
    }

    private fun insertAbsoluteApp(pkg: String, remainingSeconds: Long) = runBlocking {
        app.repository.insertOrUpdateApp(
            RestrictedAppEntity(
                pkg, "Test Absolute App",
                useAbsoluteTime = true,
                individualLimitMinutes = (remainingSeconds / 60).toInt(),
                individualRemainingSeconds = remainingSeconds
            )
        )
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

    private suspend fun awaitState(
        timeoutMs: Long = 10000,
        predicate: (ActiveSessionState) -> Boolean
    ): ActiveSessionState {
        val flow = stateFlow()
        withTimeout(timeoutMs) {
            while (!predicate(flow.value)) delay(20)
        }
        return flow.value
    }

    private fun cleanup(pkg: String) {
        LimitTimerManager.stopTimer(app)
        runBlocking { runCatching { app.repository.deleteApp(pkg) } }
    }

    /** nextStartedActivity() dequeues, so peek while waiting and pop once at the end. */
    private suspend fun awaitStartedActivity(timeoutMs: Long = 8000): android.content.Intent {
        val shadow = shadowOf(app)
        withTimeout(timeoutMs) {
            while (shadow.peekNextStartedActivity() == null) delay(20)
        }
        return shadow.nextStartedActivity!!
    }

    // =====================================================================
    // A) ILHA APARECE EM TODO APP COM LIMITE
    // =====================================================================

    /** A1: entrar em app com TEMPO ABSOLUTO → sessão ativa (ilha mostrada via showIsland). */
    @Test
    fun absoluteLimitAppEntryStartsIslandSession() = runBlocking {
        val pkg = "com.cert.absolute.entry"
        insertAbsoluteApp(pkg, remainingSeconds = 900L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            val state = awaitState { it.isRunning && it.packageName == pkg }
            assertTrue("Sessão deve estar ativa (ilha visível)", state.isRunning)
            assertEquals(900L, state.remainingSeconds)
            assertEquals(900L, state.totalLimitSeconds)
            assertFalse(state.isShortVideo)
        } finally {
            cleanup(pkg)
        }
    }

    /** A2: entrar em app monitorado SEM vídeo nenhum (feed) → ilha ainda aparece (timer contínuo). */
    @Test
    fun feedEntryStartsIslandSessionForLimitedApp() = runBlocking {
        val pkg = "com.cert.feed.entry"
        insertDualVideoApp(pkg)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = false)
            val state = awaitState { it.isRunning && it.packageName == pkg }
            assertTrue("Ilha deve aparecer mesmo no feed (timer contínuo)", state.isRunning)
        } finally {
            cleanup(pkg)
        }
    }

    /** A3: app NÃO monitorado → nenhuma sessão deve existir (ilha escondida). */
    @Test
    fun unmonitoredAppDoesNotStartIslandSession() = runBlocking {
        val pkg = "com.cert.unmonitored.app"
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            delay(300)
            val state = stateFlow().value
            assertFalse("Sem limite configurado → ilha não deve aparecer", state.isRunning)
        } finally {
            cleanup(pkg)
        }
    }

    // =====================================================================
    // B) TIMER INDIVIDUAL DE VÍDEOS CURTOS (Shorts/Reels)
    // =====================================================================

    /** B1: entrar em vídeo curto → consome shortVideoRemainingSeconds e mostra rótulo Shorts/Reels. */
    @Test
    fun shortVideoUsesIndividualShortTimer() = runBlocking {
        val pkg = "com.cert.short.video"
        insertDualVideoApp(pkg, shortRemaining = 600L, longRemaining = 1800L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val state = awaitState { it.isRunning && it.packageName == pkg && it.isShortVideo }
            assertEquals("Vídeo curto deve usar o timer de SHORT (600s), não o de longo (1800s)", 600L, state.remainingSeconds)
            assertEquals(600L, state.totalLimitSeconds)
            assertTrue(state.isShortVideo)
            assertTrue("Rótulo deve indicar Shorts", state.appName!!.contains("Shorts"))
        } finally {
            cleanup(pkg)
        }
    }

    /** B2: no Instagram, o rótulo do vídeo curto é "Reels". */
    @Test
    fun instagramShortVideoShowsReelsLabel() = runBlocking {
        val pkg = "com.cert.instagram.reels"
        insertDualVideoApp(pkg)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val state = awaitState { it.isRunning && it.isShortVideo && it.packageName == pkg }
            assertTrue(state.appName!!.contains("Reels"))
        } finally {
            cleanup(pkg)
        }
    }

    /** B3: app só com limite de SHORT → sessão ativa com o tempo de shorts. */
    @Test
    fun shortOnlyAppEntryStartsIslandSession() = runBlocking {
        val pkg = "com.cert.short.only"
        insertShortOnlyApp(pkg, shortRemaining = 600L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val state = awaitState { it.isRunning && it.isShortVideo && it.packageName == pkg }
            assertEquals(600L, state.remainingSeconds)
        } finally {
            cleanup(pkg)
        }
    }

    /** B4: app só com limite de SHORT, sem vídeo curto detectado (feed) → sem limite para esse modo → sem sessão. */
    @Test
    fun shortOnlyAppFeedEntryDoesNotTick() = runBlocking {
        val pkg = "com.cert.short.only.feed"
        insertShortOnlyApp(pkg)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = false)
            delay(400)
            val state = stateFlow().value
            assertFalse("Feed sem vídeo curto em app 'somente shorts' → ilha não deve contar", state.isRunning)
        } finally {
            cleanup(pkg)
        }
    }

    // =====================================================================
    // C) TIMER INDIVIDUAL DE VÍDEOS LONGOS
    // =====================================================================

    /** C1: entrar em vídeo longo → consome longVideoRemainingSeconds (não o de shorts). */
    @Test
    fun longVideoUsesIndividualLongTimer() = runBlocking {
        val pkg = "com.instagram.longvideo.cert"
        insertDualVideoApp(pkg, shortRemaining = 600L, longRemaining = 1800L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            val state = awaitState { it.isRunning && it.packageName == pkg && !it.isShortVideo }
            assertEquals("Vídeo longo deve usar o timer LONGO (1800s), não o de shorts (600s)", 1800L, state.remainingSeconds)
            assertEquals(1800L, state.totalLimitSeconds)
            assertFalse(state.isShortVideo)
            assertTrue("Rótulo deve indicar Vídeo Longo", state.appName!!.contains("Vídeo Longo"))
        } finally {
            cleanup(pkg)
        }
    }

    /** C2: app com limite de shorts + limite longo ZERADO → vídeo longo não tem limite próprio... mas tem individual.
     *  Nesse caso cai no timer individual (fallback), então deve rodar com o tempo individual. */
    @Test
    fun longVideoFallsBackToIndividualTimerWhenLongLimitIsZero() = runBlocking {
        val pkg = "com.cert.long.fallback"
        app.repository.insertOrUpdateApp(
            RestrictedAppEntity(
                pkg, "Test Fallback App",
                useAbsoluteTime = false,
                shortVideoLimitMinutes = 10,
                shortVideoRemainingSeconds = 600,
                longVideoLimitMinutes = 0,
                longVideoRemainingSeconds = 0,
                individualLimitMinutes = 20,
                individualRemainingSeconds = 1200
            )
        )
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            val state = awaitState { it.isRunning && it.packageName == pkg && !it.isShortVideo }
            assertEquals(1200L, state.remainingSeconds)
        } finally {
            cleanup(pkg)
        }
    }

    // =====================================================================
    // D) TRANSIÇÕES CURTO ↔ LONGO (sem perder tempo)
    // =====================================================================

    /** D1: vídeo curto → vídeo longo na MESMA app: cada timer mantém SEU tempo separado. */
    @Test
    fun modeSwitchKeepsSeparateTimers() = runBlocking {
        val pkg = "com.cert.mode.switch"
        insertDualVideoApp(pkg, shortRemaining = 600L, longRemaining = 1800L)
        awaitMonitored(pkg)
        try {
            // 1) Vídeo curto
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            awaitState { it.isRunning && it.isShortVideo && it.packageName == pkg }
            // 2) Troca para vídeo longo
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            val longState = awaitState { it.isRunning && !it.isShortVideo && it.packageName == pkg }
            assertEquals("Timer longo deve ser independente (1800s)", 1800L, longState.remainingSeconds)
            // 3) Volta para vídeo curto
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val shortState = awaitState { it.isRunning && it.isShortVideo && it.packageName == pkg }
            assertEquals("Timer curto deve preservar seu tempo (600s)", 600L, shortState.remainingSeconds)
        } finally {
            cleanup(pkg)
        }
    }

    /** D2: deixar o app e voltar ao vídeo longo → tempo persistido é retomado. */
    @Test
    fun leavingAndReenteringLongVideoRestoresTimer() = runBlocking {
        val pkg = "com.cert.long.reenter"
        insertDualVideoApp(pkg, shortRemaining = 600L, longRemaining = 1800L)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            awaitState { it.isRunning && !it.isShortVideo && it.packageName == pkg }
            LimitTimerManager.stopTimer(app)
            assertFalse(stateFlow().value.isRunning)
            // Re-entra no vídeo longo
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            val state = awaitState { it.isRunning && !it.isShortVideo && it.packageName == pkg }
            assertEquals(1800L, state.remainingSeconds)
        } finally {
            cleanup(pkg)
        }
    }

    /** D3: feed (sem vídeo) → vídeo curto: timer contínuo, SEM pausa/zeramento. */
    @Test
    fun feedToShortVideoTransitionKeepsTimerRunning() = runBlocking {
        val pkg = "com.cert.feed.to.short"
        insertDualVideoApp(pkg)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = false)
            awaitState { it.isRunning && it.packageName == pkg }
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            val state = awaitState { it.isRunning && it.isShortVideo && it.packageName == pkg }
            assertEquals("Transição feed→short não deve resetar o timer", 600L, state.remainingSeconds)
        } finally {
            cleanup(pkg)
        }
    }

    // =====================================================================
    // E) TEMPO ZERADO → BLOQUEIO
    // =====================================================================

    /** E1: vídeo curto com tempo esgotado (limite configurado, saldo 0) → bloqueio imediato. */
    @Test
    fun depletedShortVideoTimeBlocksImmediately() = runBlocking {
        val pkg = "com.cert.short.depleted"
        insertShortOnlyApp(pkg, shortRemaining = 0L, shortLimitMinutes = 10)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = true, isVideoActive = true)
            // Entrada com saldo 0 → triggerBlockScreen (BlockedActivity) + stopTimer, sem sessão ativa.
            awaitState { !it.isRunning }
            val started = awaitStartedActivity()
            assertEquals(BlockedActivity::class.java.name, started.component?.className)
            assertEquals(true, started.getBooleanExtra(BlockedActivity.EXTRA_IS_DEPLETED_LIMIT, false))
        } finally {
            cleanup(pkg)
        }
    }

    /** E2: vídeo longo com tempo esgotado (limite configurado, saldo 0) → bloqueio imediato. */
    @Test
    fun depletedLongVideoTimeBlocksImmediately() = runBlocking {
        val pkg = "com.cert.long.depleted"
        insertDualVideoApp(pkg, shortRemaining = 600L, longRemaining = 0L, longLimitMinutes = 30)
        awaitMonitored(pkg)
        try {
            LimitTimerManager.onAppForegroundChanged(app, pkg, isShortVideo = false, isVideoActive = true)
            awaitState { !it.isRunning }
            val started = awaitStartedActivity()
            assertEquals(BlockedActivity::class.java.name, started.component?.className)
            assertEquals(true, started.getBooleanExtra(BlockedActivity.EXTRA_IS_DEPLETED_LIMIT, false))
        } finally {
            cleanup(pkg)
        }
    }
}